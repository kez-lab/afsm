# Raw Sources

This directory stores immutable source material for the project wiki.

- `conversations/2026-05-01-android-viewmodel-fsm-discussion.md`: source conversation that led to the Android FSM direction.
- `sources/2026-05-01-llm-wiki-pattern.md`: user-provided LLM Wiki pattern used to structure this project knowledge base.
- `verification/2026-05-09-sample-shop-fsm-smoke/`: Android CLI layout/screenshot evidence for the sample-shop auth and product registration FSM smoke test.
- `verification/2026-05-09-product-editor-transition-action-rename-smoke/`: Android CLI layout/screenshot evidence after renaming ProductEditor phase states and transition action commands.
- `verification/2026-05-09-product-editor-executable-dsl-smoke/`: Android CLI layout/screenshot evidence after migrating ProductEditor to the executable DSL.
- `verification/2026-07-10-first-use-api-experiment/`: Kotlin compile, behavior, and diagnostic evidence for the first pre-release declaration redesign prototypes.
- `verification/2026-07-10-graphable-machine-properties/`: KSP, sample, graph, API, and clean external-consumer evidence for direct top-level machine properties.
- `verification/2026-07-10-dynamic-initial-state-safety/`: Core, ViewModel, compiler-diagnostic, Checkout, API, graph, and external-consumer evidence for separating default and runtime-supplied machine state.
- `verification/2026-07-10-checkout-product-goal-fit/`: constrained machine/graph/tests-only evidence for whether Checkout supports Afsm's readability and safety goal.
- `verification/2026-07-10-checkout-first-use-protocol-dry-run/`: facilitator setup and rubric-grounding checks for the human Checkout first-use protocol; not a participant result.
- `verification/2026-07-10-checkout-viewmodel-integration/`: dynamic product id, real repository command-result, durable completion/effect, failure, full release-gate, and external-consumer evidence for Checkout's Android adapter.
- `verification/2026-07-10-checkout-process-restoration/`: red/green tests, minimal SavedStateHandle keys, unknown-payment protection, graph/APK/release-gate results, and Android CLI device-discovery boundary.
- `verification/2026-07-11-diagnostic-privacy/`: credential-like red/green fixtures, types-only default diagnostics, explicit raw-value opt-in, API dump, full release-gate, and external-consumer evidence.
- `verification/2026-07-11-phase-owned-invocation/`: sequential cancel-command contradiction, red/green invocation tests, ProductEditor cooperative upload cancellation, graph/API checks, and external-consumer evidence.
- `verification/2026-07-11-product-editor-upload-boundary/`: injected suspend uploader red/green tests, safe failure/cancellation mapping, graph/APK/release gate, and exact Android CLI device-discovery failure.

Raw files are source-of-truth evidence. The maintained synthesis lives in `wiki/`.
