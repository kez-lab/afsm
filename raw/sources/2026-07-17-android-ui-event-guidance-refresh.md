# Android UI Event Guidance Refresh

Accessed: 2026-07-17

This note refreshes the official Android constraints used for the Afsm output
model decision. It records the relevant guidance without copying the source
pages wholesale.

## Sources

- [UI events](https://developer.android.com/topic/architecture/ui-layer/events)
- [UI state production](https://developer.android.com/topic/architecture/ui-layer/state-production)
- [State holders and UI state](https://developer.android.com/topic/architecture/ui-layer/stateholders)

## Current Guidance Used

- User interactions that require business logic are normally delegated to
  verb-named ViewModel functions. UI behavior such as navigation or showing a
  message can be handled directly by the UI.
- Actions originating from a ViewModel should result in UI state updates so the
  result is reproducible and not lost during configuration changes.
- Events are transient inputs to state production; state is the durable output
  consumed by UI.
- Navigation is UI logic. The UI may react to screen state to choose how and
  when navigation occurs.
- Lifecycle-dependent UI behavior remains in the UI layer rather than being
  moved into a screen-level business state holder merely to centralize it.

## Afsm Constraint

An Afsm-owned best-effort `Effect` stream is not required by current Android
guidance. Business outcomes should be represented in machine state. UI-originated
UI behavior can invoke a callback directly; UI behavior that follows a business
outcome can react to durable state, with a feature-owned acknowledgement state
only when repeated handling must be controlled.
