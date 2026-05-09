---
title: Sample Shop FSM Smoke Verification
updated: 2026-05-09
---

# Sample Shop FSM Smoke Verification

This report records Android CLI verification for the rewritten sample-shop auth and product registration FSM flows.

Raw evidence:

- [raw/verification/2026-05-09-sample-shop-fsm-smoke](../../raw/verification/2026-05-09-sample-shop-fsm-smoke)

## Build Verification

Command:

```bash
./gradlew test :sample-shop:assembleDebug --warning-mode all --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

## Journey

Device:

- `emulator-5556`

Actions:

- Installed and launched `sample-shop-debug.apk` through `android run`.
- Verified auth start screen with `android layout`.
- Switched to register mode.
- Created account for `Mina`.
- Verified catalog loaded with user `Mina`.
- Opened product registration.
- Entered product draft `JourneyMug`.
- Submitted for review.
- Verified first mock review rejection.
- Resubmitted for review.
- Verified approval.
- Published product.
- Verified published state.
- Returned to catalog.
- Verified `JourneyMug` appears with `$21.50`.

## Result

Passed.

Important layout evidence:

- `catalog-after-signup.json`: contains `Products`, `Mina`, and `Register product`.
- `product-editor-rejected.json`: contains `Status: Review rejected`.
- `product-editor-approved.json`: contains `Status: Review approved`.
- `product-editor-published.json`: contains `Status: Product published`.
- `catalog-after-publish.json`: contains `JourneyMug`, `$21.50`, and `Durable commuter mug`.
