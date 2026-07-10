# Checkout Process Restoration Verification

Date: 2026-07-10

Repository implementation commit: `6d381a0`
(`feat: restore Checkout stable payment state`)

## Red Test Evidence

The focused Checkout tests were first run before production changes. Kotlin
compilation failed on the missing `PaymentStatusUnknown` phase, saved-state
keys, and `savedStateHandle` ViewModel parameter. No existing test was weakened.

## Implemented Contract

- Persist only `productId`, `completedOrderId`, and
  `pendingPaymentRequestId`.
- Restore completion without command execution or effect replay.
- Restore unresolved payment as `PaymentStatusUnknown(requestId)` with no
  automatic retry or payment action.
- Start `ScreenEntered` only for a fresh `Idle` state.
- Write pending immediately before the payment repository call.
- On success, write completion before clearing pending and dispatching the
  result event.
- On normal failure, clear pending and keep the existing retry path.
- Use AndroidX `viewModelFactory { initializer { createSavedStateHandle() } }`
  in the sample route.

## Passing Verification

```bash
./gradlew :sample-shop:testDebugUnitTest \
  --tests 'afsm.sample.shop.feature.checkout.CheckoutViewModelTest' \
  --tests 'afsm.sample.shop.feature.checkout.CheckoutStateMachineTest' \
  :sample-shop:generateAfsmMmd \
  --no-daemon

./gradlew :sample-shop:testDebugUnitTest \
  :sample-shop:generateAfsmMmd \
  :sample-shop:assembleDebug \
  --no-daemon

./scripts/verify-release-local.sh --no-daemon
```

Result: passed, including API checks, Maven Local publication, and the clean
external consumer build.

The generated Checkout graph contains `PaymentStatusUnknown` as a declared
restoration-only state with no ordinary incoming transition.

## Android CLI Boundary

`android describe --project_dir=.` found the assembled sample APK. The official
runtime launch could not be completed:

```text
Error: No online devices or emulators found. Please connect a device or start an emulator.
```

`android emulator start medium_phone` reported `emulator-5554` booted, but
subsequent official `android run` attempts still reported that neither the
serial nor AVD name was discoverable. No ADB installation fallback was used.

## Evidence Boundary

The tests prove snapshot conversion and ViewModel behavior using real
`SavedStateHandle` instances. They do not simulate OS process death or resolve a
real backend payment. The APK assembles, but on-device Compose factory execution
remains unverified because the official Android CLI could not discover a device.
