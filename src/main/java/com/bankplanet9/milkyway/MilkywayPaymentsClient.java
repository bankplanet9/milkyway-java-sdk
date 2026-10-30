package com.bankplanet9.milkyway;

import com.bankplanet9.milkyway.auth.AccessTokenProvider;
import com.bankplanet9.milkyway.auth.KeycloakTokenProvider;
import com.bankplanet9.milkyway.exception.MilkywayApiException;
import com.bankplanet9.milkyway.exception.MilkywayAuthException;
import com.bankplanet9.milkyway.exception.MilkywayExposureBlockedException;
import com.bankplanet9.milkyway.exception.MilkywayNotFoundException;
import com.bankplanet9.milkyway.exception.MilkywayPaymentRejectedException;
import com.bankplanet9.milkyway.exception.MilkywayRecipientBankException;
import com.bankplanet9.milkyway.exception.MilkywayServiceUnavailableException;
import com.bankplanet9.milkyway.exception.MilkywayValidationException;
import com.bankplanet9.milkyway.internal.CancelRequest;
import com.bankplanet9.milkyway.internal.DiscoveryResponse;
import com.bankplanet9.milkyway.internal.HttpExecutor;
import com.bankplanet9.milkyway.internal.MilkywayJson;
import com.bankplanet9.milkyway.internal.PayResponse;
import com.bankplanet9.milkyway.model.CancelResult;
import com.bankplanet9.milkyway.model.Destination;
import com.bankplanet9.milkyway.model.PayRequest;
import com.bankplanet9.milkyway.model.PostcheckResult;
import com.bankplanet9.milkyway.model.PrecheckRequest;
import com.bankplanet9.milkyway.model.PrecheckResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for the MilkyWay Payments API ({@code /payments/v1}).
 *
 * <p>Build one with {@link MilkywayOptions} and reuse it for the lifetime of your
 * application — it is thread-safe. The client owns its {@link HttpClient}; the
 * token endpoint uses a separate client so token acquisition never recurses through
 * the auth/retry pipeline.
 */
public final class MilkywayPaymentsClient {

    private static final String PAYMENTS_PATH = "payments/v1";

    private static final TypeReference<Map<String, Object>> SCHEMA_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final String baseUrl;
    private final HttpExecutor executor;

    /** Builds a fully self-contained client from {@link MilkywayOptions}. */
    public MilkywayPaymentsClient(MilkywayOptions options) {
        this(options, HttpClient.newHttpClient(), null);
    }

    /**
     * Test/advanced constructor: supply the API {@link HttpClient} and optionally a
     * pre-built {@link AccessTokenProvider}. The {@code apiHttpClient} is used for all
     * Payments API calls; when {@code tokenProvider} is {@code null}, a
     * {@link KeycloakTokenProvider} is created over a separate default {@link HttpClient}.
     */
    public MilkywayPaymentsClient(MilkywayOptions options, HttpClient apiHttpClient, AccessTokenProvider tokenProvider) {
        if (options == null) {
            throw new IllegalArgumentException("options is required.");
        }
        if (apiHttpClient == null) {
            throw new IllegalArgumentException("apiHttpClient is required.");
        }
        this.baseUrl = normalizeBaseUrl(options.getBaseUrl());
        AccessTokenProvider provider = tokenProvider != null
                ? tokenProvider
                : new KeycloakTokenProvider(HttpClient.newHttpClient(), options);
        this.executor = new HttpExecutor(apiHttpClient, provider, options);
    }

