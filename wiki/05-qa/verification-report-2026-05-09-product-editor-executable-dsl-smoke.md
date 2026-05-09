---
title: ProductEditor Executable DSL Smoke Verification
updated: 2026-05-09
---

# ProductEditor Executable DSL Smoke Verification

This report records device verification after ProductEditor was migrated from the phased helper to the executable DSL.

Raw evidence:

- [raw/verification/2026-05-09-product-editor-executable-dsl-smoke](../../raw/verification/2026-05-09-product-editor-executable-dsl-smoke)

## Build Verification

Commands:

```bash
./gradlew :sample-shop:assembleDebug --no-daemon
./gradlew :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

## Android Journey

Device:

- `emulator-5556`

Actions:

- Installed and launched `sample-shop-debug.apk` through `android run`.
- Created account `DslSeller2`.
- Opened product registration.
- Entered product draft `DslMug`, `Leakproof mug for executable DSL smoke test`, and `24.50`.
- Submitted for review.
- Verified first mock review rejection.
- Resubmitted for review.
- Verified review approval.
- Published product.
- Verified published state.
- Returned to catalog through `Done`.
- Verified `DslMug` appears with `$24.50`.

## Result

Passed.

Important layout evidence:

- `11-after-submit-tap-layout.json`: contains `Status: Review rejected`.
- `12-after-resubmit-layout.json`: contains `Status: Review approved`.
- `13-after-publish-layout.json`: contains `Status: Product published` and `Published product: DslMug`.
- `14-after-done-layout.json`: contains `DslMug`, `$24.50`, and `Leakproof mug for executable DSL smoke test`.
