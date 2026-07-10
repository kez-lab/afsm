---
title: ProductEditor Upload Boundary Fresh-Use Review
updated: 2026-07-11
status: repository-pass-device-pass
---

# ProductEditor Upload Boundary Fresh-Use Review

## Scope

Review the new ProductImageUploader, ProductEditorViewModel handler, route
composition, and ViewModel tests as an Android developer integrating real
phase-owned work.

This is an AI repository review plus a scripted emulator journey. It is not a
human or real-network usability review.

## What Became Clearer

- The machine still expresses ownership through `invoke`; the ViewModel now
  shows the application boundary separately as `imageUploader.upload(draft)`.
- Success, ordinary failure, and cancellation have three explicit paths.
- The fixed UI error avoids leaking backend exception details.
- ProductEditorRoute makes the sample dependency visible instead of relying on
  a hidden ViewModel default.
- A controllable fake proves start and cancellation directly; the test no
  longer depends on winning a 250 ms race.
- No generic Afsm transport/service abstraction or ViewModel `Job` registry was
  added.
- The emulator journey shows that the 2 second demo window exposes
  `Uploading mock images` and `Cancel upload`, and that tapping cancel returns
  to `Editing draft` with the draft preserved.

## What Became Harder

- ProductEditor adds one interface, one mock implementation, and one constructor
  dependency.
- The 2 second mock delay is an explicit demo assumption, not measured human
  evidence.
- An independently thrown `CancellationException` leaves the machine in its
  current phase; in normal use cancellation should come from phase exit or host
  closure.
- Progress, resumable upload, remote abort, and request identity remain outside
  this sample boundary.

## Verdict

Accept the injected boundary. The extra interface earns its cost by matching
the Android integration shape a real team must test, while leaving Afsm's
runtime API unchanged. Do not generalize it into an Afsm upload API.

The device journey no longer blocks this sample claim. The next evidence step
is not another repository abstraction: use a real transport/SDK in the
production-like pilot and record human comprehension of `invoke` and its local
versus remote cancellation boundary.
