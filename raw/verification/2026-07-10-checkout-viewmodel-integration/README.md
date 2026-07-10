# Checkout ViewModel Integration Verification

Date: 2026-07-10

Repository baseline: `62e73d1` (`test: verify Checkout ViewModel integration`)

## Verified Boundary

`CheckoutViewModelTest` uses the production `CheckoutViewModel`,
`ProductRepository`, `PaymentRepository`, `SessionRepository`, and Afsm host.
Only Room DAO boundaries are replaced with in-memory recording fakes.

The four scenarios prove:

- navigation `productId` drives the load command and reaches `ProductReady`,
- a valid session and payment result reach durable `Completed(orderId)`, insert
  the expected order, and emit `PaymentCompleted` to an active collector,
- a missing session reaches `PaymentFailed` without inserting an order,
- a missing product reaches `ProductUnavailable` through command-result event
  mapping.

No production code changed.

## Verification

```bash
./gradlew :sample-shop:testDebugUnitTest \
  --tests 'afsm.sample.shop.feature.checkout.CheckoutViewModelTest' \
  --no-daemon

./gradlew :sample-shop:testDebugUnitTest \
  :sample-shop:generateAfsmMmd \
  --no-daemon

./scripts/verify-release-local.sh --no-daemon
```

Result: all commands passed, including API checks, Maven Local publication, and
the clean external consumer build.

Known non-blocking output remained the SDK XML compatibility warning and Gradle
deprecation notice already present in the repository environment.

## Evidence Boundary

This proves repository-sample ViewModel wiring. It does not prove Android
process recreation, a real Room database, Compose rendering, or human pilot
usefulness.