    /**
     * GET {@code /payments/v1/healthcheck}. Returns the recipient status string;
     * throws {@link MilkywayServiceUnavailableException} if the recipient is down.
     */
    public String healthcheck(String thirdPartyId, String serviceId) {
        if (isBlank(thirdPartyId)) {
            throw new IllegalArgumentException("thirdPartyId is required.");
        }
        if (isBlank(serviceId)) {
            throw new IllegalArgumentException("serviceId is required.");
        }
        String uri = baseUrl + PAYMENTS_PATH + "/healthcheck?third_party_id=" + enc(thirdPartyId)
                + "&service_id=" + enc(serviceId);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(uri)).GET();
        HttpResponse<String> response = executor.send(builder, true);
        if (!isSuccess(response.statusCode())) {
            throw toException(response.statusCode(), response.body());
        }
        return response.body();
    }

    /**
     * POST {@code /payments/v1/precheck}. Returns the FX + commission quote.
     *
     * @throws MilkywayValidationException       400 — invalid request; {@code getField()} names the field
     *                                           (e.g. {@code recipient_id} when the receiving bank reports
     *                                           the recipient invalid).
     * @throws MilkywayExposureBlockedException  402 — would breach a block-action exposure limit.
     * @throws MilkywayPaymentRejectedException  422 — the receiving bank refused the payment; not retried.
     * @throws MilkywayRecipientBankException    502 — the receiving bank's connector failed; retried as a 5xx.
     * @throws MilkywayServiceUnavailableException 503/5xx — a service is unavailable; retried.
     */
    public PrecheckResult precheck(PrecheckRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required.");
        }
        HttpRequest.Builder builder = jsonPost(PAYMENTS_PATH + "/precheck", request);
        return sendForJson(builder, true, PrecheckResult.class);
    }

    /**
     * POST {@code /payments/v1}. Returns the new transaction id.
     *
     * <p>Without an idempotency key the call is NOT auto-retried (a duplicate
     * payment could otherwise be created). Pass a stable {@code idempotencyKey} to
     * make the call safely retryable.
     *
     * @throws MilkywayValidationException       400 — invalid request; {@code getField()} names the field.
     * @throws MilkywayExposureBlockedException  402 — would breach a block-action exposure limit.
     * @throws MilkywayPaymentRejectedException  422 — the receiving bank refused the payment; nothing was
     *                                           created, and repeating the request will not change the answer.
     * @throws MilkywayRecipientBankException    502 — the receiving bank's connector failed; nothing was created.
     * @throws MilkywayServiceUnavailableException 503/5xx — a service is unavailable; nothing was created.
     */
    public long pay(PayRequest request, String idempotencyKey) {
        if (request == null) {
            throw new IllegalArgumentException("request is required.");
        }
        HttpRequest.Builder builder = jsonPost(PAYMENTS_PATH, request);
        boolean retryable = !isBlank(idempotencyKey);
        if (retryable) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        PayResponse result = sendForJson(builder, retryable, PayResponse.class);
        return result.getTransactionId();
    }

    /** Convenience overload: {@code pay} without an idempotency key (not auto-retried). */
    public long pay(PayRequest request) {
        return pay(request, null);
    }

    /**
     * GET {@code /payments/v1/postcheck}. Returns the current transaction status; on a
     * terminal outcome {@link PostcheckResult#getError()} may carry a
     * {@link com.bankplanet9.milkyway.model.StatusReason} code.
     */
    public PostcheckResult postcheck(long transactionId) {
        String uri = baseUrl + PAYMENTS_PATH + "/postcheck?transaction_id=" + transactionId;
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(uri)).GET();
        return sendForJson(builder, true, PostcheckResult.class);
    }

    /**
     * POST {@code /payments/v1/cancel}. Returns the resulting transaction status.
     *
     * <p>A {@code 403} ({@code ErrPaymentCannotBeCanceled}) raises a plain
     * {@link MilkywayApiException}: the payment can no longer be cancelled.
     *
     * @throws MilkywayNotFoundException          404 — not found, or not your transaction.
     * @throws MilkywayRecipientBankException     502 — the receiving bank's connector failed; the payment
     *                                            is unchanged.
     * @throws MilkywayServiceUnavailableException 503 — the receiving bank could not be reached
     *                                            ({@code getServiceId()}, {@code getThirdPartyId()} name it);
     *                                            the payment is unchanged.
     */
    public CancelResult cancel(long transactionId) {
        HttpRequest.Builder builder = jsonPost(PAYMENTS_PATH + "/cancel", new CancelRequest(transactionId));
        return sendForJson(builder, true, CancelResult.class);
    }

    /** Convenience overload: the full discovery catalog, unfiltered by country. */
    public List<Destination> discovery() {
        return discovery(null);
    }

    /**
     * GET {@code /payments/v1/discovery}. Lists the destination banks you can pay and
     * the services each can receive through. Pass an ISO 3166-1 alpha-2 {@code country}
     * (case-insensitive) to filter; pass {@code null} or blank to list all.
     *
     * <p>Each {@code ServiceCapability} carries its {@code PartyProfile} — which
     * {@code senderInfo} / {@code recipientInfo} fields the recipient bank requires — and
     * omits its {@code data} JSON Schema; fetch that on demand with
     * {@link #serviceSchema(String, String)}.
     */
    public List<Destination> discovery(String country) {
        String uri = baseUrl + PAYMENTS_PATH + "/discovery";
        if (!isBlank(country)) {
            uri += "?country=" + enc(country);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(uri)).GET();
        DiscoveryResponse response = sendForJson(builder, true, DiscoveryResponse.class);
        List<Destination> destinations = response.getDestinations();
        return destinations != null ? destinations : Collections.emptyList();
    }

    /**
     * GET {@code /payments/v1/discovery/{thirdPartyId}/{serviceId}/schema}. Returns the
     * service's raw Draft-2020 JSON Schema as a parsed {@code Map}, describing the
     * {@code data} extras (never identity) a {@code pay}/{@code precheck} for this service
     * must satisfy. Most services take none.
     */
    public Map<String, Object> serviceSchema(String thirdPartyId, String serviceId) {
        if (isBlank(thirdPartyId)) {
            throw new IllegalArgumentException("thirdPartyId is required.");
        }
        if (isBlank(serviceId)) {
            throw new IllegalArgumentException("serviceId is required.");
        }
        String uri = baseUrl + PAYMENTS_PATH + "/discovery/" + encPath(thirdPartyId)
                + "/" + encPath(serviceId) + "/schema";
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(uri)).GET();
        return sendForJson(builder, true, SCHEMA_TYPE);
    }

    /** Polls {@code postcheck} with default {@link PollOptions} until terminal or timeout. */
    public PostcheckResult waitForCompletion(long transactionId) {
        return waitForCompletion(transactionId, PollOptions.defaults());
    }

    /**
     * Polls {@code postcheck} with exponential backoff until the transaction reaches
     * a terminal status or the poll-timeout budget is exhausted. When the budget is
     * exceeded the last observed (non-terminal) status is returned.
     */
    public PostcheckResult waitForCompletion(long transactionId, PollOptions pollOptions) {
        PollOptions opts = pollOptions != null ? pollOptions : PollOptions.defaults();
        long startNanos = System.nanoTime();
        Duration delay = opts.getInitialDelay();

        PostcheckResult result = postcheck(transactionId);
        while (!result.getStatus().isTerminal()) {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
            if (elapsed.plus(delay).compareTo(opts.getTimeout()) > 0) {
                return result;
            }

            sleep(delay);

            long nextMs = (long) Math.min(
                    delay.toMillis() * opts.getBackoffMultiplier(),
                    opts.getMaxDelay().toMillis());
            delay = Duration.ofMillis(nextMs);

            result = postcheck(transactionId);
        }
        return result;
    }

    // ---- internals ----------------------------------------------------------

    private HttpRequest.Builder jsonPost(String path, Object payload) {
        String json;
        try {
            json = MilkywayJson.MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new MilkywayApiException(0, "Failed to serialize the request body.", null, ex);
        }
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
    }

    private <T> T sendForJson(HttpRequest.Builder builder, boolean retryable, Class<T> type) {
        HttpResponse<String> response = sendChecked(builder, retryable);
        try {
            return requireNonNull(MilkywayJson.MAPPER.readValue(response.body(), type), response);
        } catch (IOException ex) {
            throw parseFailure(response, ex);
        }
    }

    private <T> T sendForJson(HttpRequest.Builder builder, boolean retryable, TypeReference<T> type) {
        HttpResponse<String> response = sendChecked(builder, retryable);
        try {
            return requireNonNull(MilkywayJson.MAPPER.readValue(response.body(), type), response);
        } catch (IOException ex) {
            throw parseFailure(response, ex);
        }
    }

    private HttpResponse<String> sendChecked(HttpRequest.Builder builder, boolean retryable) {
        HttpResponse<String> response;
        try {
            response = executor.send(builder, retryable);
        } catch (HttpExecutor.MilkywayServiceUnavailableMarker marker) {
            throw new MilkywayServiceUnavailableException(0,
                    "The Payments API could not be reached.", null);
        }
        if (!isSuccess(response.statusCode())) {
            throw toException(response.statusCode(), response.body());
        }
        return response;
    }

    private static <T> T requireNonNull(T parsed, HttpResponse<String> response) {
        if (parsed == null) {
            throw new MilkywayApiException(response.statusCode(),
                    "The API returned an empty response.", response.body());
        }
        return parsed;
    }

    private static MilkywayApiException parseFailure(HttpResponse<String> response, IOException ex) {
        return new MilkywayApiException(response.statusCode(),
                "Failed to parse the API response.", response.body(), ex);
    }

    private static MilkywayApiException toException(int status, String body) {
        ErrorBody err = ErrorBody.parse(body);
        String message = err.error != null ? err.error : "Request failed with status " + status + ".";
        String code = errorCodeOf(err.error);
        switch (status) {
            case 400:
                return new MilkywayValidationException(message, body, code, err.field);
            case 401:
                return new MilkywayAuthException(message, body);
            case 402:
                return new MilkywayExposureBlockedException(message, body, code);
            case 404:
                return new MilkywayNotFoundException(message, body, code);
            case 422:
                return new MilkywayPaymentRejectedException(message, body, code);
            case 502:
                return new MilkywayRecipientBankException(message, body, code);
            default:
                if (status >= 500) {
                    return new MilkywayServiceUnavailableException(status, message, body, code,
                            err.serviceId, err.thirdPartyId);
                }
                return new MilkywayApiException(status, message, body, code, null);
        }
    }

    /** The leading {@code Err...} identifier of an {@code error} value, or {@code null}. */
    static String errorCodeOf(String error) {
        if (error == null || !error.startsWith("Err")) {
            return null;
        }
        int end = 3;
        while (end < error.length() && Character.isLetterOrDigit(error.charAt(end))) {
            end++;
        }
        if (end == 3 || (end < error.length() && error.charAt(end) != ':')) {
            return null;
        }
        return error.substring(0, end);
    }

    /** The fields an error response may carry; any of them may be absent. */
    private static final class ErrorBody {
        String error;
        String field;
        String serviceId;
        String thirdPartyId;

        static ErrorBody parse(String body) {
            ErrorBody result = new ErrorBody();
            if (isBlank(body)) {
                return result;
            }
            JsonNode root;
            try {
                root = MilkywayJson.MAPPER.readTree(body);
            } catch (IOException ex) {
                // Non-JSON body (e.g. plain-text 500 from healthcheck) — use it as-is.
                result.error = body.trim();
                return result;
            }
            if (root != null && root.isObject()) {
                result.error = text(root, "error");
                result.field = text(root, "field");
                result.serviceId = text(root, "service_id");
                result.thirdPartyId = text(root, "third_party_id");
            }
            return result;
        }

        private static String text(JsonNode root, String name) {
            JsonNode node = root.get(name);
            return node != null && node.isTextual() ? node.asText() : null;
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "/";
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** URL-encodes a single path segment ({@code +} means space in a query, not a path). */
    private static String encPath(String value) {
        return enc(value).replace("+", "%20");
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MilkywayApiException(0, "Interrupted while waiting for completion.", null, ex);
        }
    }
}
