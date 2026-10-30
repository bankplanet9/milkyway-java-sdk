package com.bankplanet9.milkyway.exception;

/**
 * HTTP 5xx — the API or a downstream recipient service is unavailable. Transient:
 * the SDK retries it (except a {@code pay} without an Idempotency-Key).
 *
 * <p>On a {@code 503} the API names the downstream service that failed:
 * {@link #getServiceId()} and {@link #getThirdPartyId()}. {@link #getErrorCode()} is
 * {@code ErrServiceUnavailable} when the recipient service is unreachable, or
 * {@code ErrRecipientRateUnavailable} when it quoted no usable FX rate.
 *
 * <p>A {@code 502} raises the subclass {@link MilkywayRecipientBankException}, so
 * catching this class covers it too.
 */
public class MilkywayServiceUnavailableException extends MilkywayApiException {

    private static final long serialVersionUID = 1L;

    private final String serviceId;
    private final String thirdPartyId;

    public MilkywayServiceUnavailableException(int statusCode, String message, String responseBody) {
        this(statusCode, message, responseBody, null, null, null);
    }

    public MilkywayServiceUnavailableException(int statusCode, String message, String responseBody,
                                               String errorCode, String serviceId, String thirdPartyId) {
        super(statusCode, message, responseBody, errorCode, null);
        this.serviceId = serviceId;
        this.thirdPartyId = thirdPartyId;
    }

    /** The downstream service that could not be reached (503 only), or {@code null}. */
    public String getServiceId() {
        return serviceId;
    }

    /** The institution owning that service (503 only), or {@code null}. */
    public String getThirdPartyId() {
        return thirdPartyId;
    }
}
