# Graphable Machine Property Verification

Date: 2026-07-10

Purpose: verify Candidate E, where an annotated stable top-level `val` is the
executable machine and graph source without a machine alias, delegated wrapper,
or factory function.

## Implementation Under Test

- `@AfsmGraph` targets classes and properties.
- KSP resolves machine type aliases and generates a direct property reference.
- Private, member, mutable, computed/delegated, and non-machine properties are
  rejected.
- Draft, Auth, Checkout, ProductEditor, and both external consumer graph
  fixtures use direct machine properties.
- Checkout continues to supply navigation `productId` through the host's
  explicit initial state.

## Verification

```bash
./gradlew :afsm-graph-ksp:check :afsm-core:check
./gradlew :sample-shop:testDebugUnitTest \
  --tests 'afsm.sample.shop.feature.auth.AuthStateMachineTest' \
  --tests 'afsm.sample.shop.feature.checkout.CheckoutStateMachineTest' \
  --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' \
  --tests 'afsm.sample.shop.feature.graph.AfsmGraphRegistryTest' \
  :sample-shop:generateAfsmMmd
./scripts/verify-consumer-smoke.sh --no-daemon
./scripts/verify-release-local.sh --no-daemon
```

Result: passed.

The consumer script published snapshots only to Maven Local, then performed a
clean external Android build, unit tests, and graph generation. No remote
artifact was published.

The full local release gate also passed after documentation synchronization.
The existing SDK XML compatibility and Gradle deprecation warnings remained
non-blocking.

## Evidence Boundary

This proves compile, behavior, graph, API-check, and external-consumer
coherence. It does not prove fresh human authoring preference.
