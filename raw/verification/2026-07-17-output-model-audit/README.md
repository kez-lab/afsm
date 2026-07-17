# Afsm Output Model and MVI Surface Audit

Date: 2026-07-17

Audited commit: `be910de`

## Question

Does the current `Command` plus optional `Effect` model earn its concept cost in
the maintained library and representative samples, and do the samples show
that Afsm can fit ordinary Android ViewModel APIs without imposing MVI-shaped UI
contracts?

## Current Surface

- `AfsmMachine<S, E, C, F>` and `AfsmReducer<S, E, C, F>` require an Effect
  type argument even when the feature has no UI one-shot output.
- `AfsmTransition<S, C, F>` carries `commands`, `commandInvocations`, and
  `effects`.
- The DSL, topology, runtime config, host, test helpers, Compose helper, API
  dumps, and public docs all have Effect-specific surface.
- `AfsmNoEffect` is required as a marker for machines without effects.
- `afsm-compose` contains only the lifecycle-aware effect collector.

## Repository Measurements

The maintained source audit found:

| Measurement | Baseline |
|---|---:|
| Maintained Kotlin files matched by the Effect API audit | 42 |
| `AfsmNoEffect` occurrences across maintained source/tests | 50 |
| Representative sample Effect sealed types | 3 |
| Representative sample Effect emissions | 3 |
| Direct feature Event constructions in sample UI files | 18 |
| Effect-specific library module | 1 (`afsm-compose`) |

Commands used:

```bash
rg -l 'AfsmNoEffect|AfsmEffectDelivery|CollectAfsmEffects|\beffect\s*\(|\.effects\b|[A-Za-z]+Effect' \
  afsm-core afsm-runtime afsm-viewmodel afsm-compose afsm-test sample-shop consumer-smoke \
  --glob '*.kt' --glob '!**/build/**'

rg -F 'AfsmNoEffect' \
  afsm-core afsm-runtime afsm-viewmodel afsm-compose afsm-test sample-shop consumer-smoke \
  --glob '!**/build/**'

rg -o 'AuthEvent\.|CheckoutEvent\.|ProductEditorEvent\.' \
  sample-shop/src/main/kotlin/afsm/sample/shop/feature/{auth,checkout,editor}/*Screen.kt
```

## Representative Sample Findings

| Feature | Effect | Existing state/UI alternative | Finding |
|---|---|---|---|
| Auth | `OpenCatalog` | `Authenticated(session)` | The effect duplicates a durable successful-auth phase. |
| Checkout | `PaymentCompleted(orderId)` | `Completed(orderId)` | The effect duplicates the durable, restored completion phase. |
| ProductEditor | `CloseEditor` after `DoneClicked` | The Done button's UI callback | The event enters the machine only to return a UI action to the same UI. |

All three canonical samples therefore teach Effect even though none requires an
Afsm-owned UI output channel.

The sample screens also construct sealed machine events and call a generic
`ViewModel.onEvent(event)`. That is a valid UDF shape but makes Afsm look like a
full MVI UI contract. Android's ordinary verb-named ViewModel method shape is
not demonstrated.

## Alternatives

| Alternative | Concept reduction | Safety/readability result |
|---|---|---|
| Add more Command/Effect explanation | None | Keeps the human-reported learning cost and lossy stream. |
| Rename Effect to `UiAction` | None | Improves direction naming but keeps the generic, channel, module, and delivery policy. |
| Merge Command and Effect into one output | Partial | Mixes host execution with UI lifecycle delivery and requires routing conventions. |
| Remove Effect from Afsm | Strong | Leaves one typed host-work output; uses state or UI callbacks for UI behavior. |

## Selected Hypothesis

Removing Effect from Afsm will reduce the public mental model and MVI impression
without losing a required sample behavior. `Command` remains because executing
repository/use-case work inside a pure transition, or launching it by observing
state, would reintroduce lifecycle and duplicate-execution ambiguity.

The hypothesis succeeds when the public API loses the Effect generic/channel,
the Effect-only module disappears, all representative UI behavior uses state or
direct UI callbacks, sample UI constructs no machine Event objects, and the
machine/graph/tests reading contract is explained and verified together.
