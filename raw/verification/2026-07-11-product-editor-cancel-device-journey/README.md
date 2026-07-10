# ProductEditor Cancel Upload Device Journey

Date: 2026-07-11

Android CLI version: `1.0.15433482`

AVD: `medium_phone` (`emulator-5554`)

APK: `sample-shop/build/outputs/apk/debug/sample-shop-debug.apk`

## Purpose

Retry the ProductEditor cancel-upload journey that previously stopped at
`android run`, and verify the visible phase-owned cancellation path on an
Android emulator without changing production code.

## Device-Lifetime Finding

The earlier attempt started the emulator in one short-lived command session
and ran the APK from another. The emulator had completed boot according to its
log, but it was no longer alive when the second session looked for it.

Keeping the official emulator command and `android run` in one persistent shell
session preserved the process lifetime:

```text
android emulator start --cold medium_phone
Virtual device successfully started as 'emulator-5554'

android run --device=emulator-5554 \
  --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk \
  --activity=.MainActivity
Installation completed successfully
Activation completed successfully
```

This is evidence about the local command-runner process boundary, not a change
to the Afsm sample or an Android CLI configuration fix.

## Journey Result

```text
<journey name="ProductEditor cancel upload">
  <action status="PASSED">Build and locate sample-shop debug APK</action>
  <action status="PASSED">Start medium_phone with Android CLI in a persistent shell</action>
  <action status="PASSED">Install and launch with android run</action>
  <action status="PASSED">Create a sample account and open Register product</action>
  <action status="PASSED">Enter a valid title, description, and price</action>
  <action status="PASSED">Submit and observe Uploading mock images plus Cancel upload</action>
  <action status="PASSED">Tap Cancel upload</action>
  <action status="PASSED">Verify Editing draft with the original draft retained</action>
</journey>
```

App installation and activation used `android run`. Android CLI has no input
subcommand in this installed version, so layout-derived tap/text interactions
used `platform-tools/adb shell input`; `adb install` was not used.

## Assertions

- `06-editor-filled-layout.json` proves the valid draft before submission.
- `07-upload-in-progress.png` visibly shows
  `Status: Uploading mock images`, disabled fields, `Cancel upload`, and
  `Processing...`.
- `08-after-cancel-layout.json` shows `Status: Editing draft`, the original
  draft values, `Save draft`, and `Submit for review` after cancellation.
- `08-after-cancel.png` visually confirms the restored editable UI.
- The existing controllable JVM test remains the authoritative proof that a
  cancelled uploader callback cannot publish a late success; this device
  journey proves the user-visible path is reachable and tappable.

## Captures

- `01-launch-layout.json`, `01-launch.png`: launched Auth screen.
- `02-register-layout.json`: registration mode.
- `03-register-filled-layout.json`: filled sample account.
- `04-catalog-layout.json`: authenticated Catalog with Register product action.
- `05-editor-layout.json`: initial ProductEditor.
- `06-editor-filled-layout.json`, `06-editor-filled.png`: valid draft.
- `07-upload-in-progress.png`: visible cancellable upload state.
- `08-after-cancel-layout.json`, `08-after-cancel.png`: editable state after
  cancellation.

## Evidence Boundary

This proves emulator installation/launch, visible cancellation affordance, tap
reachability, retained draft data, and return to `EditingDraft` for the demo
uploader. It does not prove that a real network request, server, or callback SDK
stops remote work. It is automated repository/device evidence, not a human
first-use session or production-like pilot.
