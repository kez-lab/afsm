# ProductEditor Upload Boundary Verification

Date: 2026-07-11

Specification commit: `a3ef911`

Implementation commit: `8b44656`

Android CLI version: `1.0.15433482`

## Red Evidence

`ProductEditorViewModelTest` was changed before production code to inject a
controllable uploader. The focused compile failed because
`ProductImageUploader` and the `imageUploader` ViewModel parameter did not
exist.

The previous test protected the intent that upload cancellation must prevent a
late success. The replacement preserves that intent and additionally proves the
injected suspend boundary starts and executes cancellation cleanup.

## Implemented Contract

- `ProductImageUploader.upload(draft)` is a feature-owned suspend boundary.
- ProductEditorRoute explicitly supplies `MockProductImageUploader`.
- The default mock uses a demo-only 2 second cooperative delay so the cancel
  state can be observed in a future journey.
- Uploader success becomes `ImageUploadSucceeded`.
- Ordinary exceptions become `ImageUploadFailed("Image upload failed.")`; raw
  backend details do not become UI data.
- `CancellationException` is rethrown and does not become a domain failure.
- A controllable fake proves start and cancellation without elapsed-time races.
- The ViewModel still owns no `Job` registry; phase lifetime remains in
  `AfsmHost` invocation.

## Passing Repository Verification

```bash
./gradlew :sample-shop:testDebugUnitTest \
  --tests 'afsm.sample.shop.feature.editor.ProductEditorViewModelTest' \
  --no-daemon
./gradlew :sample-shop:testDebugUnitTest \
  :sample-shop:generateAfsmMmd \
  :sample-shop:assembleDebug --no-daemon
./scripts/verify-release-local.sh --no-daemon
```

Result: passed, including sample tests, unchanged graph generation, APK
assembly, all API checks, Maven Local publication, and the clean external
consumer.

## Android CLI Journey Result

The required installation/launch action failed, so no layout, screenshot, or
tap assertion was attempted.

```text
android run --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk
Error: No online devices or emulators found. Please connect a device or start an emulator.

android emulator list
Pixel_9
medium_phone

android emulator start medium_phone
Virtual device successfully started as 'emulator-5554'

android run --device=emulator-5554 --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk
Error: Device with serial or AVD name 'emulator-5554' not found.

android run --device=medium_phone --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk
Error: Device with serial or AVD name 'medium_phone' not found.

android run --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk
Error: No online devices or emulators found. Please connect a device or start an emulator.
```

`android emulator stop emulator-5554` then reported that no local emulator was
running. ADB installation was intentionally not used as substitute evidence.

## Evidence Boundary

This proves a realistic local suspend boundary, safe failure mapping, and
cooperative cancellation through the real ViewModel. It does not prove a real
transport or SDK stops work, and it does not prove the cancel button is visible
or tappable on-device. Those claims require a functioning official CLI device
path and a production-like pilot.
