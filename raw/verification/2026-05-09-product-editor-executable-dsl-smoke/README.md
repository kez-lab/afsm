# ProductEditor Executable DSL Smoke

Date: 2026-05-09

Purpose: verify that migrating ProductEditor from the phased helper to the executable DSL keeps the Android sample-shop product registration journey working on device.

## Code Change Under Test

- ProductEditor now wraps an executable `AfsmMachine<ProductEditorPhase, ProductEditorContext, ...>`.
- ProductEditor still exposes `ProductEditorState = ProductEditorPhase + ProductEditorContext` to the ViewModel/UI.
- ProductEditor graph edges are exported from `ProductEditorStateMachine.topology`.

## Build Verification

```bash
./gradlew :sample-shop:assembleDebug --no-daemon
./gradlew :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' --no-daemon
```

Result: passed.

## Device Journey

Device: `emulator-5556`

Commands:

```bash
android run --device=emulator-5556 --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk --activity=.MainActivity
android layout --device=emulator-5556 --pretty --output=...
android screen capture --annotate --output=...
adb -s emulator-5556 shell input tap ...
adb -s emulator-5556 shell input text ...
```

Actions:

- Installed and launched `sample-shop-debug.apk`.
- Switched to register mode.
- Created account `DslSeller2`.
- Verified catalog with `Register product`.
- Opened product registration.
- Entered product draft `DslMug`, `Leakproof mug for executable DSL smoke test`, and `24.50`.
- Submitted for review.
- Verified first mock review rejection.
- Resubmitted for review.
- Verified review approval.
- Published the product.
- Verified `Published product: DslMug`.
- Tapped `Done`.
- Verified catalog contains `DslMug`, `$24.50`, and the draft description.

Result: passed.

## Evidence Files

- `01-launch-layout.json`
- `02-register-mode-layout.json`
- `08-after-create-account-layout.json`
- `09-editor-empty-layout.json`
- `10-editor-filled-keyboard-layout.json`
- `10-editor-filled-keyboard-annotated.png`
- `11-after-submit-tap-layout.json`
- `12-after-resubmit-layout.json`
- `13-after-publish-layout.json`
- `14-after-done-layout.json`
- `14-after-done-annotated.png`
