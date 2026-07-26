# Restoration, Command, and UI Policy

## Restoration restores business truth

Reconstruct the safest known business state from navigation input and persisted
data. Do not automatically repeat unsafe work merely because a `ViewModel` was
recreated.

Before persisting or restoring a command phase, write the recovery rule in
product terms. If the repository or backend cannot prove the old work completed,
restore an editable or explicit unknown state instead of pretending the command
is still safely running.

Checkout examples:

- persisted completed order -> `Completed(orderId)`,
- persisted pending payment with unknown outcome ->
  `PaymentStatusUnknown(requestId)`,
- no persisted progress -> `Idle`, followed by normal screen entry.

## Commands are execution requests

Machines emit command values; Android hosts execute them. Command results return
as events. State observation itself must not trigger repository work.

Sequential commands preserve accepted order. Phase-owned `invoke` work is
cancelled when its owning phase exits.

## UI behavior

Use this order:

1. If it is product progress, represent it in state.
2. If the UI initiated a UI-only action, use a direct callback.
3. If an async business result should cause navigation, let the route react to
   durable completion state.
4. If repeated handling is unsafe, model pending/acknowledged state explicitly.

There is no separate best-effort one-shot output channel.

## SavedStateHandle ownership

`ViewModel` owns `SavedStateHandle`. Plain Kotlin machines receive a restored
state and remain Android-independent. Persist only the identifiers necessary to
reconstruct business progress; repository data remains the authority for domain
objects.

Initial state construction does not execute `onEnter`. A restored state should
therefore represent already-known business truth, not a hidden request to start
new repository work.

## Unsafe work rule

Payments, publishing, and other non-idempotent operations need a restoration
policy before they are started. If completion cannot be determined after
process death, prefer an explicit unknown state over automatic retry.
