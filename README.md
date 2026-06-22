# MilkyWay Payments — Java SDK

A production-ready Java client for the **MilkyWay Payments API** (`/payments/v1`):
Keycloak client-credentials auth with token caching + refresh, transient-failure
retries with backoff, typed exceptions, exact-decimal money, and a polling helper.

- **Coordinates:** `com.bankplanet9:milkyway-payments-sdk`
- **Base package:** `com.bankplanet9.milkyway`
- **Requires:** Java 11+. Depends only on Jackson (`jackson-databind`) and the JDK's
  built-in `java.net.http.HttpClient`.

## Install

This SDK is **not published to Maven Central**. Consume it via
[JitPack](https://jitpack.io/) (builds the tag from source on demand), grab the
JAR attached to a [GitHub release](https://github.com/bankplanet9/milkyway-java-sdk/releases),
or build it yourself (`mvn -B -DskipTests package`).

### Maven (JitPack)

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.bankplanet9</groupId>
  <artifactId>milkyway-java-sdk</artifactId>
  <version>v1.0.0</version>
</dependency>
```

### Gradle (JitPack)

```kotlin
repositories { maven { url = uri("https://jitpack.io") } }
implementation("com.github.bankplanet9:milkyway-java-sdk:v1.0.0")
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

## Releasing

Versioning is automated with **semantic-release** (`.releaserc.json`, branches
`["main"]`, tag format `v${version}`): conventional commits drive the version
(`feat:` → minor, `fix:`/`perf:` → patch, `!` → major). On each release-worthy push
to `main`, CI builds the JAR at that version, **attaches it to a GitHub release**,
and tags `vX.Y.Z`.

This SDK is intentionally **not published to a package registry** — no Maven Central,
no Sonatype account, no GPG key. Consumers get it three ways:

- **JitPack** — `com.github.bankplanet9:milkyway-java-sdk:vX.Y.Z` (builds the tag from
  source on first request; see [Install](#install)).
- **GitHub release asset** — download the JAR attached to any
  [release](https://github.com/bankplanet9/milkyway-java-sdk/releases).
- **Build from source** — `mvn -B -DskipTests package`.

## License

[MIT](LICENSE) — Copyright (c) 2026 Planet9.
