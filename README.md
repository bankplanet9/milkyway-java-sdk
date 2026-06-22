# MilkyWay Payments — Java SDK

A production-ready Java client for the **MilkyWay Payments API** (`/payments/v1`):
Keycloak client-credentials auth with token caching + refresh, transient-failure
retries with backoff, typed exceptions, exact-decimal money, and a polling helper.

- **Coordinates:** `com.bankplanet9:milkyway-payments-sdk`
- **Base package:** `com.bankplanet9.milkyway`
- **Requires:** Java 11+. Depends only on Jackson (`jackson-databind`) and the JDK's
  built-in `java.net.http.HttpClient`.

## Install

### Maven

```xml
<dependency>
  <groupId>com.bankplanet9</groupId>
  <artifactId>milkyway-payments-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle

```kotlin
implementation("com.bankplanet9:milkyway-payments-sdk:1.0.0")
```

## Quick start

```java
import com.bankplanet9.milkyway.*;
import com.bankplanet9.milkyway.model.*;
import java.math.BigDecimal;

MilkywayOptions options = MilkywayOptions.builder()
        .baseUrl("https://milkyway.stage.planet9.ae")
        .tokenUrl("https://keycloak.ac8o.planet9.ae/realms/planet9-stage/protocol/openid-connect/token")
        .clientId("your-client-id")
        .clientSecret("your-client-secret")
        // .scope("payments")           // optional
        // .maxRetries(3)               // optional resilience tuning
        // .requestTimeout(Duration.ofSeconds(30))
        .build();

MilkywayPaymentsClient client = new MilkywayPaymentsClient(options);

// 1. Health of a recipient service
String health = client.healthcheck("recipient-bank-id", "card-payout");

// 2. Quote (FX rate + commission), no payment created
PrecheckResult quote = client.precheck(new PrecheckRequest()
        .setThirdPartyIdDebit("recipient-bank-id")
        .setServiceId("card-payout")
        .setRecipientId("recipient-ref")
        .setAmountCredit(new BigDecimal("100.00"))
        .setCurrencyCredit("USD"));
System.out.println(quote.getAmountDebit() + " " + quote.getCurrencyDebit());

// 3. Pay — pass a stable Idempotency-Key to make the call safely retryable
long transactionId = client.pay(new PayRequest()
        .setThirdPartyIdDebit("recipient-bank-id")
        .setServiceId("card-payout")
        .setSenderId("sender-ref")
        .setRecipientId("recipient-ref")
        .setAmountCredit(new BigDecimal("100.00"))
        .setCurrencyCredit("USD"), "order-4242");

// 4. Poll until terminal
PostcheckResult result = client.waitForCompletion(transactionId);
if (result.getStatus() == TransactionStatus.DONE) {
    System.out.println("settled");
} else {
    System.out.println("ended in " + result.getStatus() + ": " + result.getError());
}

