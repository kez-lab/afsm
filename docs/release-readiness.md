# Afsm Release Readiness

This checklist defines the current gate for turning Afsm into a publishable Android/Kotlin library.

## Current Local Release Gate

Run:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

What this proves:

- Core transition and executable machine APIs compile and pass unit tests.
- Runtime dispatch, sequential commands, phase-owned invocation cancellation,
  effects, invalid transitions, and command failure policies pass unit tests.
- Android ViewModel helper compiles and passes unit tests.
- Compose effect helper compiles.
- The sample app compiles and exports `.mmd` graphs from real annotated machines.
- Graph KSP processor functional tests cover registry generation and invalid
  annotation diagnostics.
- Graph Gradle plugin functional tests cover Android app/library fixture usage,
  KSP-missing setup errors, ordinary unit-test separation, and no-registry graph
  generation failures.
- The graph Gradle plugin default processor dependency is generated from the
  shared Afsm version and tested so `io.github.afsm.graph` and
  `afsm-graph-ksp` stay aligned.
- Maven Local publishes all six API-tracked library modules plus the Afsm graph Gradle plugin.
- A separate Android Gradle build consumes the published Maven Local artifacts,
  including the ViewModel AAR, test helper artifact, graph Gradle plugin, KSP
  processor, and the Draft quickstart machine/ViewModel mirrored from
  `docs/getting-started.md`.
- The separate consumer build runs Draft quickstart JVM tests for validation,
  command emission, and save failure recovery against those Maven Local
  artifacts.
- The separate consumer build also runs Draft ViewModel wiring tests with a
  main dispatcher rule so command execution and explicit initial state from
  `SavedStateHandle` are verified outside the root build.
- The separate consumer build also verifies unexpected command handler
  exceptions use `AfsmCommandFailurePolicy` diagnostics instead of being
  modeled as domain failure result events. Its assertion also proves the
  external default diagnostic exposes safe code/type fields and no raw Draft
  values.
- The separate consumer build also compiles a no-command machine using
  `AfsmNoCommand` and hosts it from a ViewModel without a command handler.
- The separate consumer build uses the published `invoke` DSL, runtime, and
  test helper to prove cooperative phase-owned work is cancelled on exit.
- The separate consumer build is cleaned and dependency-refreshed by
  `verify-consumer-smoke.sh` so graph validation does not pass on stale outputs.
- Kotlin explicit API mode is enabled for `afsm-core`, `afsm-runtime`, `afsm-test`, `afsm-viewmodel`, `afsm-compose`, and `afsm-graph-ksp`.
- Binary API dumps are checked for the six API-tracked Afsm library modules.

## Hosted CI

The private GitHub repository is:

```text
https://github.com/kez-lab/afsm
```

The hosted GitHub Actions CI workflow was removed for cost control:

```text
.github/workflows/ci.yml
```

Run the local release gate explicitly when full verification is needed:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

## Internal Beta Adoption Contract

Afsm is currently suitable for controlled internal beta pilots on complex
Android flow screens only.

| Topic | Current contract |
|---|---|
| Distribution | Maven Local snapshot or direct project-module dependency inside this repository |
| Support owner | Product/engineering owner must be assigned before the first pilot starts |
| Allowed usage | Complex transaction or multi-step screens with meaningful phases, retries, async results, or invalid transitions |
| Discouraged usage | Simple data display screens where ordinary `ViewModel + StateFlow` is clearer |
| API stability | Breaking API changes are allowed during internal beta, but they must update docs, API dumps, examples, and migration notes in the same change |
| Rollback | Pilot apps should keep Afsm usage isolated to feature modules so rollback can replace the machine with a local reducer/ViewModel implementation |
| Verification | Pilot branches must pass their app tests plus this repository's local release gate before upgrading Afsm |

Current compatibility baseline:

| Item | Version |
|---|---|
| JDK | 17 |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.10.1 |
| KSP | 2.0.21-1.0.28 |
| compileSdk / targetSdk | 36 |
| minSdk | 23 |

