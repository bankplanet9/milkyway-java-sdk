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

// Your customer (the payer). Fill what you hold; the service's party profile in
// discovery says which fields the recipient bank requires.
PartyInfo sender = new PartyInfo()
        .setPartyType(PartyType.NATURAL)
        .setName(new PartyName().setFirst("Ivan").setLast("Petrov").setFull("Petrov Ivan Sergeevich"))
        .setDateOfBirth("1990-04-18")
        .setIdentityDocumentType(IdentityDocumentType.PASSPORT)
        .setIdentityDocumentNumber("AB1234567")
        .setIdentityDocumentCountry("TJ")
        .setCountryOfResidence("AE");

// 2. Quote (FX rate + commission), no payment created
PrecheckResult quote = client.precheck(new PrecheckRequest()
        .setThirdPartyIdDebit("recipient-bank-id")
        .setServiceId("card-payout")
        .setRecipientId("recipient-ref")
        .setAmountCredit(new BigDecimal("100.00"))
        .setCurrencyCredit("USD")
        .setSenderInfo(sender));
System.out.println(quote.getAmountDebit() + " " + quote.getCurrencyDebit());

// 3. Pay — pass a stable Idempotency-Key to make the call safely retryable
long transactionId = client.pay(new PayRequest()
        .setThirdPartyIdDebit("recipient-bank-id")
        .setServiceId("card-payout")
        .setSenderId("sender-ref")
        .setRecipientId("recipient-ref")
        .setAmountCredit(new BigDecimal("100.00"))
        .setCurrencyCredit("USD")
        .setSenderInfo(sender), "order-4242");

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

## Discovery

Two read-only endpoints let you discover who you can pay, which payer and
beneficiary fields each service needs, and what its `data` extras (if any) must look
like — no payment is created.

```java
import com.bankplanet9.milkyway.model.Destination;
import com.bankplanet9.milkyway.model.PartyProfile;
import com.bankplanet9.milkyway.model.ServiceCapability;
import java.util.List;
import java.util.Map;

// 1. List destination banks and their services (optionally filter by ISO 3166-1
//    alpha-2 country; case-insensitive). Omit the country to list all.
List<Destination> destinations = client.discovery("TJ");   // or client.discovery()
for (Destination d : destinations) {
    System.out.println(d.getThirdPartyId() + " (" + d.getPartnerName() + ")");
    for (ServiceCapability svc : d.getServices()) {
        System.out.println("  " + svc.getServiceId() + " via " + svc.getPayoutMethod()
                + " " + svc.getPayoutCurrencies());
        // Which senderInfo fields the recipient bank requires, e.g. [name.first, date_of_birth].
        PartyProfile profile = svc.getPartyProfile();
        if (profile != null && profile.getSenderInfo() != null) {
            System.out.println("    sender requires " + profile.getSenderInfo().getRequired());
        }
    }
}

// 2. Fetch a service's JSON Schema (Draft 2020-12) — the shape of the `data`
//    extras a precheck/pay for that service must satisfy (most services take none).
Map<String, Object> schema = client.serviceSchema("recipient-bank-id", "card-payout");
```

The listing omits each service's schema for compactness; fetch it on demand with
`serviceSchema(thirdPartyId, serviceId)`. Both calls raise `MilkywayAuthException`
on `401`, and `serviceSchema` raises `MilkywayNotFoundException` on `404`.

## Payer and beneficiary (`senderInfo`, `recipientInfo`)

The identity of your customer (`senderInfo`, the originator) and of the beneficiary
(`recipientInfo`) travels in two typed `PartyInfo` objects that are **the same for
every recipient bank and service** (FATF Travel Rule / ISO 20022 / IVMS101
originator–beneficiary data). Map your customer data to it once; each recipient
bank's connector converts it to that bank's format. The account, card or phone stays
in `senderId` / `recipientId` — these objects carry the person.

| Setter | Wire field | Format |
| --- | --- | --- |
| `setPartyType` | `party_type` | `PartyType.NATURAL` (the default, always sent). `PartyType.LEGAL` is not accepted by the API yet. |
| `setName(new PartyName().setFirst/setLast/setFull)` | `name.first`, `name.last`, `name.full` | Text. `full` is the name as written in the document, family name first, with a patronymic if any. |
| `setDateOfBirth` | `date_of_birth` | ISO 8601 date, `YYYY-MM-DD`. |
| `setIdentityDocumentType` | `identity_document_type` | An `IdentityDocumentType` constant: `PASSPORT`, `NATIONAL_ID`, `DRIVER_LICENSE`, `RESIDENCE_PERMIT`, `TAX_ID`, `SOCIAL_SECURITY`, `OTHER`. Required with `setIdentityDocumentNumber`. |
| `setIdentityDocumentNumber` | `identity_document_number` | Series and number together, as in the document. |
| `setIdentityDocumentCountry`, `setCountryOfResidence` | `identity_document_country`, `country_of_residence` | ISO 3166-1 alpha-2, upper case (`TJ`). |
| `setAddress(new PartyAddress()...)` | `address` | `country` (required when an address is sent), `city`, `street`, `building`, `postalCode`, and `line` — up to 7 free-text lines. |

Text values are at most 256 characters. Null and empty values are left off the wire.
The server refuses a value of the wrong type or an invalid value with `400`
(`MilkywayValidationException`) naming the field, e.g.
`sender_info.identity_document_type`.

