---
title: Afsm Phase-Owned Invocation Experiment
updated: 2026-07-11
status: candidate-d-implemented
---

# Afsm Phase-Owned Invocation Experiment

## Problem

Afsm currently documents user-triggered cancellation as an `onExit` cancel
command plus request ids. That contract does not work with the implemented
runtime:

1. ordinary commands execute in one sequential command processor,
2. a long-running upload suspends that processor,
3. a cancel command emitted by a later transition waits behind the upload,
4. the cancel command runs only after the work it intended to cancel finishes.

The existing `Handled` runtime test proves a cleanup command can execute when
no earlier command is blocking the processor. It does not prove interruption of
active work. Request ids reject stale results but do not stop unnecessary local
work, network use, or SDK activity.

## Product Scenario

Use ProductEditor image upload as the realistic proof:

```text
EditingDraft
-> SubmitClicked
-> ImageUploadInProgress
-> user chooses Cancel upload
-> EditingDraft
```

The machine, graph, and tests should show that image upload belongs to
`ImageUploadInProgress` and is cancelled by leaving that phase. The ViewModel
should not need a manual `Job` map.

## Invariants

- Ordinary commands remain sequential and bounded.
- A phase-owned long-running command starts without blocking later event
  reduction or ordinary command routing.
- Leaving its owning phase cancels the local coroutine before later transition
  work can treat it as active.
- Cancellation uses Kotlin cooperative cancellation and is never reported as a
  command failure diagnostic.
- Host closure cancels every active invocation.
- The same invocation key cannot silently replace an active invocation.
- A cancelled invocation cannot dispatch a result event through the Afsm-owned
  dispatch callback.
- Request/correlation ids remain required when remote work, SDK callbacks, or
  non-cooperative code can outlive local coroutine cancellation.
- The graph makes both invocation start and phase-exit cancellation visible.
- No Android type enters `afsm-core` or `afsm-runtime`.

## Candidate A: Keep Queued Cancel Commands

Verdict: rejected. It reads clearly in the machine but cannot interrupt the
currently executing command under the sequential runtime. Keeping this guidance
would be a false safety promise.

## Candidate B: Launch And Track Jobs In Each ViewModel

The command handler could launch a second `viewModelScope` job, return
immediately, store it in a map, and cancel it from a later command.

Verdict: rejected as the product default. It duplicates job ownership in every
feature and moves cancellation, failure policy, and result-delivery safety
outside the runtime that claims to own command execution.

## Candidate C: Make Every Command Concurrent

Add a global concurrent command policy and let application code find/cancel
matching jobs.

Verdict: rejected. It weakens predictable command ordering for unrelated
repository/database work, makes bounded queue meaning unclear, and still needs
an application-owned job registry or cancel API.

## Candidate D: Bounded Phase-Owned Invocation

Add a separate lifecycle-aware command path:

```kotlin
val ImageUpload = AfsmInvocationKey("product-editor/image-upload")

phase(ProductEditorPhase.ImageUploadInProgress) {
    onEnter {
        invoke(
            key = ImageUpload,
            label = "StartImageUpload",
        ) {
            ProductEditorCommand.StartImageUpload(data.draft)
        }
    }

    on<ProductEditorEvent.CancelUploadClicked> {
        transitionTo(ProductEditorPhase.EditingDraft)
    }
}
```

`invoke` is initially allowed only in `onEnter`. It emits a keyed
`AfsmCommandInvocation.Start`. The executable machine automatically emits
`AfsmCommandInvocation.Cancel` when a transition exits the owning phase.

The runtime executes the start through the existing `AfsmCommandHandler` in a
tracked child job. Cancellation is processed directly from accepted transition
output, not queued behind ordinary commands. No feature-level cancel command or
ViewModel `Job` map is needed.

Verdict: selected for a breaking pre-release prototype. It adds one concept
only where long-lived phase-owned work needs it and preserves the ordinary
command path for short sequential work.

## Candidate E: Full Actor/Service Runtime

Add nested actors, callbacks, restart strategies, hierarchical ownership, and
general service lifecycle semantics.

Verdict: defer. It is much broader than the Android screen-flow evidence and
would make the first-use model harder before the bounded invocation proves its
value.

## Proposed Public Contract

```kotlin
@JvmInline
value class AfsmInvocationKey(val value: String)

sealed interface AfsmCommandInvocation<out C : Any> {
    val key: AfsmInvocationKey

    data class Start<C : Any>(
        override val key: AfsmInvocationKey,
        val command: C,
    ) : AfsmCommandInvocation<C>

    data class Cancel(
        override val key: AfsmInvocationKey,
    ) : AfsmCommandInvocation<Nothing>
}

class AfsmTransition<out S : Any, out C : Any, out F : Any> {
    val commands: List<C>
    val commandInvocations: List<AfsmCommandInvocation<C>>
}
```

Ordinary `command(...)` output and phase-owned invocation output stay separate
so tests and runtime policy cannot confuse sequential work with cancellable
long-lived work.

## Runtime Order

For a phase-changing accepted event:

```text
machine: source invocation cancellation
-> source onExit
-> event case actions
-> target phase factory
-> target onEnter invocation start

host: publish next state
-> emit effects
-> apply invocation cancels/starts in machine order
-> enqueue ordinary commands
```

The source cancel appears before the target start. Reusing one key across two
phases therefore cancels the old job before starting the new one.

## Acceptance Criteria

1. A red runtime test reproduces that an active upload must be cancelled while
   later UI events continue reducing.
2. `invoke` is available only from `onEnter` in the first prototype.
3. A machine transition into a phase exposes `Start(key, command)`; leaving the
   phase exposes `Cancel(key)` automatically.
4. Duplicate invocation keys in one phase definition fail machine validation.
5. Runtime cancellation interrupts a cooperative suspended handler and emits
   no command-failure diagnostic or result event.
6. An ordinary command still executes sequentially while an invocation is
   active.
7. Host closure cancels active invocations.
8. ProductEditor exposes `CancelUploadClicked`, returns to `EditingDraft`, and
   no longer needs a `CancelImageUpload` command or ViewModel job registry.
9. Flow/Full graphs identify the invoked upload and automatic exit
   cancellation.
10. Core/runtime/test/ViewModel/sample/API checks and the full external-consumer
    release gate pass.

## Evidence Boundary

This prototype can prove local cooperative coroutine cancellation and readable
ownership in the reference flow. It cannot prove a remote server stopped work,
an arbitrary SDK honors cancellation, or real Android developers prefer the
new term. Request ids, idempotency, and a human fresh-use review remain required
evidence.

## Implementation Result

Candidate D is implemented in `b0fd60a`:

- `AfsmInvocationKey` and `AfsmCommandInvocation.Start/Cancel` are public core
  types,
- `AfsmTransition` separates ordinary `commands` from
  `commandInvocations`,
- `invoke` is limited to `onEnter`, and phase exit automatically emits cancel
  before the next phase starts its invocation,
- ordinary commands remain sequential while invocation jobs run concurrently,
- cancellation and host closure stop cooperative jobs without failure
  diagnostics,
- cancelled jobs cannot dispatch through the Afsm-owned callback,
- ProductEditor exposes `CancelUploadClicked` and a cancel UI action without a
  ViewModel job registry,
- topology notes show invocation start and automatic cancellation,
- API checks and the full Maven Local external-consumer release gate pass.

The result proves the bounded local contract, not remote cancellation. The
fresh-use review accepts it provisionally and keeps key ceremony plus real
upload integration open.
