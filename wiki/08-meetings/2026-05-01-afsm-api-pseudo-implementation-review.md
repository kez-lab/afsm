---
title: Afsm API Pseudo Implementation Review
updated: 2026-05-01
---

# Afsm API Pseudo Implementation Review

## Meeting Context

Date: 2026-05-01

Topic: Review whether `AfsmTransition<S, C, F>` is too verbose and whether the signup identity retry pseudo-implementation is suitable as a basis for implementation.

Input documents:

- [[../03-engineering/afsm-public-api-draft|Afsm Public API Draft]]
- [[../03-engineering/reference-flow-signup-identity-retry|Reference Flow - Signup Identity Retry]]
- [[../03-engineering/signup-state-machine-pseudo-implementation|Signup StateMachine Pseudo Implementation]]
- [[../03-engineering/android-official-guidance|Android Official Guidance]]

Review participants:

- Android architecture reviewer
- Kotlin library/API reviewer
- Main project agent synthesis

## Shared Conclusion

Recommendation: approve with changes.

The reviewers agree that `AfsmTransition<S, C, F>` is not disqualifyingly verbose if the project documents feature-local typealiases as the standard convention.

The larger issue is not generic length. The larger issue is incomplete runtime/API semantics around:

- same-state accepted transitions,
- effect delivery,
- event dispatch serialization,
- command execution behavior,
- saved state restoration pattern.

Implementation should not start until these API points are revised in the draft and accepted.

## Point 1: Generic Verbosity

Finding:

```kotlin
AfsmTransition<SignupState, SignupCommand, SignupEffect>
AfsmStateMachine<SignupState, SignupEvent, SignupCommand, SignupEffect>
```

is noisy at declaration sites.

However, feature-local aliases make the implementation readable:

```kotlin
private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand, SignupEffect>

private typealias SignupMachine =
    AfsmStateMachine<SignupState, SignupEvent, SignupCommand, SignupEffect>
```

Conclusion:

- Keep the generic shape for now.
- Require examples and docs to show typealiases.
- Do not create a DSL only to hide generics.

## Point 2: `Stayed` Is Needed

The pseudo-implementation exposed an API gap.

Some events are valid and accepted, but the state intentionally remains the same while commands or effects may still be emitted.

Example:

```text
CancelRequested while ReadyForIdentityVerification
-> state may remain stable
-> CancelSignupSession command should run
-> UI owns actual exit navigation
```

Current draft only has:

- `Transitioned`
- `Ignored`
- `Invalid`

Reviewer consensus:

- `Ignored` is too overloaded.
- Add `AfsmDecision.Stayed`.
- Add `Afsm.stay(...)`.
- Keep `ignore(...)` output-free.
- Keep `invalid(...)` output-free initially.

Proposed semantics:

- `Transitioned`: event accepted and transition occurred or outputs are part of forward progress.
- `Stayed`: event accepted, state intentionally unchanged, outputs may be emitted.
- `Ignored`: event irrelevant; no outputs should run.
- `Invalid`: flow/programmer error; no outputs should run.

## Point 3: Effects Should Stay In Core, But Need Lifecycle Semantics

The reviewers agree that `LaunchIdentityVerification` is a real UI-side effect and should not be hidden in ViewModel glue.

Keeping effects in `AfsmTransition` makes this transition explicit:

```text
IdentityVerificationRequestCreated
-> AwaitingIdentityResult
-> LaunchIdentityVerification
```

However, Android lifecycle risk remains:

- if UI is not collecting, a one-shot effect can be dropped;
- if replayed incorrectly, identity verification can relaunch after rotation;
- if collected by multiple consumers, delivery can duplicate.

Required before implementation:

- define whether effects use `SharedFlow`, `Channel`, or another internal mechanism;
- define replay/buffer behavior;
- define lifecycle-aware collection guidance;
- define whether critical UI effects need acknowledgement events.

## Point 4: Dispatch And Command Execution Semantics Are Under-Specified

The reviewers flagged `AfsmHost.dispatch(event)` as not ready for implementation without stricter behavior.

Needed decisions:

- Is `dispatch` synchronous, suspending, or fire-and-forget?
- Are events serialized through a mutex/queue?
- What happens if a command calls `dispatch` while another transition is running?
- Are commands executed sequentially by default?
- How are command exceptions mapped to events?
- How is `CancellationException` handled?
- Can polling commands overlap?

Consensus:

- MVP should use deterministic serialized event processing.
- MVP should use sequential command execution unless a later sample proves more is needed.
- Reentrancy must be explicitly handled.

## Point 5: Saved State Needs A Concrete Pattern

Current saved state policy is directionally correct:

- do not serialize full FSM state by default,
- store minimal keys,
- reconstruct complex state from domain/data layer.

But for an Android library, the pattern needs to be concrete.

Required before implementation:

- define a minimal snapshot model for the signup flow,
- show how `SavedStateHandle` is updated from state changes,
- show how restore produces either an initial state or a restoration command/event,
- keep this as sample guidance unless multiple flows prove a reusable adapter API.

## Point 6: Module And Dependency Direction

Consensus:

- `afsm-core` should stay pure Kotlin and coroutine-free if possible.
- `afsm-runtime` may use coroutines.
- `afsm-viewmodel` should be thin and compositional.
- `afsm-compose` should not be in MVP unless the sample proves repeated safe abstraction.
- No Hilt/Koin/serialization/annotation-processing dependency in MVP.

## Point 7: Public API Stability Risk

The API reviewer noted that public Kotlin `data class` types expose constructor shape, `copy`, `componentN`, and equality behavior.

Implication:

- `AfsmTransition` as a data class is acceptable if `state`, `commands`, `effects`, and `decision` are stable enough.
- `AfsmConfig` may be better as a regular class or builder-style API later because policies are likely to grow.

## Final Review Result

Status: not approved as-is for implementation.

Approved direction:

- keep the `Afsm` name for now,
- keep `AfsmTransition<S, C, F>` with typealias guidance,
- keep effects in core,
- keep ordinary Kotlin reducers,
- avoid DSL in MVP.

Required changes before implementation:

- add `AfsmDecision.Stayed`,
- add `Afsm.stay(...)`,
- define no-effect convention,
- define effect delivery lifecycle semantics,
- define `AfsmHost.dispatch` serialization/reentrancy behavior,
- define command exception/cancellation behavior,
- define concrete signup saved-state restoration pattern.

## Recommendation To CEO

Do not approve implementation yet.

Approve the API direction conditionally, then request one more design pass that revises the public API draft around the required changes above.