**Which fields a service needs** is set by the recipient bank and published per
service as `ServiceCapability.getPartyProfile()` in `discovery(...)`: `getRequired()`
and `getAccepted()` field paths (e.g. `name.first`, `date_of_birth`) for
`getSenderInfo()` and `getRecipientInfo()`, and, when the bank takes only some
document kinds, `getIdentityDocumentTypes()`. A precheck or pay missing a required
field is refused with `400` naming it (`sender_info.date_of_birth: is required by the
receiving bank for this service`). Send the fields you have; fields the bank does not
use are ignored.

```java
PayRequest req = new PayRequest()
        // ...
        .setSenderInfo(sender)
        .setRecipientInfo(new PartyInfo()
                .setName(new PartyName().setFirst("Aziz").setLast("Karimov"))
                .setAddress(new PartyAddress().setCountry("TJ").setCity("Dushanbe")));
```

Identity must **not** be put in `data`: MilkyWay never reads it from there.

## Service-specific extras (`data`)

`setData(...)` carries only what a service needs beyond the parties (for example a
test control field on the stage stub bank). Most services take no `data` fields. When
one does, `serviceSchema(thirdPartyId, serviceId)` returns its JSON Schema, and the
recipient bank validates it during precheck. The human-browsable registry of banks,
services, party profiles and `data` fields is the **Handbook** at
<https://docs.planet9.ae/site/milkyway-handbook/en/> (section **Services**).

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
`getStatusCode()`, the server's `error` message as `getMessage()`, its
machine-readable code as `getErrorCode()` (for example `ErrPaymentRejected`;
`null` when the `error` does not start with an `Err...` code), and
`getResponseBody()` for diagnostics:

| HTTP  | Exception                              | Meaning |
| ----- | -------------------------------------- | ------- |
| 400   | `MilkywayValidationException`          | Invalid request. `getField()` names the field at fault, e.g. `amount_credit`, `currency_credit`, `sender_info.date_of_birth`, or `recipient_id` (`ErrRecipientInvalid`: the receiving bank rejects the recipient). |
| 401   | `MilkywayAuthException`                | Missing or invalid token. |
| 402   | `MilkywayExposureBlockedException`     | The payment would breach a block-action exposure limit. |
| 404   | `MilkywayNotFoundException`            | Not found, or not your transaction. |
| 422   | `MilkywayPaymentRejectedException`     | `ErrPaymentRejected` (precheck, pay): the receiving bank refused the payment on its merits. Nothing was created; repeating it will not change the answer. Never retried. |
| 502   | `MilkywayRecipientBankException`       | `ErrRecipientBankError` (precheck, pay, cancel): the receiving bank's connector failed. Nothing was created and a cancelled payment is unchanged. Subclass of `MilkywayServiceUnavailableException`. |
| 5xx   | `MilkywayServiceUnavailableException`  | A service is unavailable. On `503` (precheck, pay, cancel) `getServiceId()` and `getThirdPartyId()` name the downstream service; nothing was created. |
| other | `MilkywayApiException`                 | e.g. `403` `ErrPaymentCannotBeCanceled` on cancel. |

```java
try {
    client.pay(req, "order-4242");
} catch (MilkywayPaymentRejectedException e) {
    // 422 — the receiving bank declined; do not resend the same payment
} catch (MilkywayRecipientBankException e) {
    // 502 — the receiving bank's connector failed; retry later or use another service
} catch (MilkywayServiceUnavailableException e) {
    // 503/5xx — e.getServiceId(), e.getThirdPartyId(); safe to retry once it recovers
} catch (MilkywayExposureBlockedException e) {
    // 402 — payment would breach a block-action exposure limit
} catch (MilkywayValidationException e) {
    // 400 — e.getField(): "sender_info.date_of_birth", "recipient_id", ...
} catch (MilkywayApiException e) {
    // anything else: e.getStatusCode(), e.getErrorCode(), e.getResponseBody()
}
```

`MilkywayRecipientBankException` extends `MilkywayServiceUnavailableException`, so
an existing `catch (MilkywayServiceUnavailableException e)` still catches a `502`.
Catch the subclass first when you want to tell them apart.

### Status reasons

On a terminal status, `PostcheckResult.getError()` (also from `waitForCompletion`)
carries the receiving bank's reason as a **code**, never free text. Compare it with
the `StatusReason` constants:

| Constant                                  | Code                          |
| ----------------------------------------- | ----------------------------- |
| `StatusReason.PAYMENT_REJECTED`           | `ErrPaymentRejected` — the bank declined (also any reason it gave without a code) |
| `StatusReason.RECIPIENT_NOT_FOUND`        | `ErrRecipientNotFound`        |
| `StatusReason.RECIPIENT_INVALID`          | `ErrRecipientInvalid`         |
| `StatusReason.SERVICE_UNAVAILABLE`        | `ErrServiceUnavailable`       |
| `StatusReason.PAYMENT_CANNOT_BE_CANCELED` | `ErrPaymentCannotBeCanceled`  |

`getError()` is `null` when the bank gave no reason. Treat an unknown code as a
generic failure: new codes may be added.

```java
PostcheckResult result = client.waitForCompletion(transactionId);
if (result.getStatus() == TransactionStatus.FAILED
        && StatusReason.RECIPIENT_NOT_FOUND.equals(result.getError())) {
    // ask the customer to check the recipient
}
```

## Retries & idempotency

- Transient failures — HTTP `5xx` (including `502`), `408`, and `IOException` — are retried with
  **exponential backoff + full jitter**, up to `maxRetries` (default 3, base delay
  500ms). Each attempt is bounded by `requestTimeout` (default 30s).
- Deterministic outcomes (`400/401/402/404/422` and every other `4xx`) are **never** retried.
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
