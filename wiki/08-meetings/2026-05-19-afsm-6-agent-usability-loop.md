---
title: Afsm 6-Agent Usability Loop
date: 2026-05-19
---

# Afsm 6-Agent Usability Loop

## Prompt

Run six Android-developer usability reviewers, implement based on their feedback,
then run six reviewers again.

## First Review Findings

- README first contact felt like maintainer verification, not first-use
  onboarding.
- Minimal Draft exposed graph metadata and heavy generics too early.
- `state(Phase.Saved) { }` looked like boilerplate for terminal states.
- Auth leaked `AuthState` into `AuthScreen`; Checkout had a clearer render-state
  boundary.
- ProductEditor needed a concrete explanation of transition-block versus target
  `onEnter` execution order.
- Graph generation still felt less automatic than the product goal because app
  modules need an export test and Gradle task.
- Internal beta adoption lacked a pilot contract.

## Implemented Changes

- Added no-block `state(phase)` and `state<PayloadPhase>()` DSL convenience for
  terminal or marker phases.
- Reworked README toward a first-use path: install, minimal machine, ViewModel,
  test, then graph generation.
- Renamed the README draft result event to `DraftSaveCompleted`.
- Moved graph metadata labels out of the minimal example.
- Added a dedicated `docs/graph-generation.md` copy-paste setup.
- Changed Auth UI to consume `AuthRenderState`, and made authenticated render
  state explicit.
- Added `CheckoutPrimaryAction` so UI actions are explicit render-state output.
- Documented ProductEditor transition execution order and payload phase lambda
  roles.
- Added an internal beta adoption contract and pilot checklist.

## Second Review Findings

Accepted improvements:

- First-use path is meaningfully simpler.
- `state(phase)` is worth the public API addition because it removes empty
  terminal-state blocks without adding a new concept.
- Auth/Checkout render-state direction is clearer for Android sample copying.
- ProductEditor order explanation is enough, with a small clarification needed
  for payload phase lambda versus transition block.
- Internal beta contract is sufficient for controlled pilots.

Follow-up fixes applied:

- Checkout payment-in-progress now keeps a disabled primary button so
  `Processing...` remains visible.
- `docs/examples.md` now uses `DraftSaveCompleted` consistently.
- Public API docs clarify exact singleton phase declarations versus payload
  phase scopes.
- `docs/sample-shop-afsm-guide.md` no longer implies KSP writes `.mmd` files by
  itself.

## Remaining Priority

The largest adoption blocker remains first-class graph generation ergonomics.
The current pre-release workflow is documented, but a dedicated Gradle plugin is
still the right direction before broad external adoption.

## Third Review Findings

The follow-up six-agent round focused on external Android developer usability.

Accepted findings:

- Graph generation should feel automatic after `@AfsmGraph`; app-maintained
  export tests and task wiring are too much for beta adoption.
- The existing KSP registry is the correct source of graph truth; the next
  slice should hide build wiring, not parse Kotlin bodies or introduce sample
  data.
- `sample-shop` graph export assertions should be registry-driven instead of
  naming only selected machines.
- ProductEditor, the most complex sample, should match Auth/Checkout by mapping
  internal FSM phase to a Compose render state.
- Release gate should include `afsm-graph-ksp:test`, and consumer smoke should
  verify graph generation rather than registry compilation only.

Deferred findings:

- Processor compile-testing for invalid annotations and duplicate ids remains
  important but is a separate hardening task.
- Multi-variant and multi-module graph aggregation should wait until the single
  app-module plugin path is stable.
- Graph API module splitting should be decided before public OSS release, not
  inside this usability slice.

## Third Round Implemented Changes

- Added `io.github.afsm.graph` as the first graph Gradle plugin slice.
- The plugin generates `AfsmGeneratedMmdExportTest`, wires unit-test source
  generation, configures `afsm.mmd.outputDir`, and registers `generateAfsmMmd`.
- `sample-shop` now applies the plugin and no longer owns a hand-written MMD
  export test.
- `consumer-smoke` now applies the published plugin and runs
  `:app:generateAfsmMmd`.
- `scripts/verify-release-local.sh` now runs `afsm-graph-ksp:test` and the
  graph plugin build test.
- ProductEditor now uses `ProductEditorRenderState` so Compose does not branch
  on `ProductEditorPhase`.

Post-change review fixes:

- `generateAfsmMmd` is now a dedicated `Test` task that only runs the generated
  graph export test; it does not depend on the whole app unit-test task.
- The generated export test is JUnit4-compatible, and the plugin no longer
  forces JUnit Platform on existing app tests.
- README and graph docs now include Maven Local plugin publication and
  `pluginManagement.repositories.mavenLocal()` guidance.
- ProductEditor published render state hides draft fields, and render mapping
  tests cover published and processing states.
- Sample-shop tests now assert the documented Auth, Checkout, and ProductEditor
  graph registry entries.

## Updated Remaining Priority

The next strongest hardening target is graph tooling verification: add processor
compile tests for invalid annotations and duplicate ids, then add a Gradle
plugin functional test that proves the generated export test/task in a fixture
Android module.

## Fourth Review Findings

Six Android-developer reviewers focused on whether the first graph plugin slice
was trustworthy enough for external Android projects.

Accepted findings:

- KSP generation needed executable coverage, not only string utility tests.
- The graph plugin needed real Android app/library fixture tests because
  `afsm-graph-gradle-plugin:test` previously had little functional value.
- `generateAfsmMmd` must stay separate from normal `testDebugUnitTest`; users
  should not see graph export behavior during ordinary unit tests.
- Applying the graph plugin before adding any `@AfsmGraph` source should not
  break normal unit tests. Running `generateAfsmMmd` in that state should fail
  with a clear message.
- Consumer smoke should verify the generated `.mmd` file, not only that the
  task ran.

Deferred findings:

- Multi-flavor/variant examples remain documentation work after the single
  variant path is stable.
- Plugin/processor version synchronization needs a release-policy decision
  before remote Maven publication.
- Dedicated graph source sets could further reduce test dependency mixing, but
  they are not necessary for the current internal beta gate.

## Fourth Round Implemented Changes

- Added KSP functional tests that run a real Kotlin/KSP fixture and verify
  registry generation for object and no-required-arg class machines.
- Added KSP failure fixtures for invalid graph sources, required constructor
  parameters, unsafe file names, duplicate ids, and duplicate file names.
- Added Gradle TestKit functional tests for Android app and library modules.
- Removed runtime AGP type references from the graph plugin so TestKit and
  consumer builds do not depend on AGP classes being visible in the plugin
  classloader.
- Changed the generated export test to load the registry reflectively. Normal
  unit tests can compile before any `@AfsmGraph` exists; `generateAfsmMmd`
  fails clearly if no registry was generated.
- Excluded the generated export test from the normal Android unit-test task.
- Added consumer-smoke shell validation that `ConsumerSmoke.mmd` exists and
  starts with `stateDiagram-v2`, including representative transition lines.
