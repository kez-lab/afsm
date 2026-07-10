---
title: ProductEditor Upload Boundary Experiment
updated: 2026-07-11
status: candidate-b-selected
---

# ProductEditor Upload Boundary Experiment

## Problem

Phase-owned invocation is verified inside `AfsmHost`, but the ProductEditor
Android adapter still implements image upload as a hardcoded `delay(250)` in the
ViewModel followed by a fixed token. Its ViewModel test proves that this delay
is cancellable, not that a repository/use-case boundary cooperates with
cancellation.

The 250 ms mock also makes the visible `Cancel upload` state too short to serve
as a reliable sample journey target. Android CLI device verification could not
evaluate it because `android emulator start medium_phone` reported
`emulator-5554` ready while every `android run` form still reported no matching
online device.

## Product Hypothesis

A feature-owned suspend uploader injected into ProductEditorViewModel will make
the Android integration more realistic and more testable without adding a new
Afsm API. A controllable fake can prove start, cooperative cancellation,
failure mapping, and absence of a late result. A slower default mock can make
the cancel UI observable in a future device journey.

## Candidate A: Keep The Hardcoded Delay

Verdict: rejected. It is concise, but it proves only cancellation of
`kotlinx.coroutines.delay` and hides the boundary a real Android team must
implement.

## Candidate B: Inject A Feature-Owned Suspend Uploader

```kotlin
fun interface ProductImageUploader {
    suspend fun upload(draft: ProductDraft): String
}
```

The ViewModel command handler invokes it and maps success/failure to typed
events. `CancellationException` is rethrown. The app route supplies a default
mock uploader; tests supply controllable fakes.

Verdict: selected. It matches Android repository/use-case practice, keeps the
machine and runtime unchanged, and lets tests prove the integration contract
without wall-clock timing.

## Candidate C: Put Upload Into ProductRepository

Verdict: rejected. ProductRepository currently owns Room-backed product
records. Mixing transient image transfer into it would blur data
responsibilities only to avoid one small feature interface.

## Candidate D: Add A Generic Upload/Service API To Afsm

Verdict: rejected. Afsm already provides lifecycle ownership through
`invoke`; transport APIs, progress protocols, retries, and remote cancellation
belong to application repositories/use cases until multiple pilots show a
repeatable runtime abstraction.

## Accepted Contract

- `ProductImageUploader.upload` is a main-safe suspend boundary supplied to the
  ViewModel.
- Its success token becomes `ImageUploadSucceeded`.
- Non-cancellation failures become `ImageUploadFailed` with a safe display
  message.
- `CancellationException` is always rethrown and produces no failure event.
- The ViewModel owns no `Job` map; `AfsmHost` still owns invocation lifetime.
- The test fake signals start, suspends, and records cancellation in `finally`.
- Tests do not depend on a 250 ms race or real elapsed time.
- The default sample uploader waits long enough for a person or journey tool to
  observe and tap `Cancel upload`.

Assumption: 2 seconds is an adequate demo-only visibility window. This is not a
production timeout or Afsm runtime policy and remains subject to device/human
evidence.

## Acceptance Criteria

1. A red ViewModel test fails because no uploader can be injected.
2. A controllable fake proves upload starts after entering
   `ImageUploadInProgress`.
3. `CancelUploadClicked` cancels that fake and state remains `EditingDraft`
   after the old completion opportunity.
4. A non-cancellation uploader failure becomes `ImageUploadFailed` and returns
   the machine to `EditingDraft` with an error.
5. No `CancellationException` is converted to a domain failure.
6. ProductEditorRoute supplies the default mock explicitly.
7. Machine/graph output remains unchanged.
8. ProductEditor ViewModel tests, sample tests, APK assemble, API checks, and
   the relevant local release gate pass.
9. Android CLI installation/journey is retried only when the official tool can
   see an online device; ADB installation is not used as substitute evidence.

## Evidence Boundary

This experiment can prove a realistic local suspend boundary and cancellation
propagation. The default mock is not a real upload backend. It cannot prove a
server stopped work, bytes stopped transferring, or an SDK callback obeyed
cancellation. A production-like pilot still needs a real transport contract,
request identity, and backend semantics.
