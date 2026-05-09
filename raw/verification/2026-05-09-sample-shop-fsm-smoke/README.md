# Sample Shop FSM Smoke Verification

Date: 2026-05-09

Device:

- `emulator-5556`

APK:

- `sample-shop/build/outputs/apk/debug/sample-shop-debug.apk`

Journey:

```text
Register account
-> open catalog
-> open product registration
-> enter product draft
-> submit for review
-> observe first mock rejection
-> resubmit
-> observe approval
-> publish
-> close editor
-> verify product appears in catalog
```

Commands used:

```bash
adb -s emulator-5556 uninstall afsm.sample.shop
android run --device=emulator-5556 --apks="/Users/kwak-euijin/Documents/New project 2/sample-shop/build/outputs/apk/debug/sample-shop-debug.apk" --activity=.MainActivity
android layout --device=emulator-5556 --pretty --output=...
adb -s emulator-5556 shell input tap ...
adb -s emulator-5556 shell input text ...
android screen capture --output=artifacts/android-cli/catalog-after-publish.png
```

Evidence files:

- `auth-start.json`
- `auth-register.json`
- `catalog-after-signup.json`
- `product-editor-start.json`
- `product-editor-filled.json`
- `product-editor-rejected.json`
- `product-editor-approved.json`
- `product-editor-published.json`
- `catalog-after-publish.json`
- `catalog-after-publish.png`

Result:

- Passed.
- `catalog-after-signup.json` shows authenticated user `Mina`.
- `product-editor-rejected.json` shows `Status: Review rejected`.
- `product-editor-approved.json` shows `Status: Review approved`.
- `product-editor-published.json` shows `Status: Product published`.
- `catalog-after-publish.json` shows `JourneyMug` with `$21.50`.