`consumer-smoke` proves that a separate Android Gradle build can resolve and
compile against the Maven Local artifacts, including the first-use Draft
quickstart machine and ViewModel. It also runs focused Draft quickstart
transition tests for validation, command emission, and save failure recovery,
plus Draft ViewModel tests for command execution and explicit initial state
from `SavedStateHandle` using a reusable main dispatcher rule. The command
failure fixture verifies types-only diagnostics from the published runtime and
does not retain raw Draft values. A separate upload fixture uses the published
invocation DSL, runtime cancellation, and test helper. The consumer build does
not prove broader sample behavior, remote-work cancellation, or binary
compatibility by itself; those remain covered by module tests, sample tests,
graph generation, and `apiCheck` in the local release gate.

The 2026-07-11 ProductEditor upload-boundary repository checks and follow-up
device journey pass. Keeping emulator start and `android run` in one persistent
shell allowed official installation/activation; current captures show the
visible `Cancel upload` state and return to the retained editable draft. This
is sample-device evidence only. Real transport cancellation and human/pilot
evidence remain release-readiness gaps.

Before starting a pilot, record:

- Product/engineering owner.
- Target app, feature module, and screen flow.
- Success criteria, such as reduced ViewModel transition logic, clearer review
  graph, or fewer missed invalid transitions.
- Stop criteria, such as excessive DSL ceremony, missing restoration support,
  runtime pressure issues, or graph generation friction.
- Upgrade command: `./scripts/verify-release-local.sh --warning-mode all` in
  this repository plus the pilot app's affected test suite.

Before counting repository readability as user evidence, run the no-coaching
[Checkout first-use participant task](checkout-first-use-participant-task.md)
with the facilitator rubric in
[the Checkout first-use comprehension protocol](../wiki/06-project/checkout-first-use-comprehension-protocol.md).
Preserve timing,
answers, interventions, and confidence as immutable raw evidence. An AI review
or green sample test is not a substitute for this session.

## Current Pre-Release Artifacts

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
testImplementation("junit:junit:4.13.2")
```

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}
```

The graph plugin adds `io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT` to the
app module by default. That default is generated from the plugin's Afsm version
at build time.

`io.github.afsm` is a temporary pre-release group id for local evaluation.

## Maven Local Metadata Audit

Current generated POMs contain:

| Module | Packaging | Internal dependencies resolve to coordinates | Name/description |
|---|---:|---:|---:|
| `afsm-core` | `jar` | None | Yes |
| `afsm-runtime` | `jar` | `io.github.afsm:afsm-core:0.1.0-SNAPSHOT` | Yes |
| `afsm-test` | `jar` | `io.github.afsm:afsm-core:0.1.0-SNAPSHOT` | Yes |
| `afsm-viewmodel` | `aar` | `io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT` | Yes |
| `afsm-compose` | `aar` | AndroidX Compose/Lifecycle dependencies | Yes |
| `afsm-graph-ksp` | `jar` | None | Yes |
| `afsm-graph-gradle-plugin` | Gradle plugin marker + `jar` | Gradle API | Yes |

Current generated POMs do not yet contain:

- Project URL.
- License metadata.
- SCM metadata.
- Developer or organization metadata.

Those fields should be added only after final product ownership decisions are made.

## Before Public Remote Publishing

Required product decisions:

- Final group id and artifact ids.
- License.
- Repository URL and SCM metadata.
- Developer/organization metadata.
- Maven Central or alternative repository target.
- Signing and release credential ownership.

Required engineering gates:

- Add remote publication metadata after final coordinates, license, SCM, and signing are decided.
- Keep `consumer-smoke` green after every publication metadata change.
- Keep `./scripts/verify-release-local.sh` authoritative while hosted CI remains
  disabled. If hosted CI is deliberately restored, make it call the same local
  gate instead of maintaining a second verification sequence.
- Keep `CONTRIBUTING.md` aligned with the current release gate and test policy.

## Known Warning

`./gradlew publishToMavenLocal --warning-mode all` currently reports:

```text
ProjectDependency.getDependencyProject() method has been deprecated.
```

Observed source:

```text
org.jetbrains.kotlin.gradle.plugin.mpp.PomDependenciesRewriter
```

Current assessment:

- Afsm build scripts do not call this deprecated API directly.
- The warning occurs while generating the `afsm-runtime` Maven POM from a `project(":afsm-core")` API dependency.
- The generated Maven Local artifacts are still consumable by `consumer-smoke`.

Release policy:

- Do not hide this by removing project dependencies or weakening publication metadata.
- Track it as a Kotlin/Gradle plugin compatibility issue.
- Re-check after the Kotlin Gradle plugin or Gradle version is upgraded.