// 5. Cancel (if still cancellable)
CancelResult cancel = client.cancel(transactionId);
```

The client is **thread-safe** — build one and reuse it for the application's
lifetime. It owns its `HttpClient`; the Keycloak token endpoint is reached over a
separate `HttpClient` so token acquisition never recurses through the
auth/retry pipeline.

## Money & types

Money is always `java.math.BigDecimal`, never `double`. Amounts are sent and
received as **bare JSON numbers** and parsed with
`USE_BIG_DECIMAL_FOR_FLOATS`, so no binary-float rounding occurs.

`TransactionStatus` mirrors the integer wire codes and exposes `isTerminal()`:

| Code | Status            | Terminal |
| ---- | ----------------- | -------- |
| 0    | `PENDING`         | no       |
| 1    | `DONE`            | yes      |
| 2    | `CANCELLED`       | yes      |
| 3    | `FAILED`          | yes      |
| 4    | `CANCEL_PENDING`  | no       |
| 5    | `STUCK`           | yes      |

## Errors

Every API error raises a subtype of `MilkywayApiException`, carrying
`getStatusCode()` and the server's `error` message (and `getResponseBody()` for
diagnostics):

| HTTP  | Exception                              |
| ----- | -------------------------------------- |
| 400   | `MilkywayValidationException`          |
| 401   | `MilkywayAuthException`                |
| 402   | `MilkywayExposureBlockedException`     |
| 404   | `MilkywayNotFoundException`            |
| 5xx   | `MilkywayServiceUnavailableException`  |
| other | `MilkywayApiException`                 |

```java
try {
    client.pay(req, "order-4242");
} catch (MilkywayExposureBlockedException e) {
    // 402 — payment would breach a block-action exposure limit
} catch (MilkywayValidationException e) {
    // 400 — bad request (e.g. e.getMessage())
} catch (MilkywayApiException e) {
    // anything else: e.getStatusCode(), e.getResponseBody()
}
```

## Retries & idempotency

- Transient failures — HTTP `5xx`, `408`, and `IOException` — are retried with
  **exponential backoff + full jitter**, up to `maxRetries` (default 3, base delay
  500ms). Each attempt is bounded by `requestTimeout` (default 30s).
- Deterministic outcomes (`400/401/402/404`) are **never** retried.
- On a `401`, the SDK **force-refreshes the token once and replays** the request.
- **`pay` without an `Idempotency-Key` is NOT auto-retried** (a retry could create a
  duplicate payment). Pass a stable key — `client.pay(req, "key")` — to opt into
  retries; `client.pay(req)` runs exactly once.

Token handling: the access token is cached and refreshed ~30s before its stated
expiry (`tokenRefreshSkew`), and concurrent callers collapse to a **single**
in-flight acquisition.

`waitForCompletion(transactionId[, PollOptions])` polls `postcheck` with
exponential backoff until a terminal status or the poll-timeout budget (default 5
min); if the budget is exhausted it returns the last observed status.

## Releasing & Maven Central setup (one-time)

Versioning is automated with **semantic-release** (`.releaserc.json`, branches
`["main"]`, tag format `v${version}`): conventional commits drive the version,
`@semantic-release/exec` injects it into the POM
(`mvn versions:set -DnewVersion=...`) and publishes with
`mvn -Prelease deploy`. The `release` Maven profile carries GPG signing and the
Sonatype Central Portal publishing plugin; a normal `mvn verify` activates neither,
so local builds and CI tests need no GPG key or credentials.

The release job is **gated on the repo variable `PUBLISH_ENABLED=true`**, so it is
skipped until you have completed the steps below — a release can never fail
half-configured.

**To enable publishing to Maven Central:**

1. **Register & verify the namespace.** Sign in to the
   [Sonatype Central Portal](https://central.sonatype.com/), add the namespace
   `com.bankplanet9`, and verify ownership (DNS TXT record for the domain, or the
   GitHub-org verification flow). Wait until the namespace shows as *Verified*.
2. **Create a publishing token.** In the Central Portal under *Account → Generate
   User Token*, generate a token; note the **username** and **password** halves.
3. **Generate a GPG key and publish it.**
   ```bash
   gpg --gen-key                                  # or --full-generate-key
   gpg --list-secret-keys --keyid-format=long     # note the key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --export-secret-keys --armor <KEY_ID>      # this is GPG_PRIVATE_KEY
   ```
   Also send the key to `keys.openpgp.org` for redundancy.
4. **Add CI secrets** (repo *Settings → Secrets and variables → Actions → Secrets*):
   - `CENTRAL_TOKEN_USERNAME` — Central Portal token username
   - `CENTRAL_TOKEN_PASSWORD` — Central Portal token password
   - `GPG_PRIVATE_KEY` — the ASCII-armored private key from step 3
   - `GPG_PASSPHRASE` — the key's passphrase
5. **Add the repo variable** (same page, *Variables* tab):
   - `PUBLISH_ENABLED` = `true`

Once these are present, every push to `main` with a release-worthy commit cuts a
`vX.Y.Z` tag + GitHub release and deploys signed artifacts (jar + sources +
javadoc) to Maven Central.

## License

[MIT](LICENSE) — Copyright (c) 2026 Planet9.
