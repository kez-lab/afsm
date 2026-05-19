# Afsm Release Readiness

This checklist defines the current gate for turning Afsm into a publishable Android/Kotlin library.

## Current Local Release Gate

Run:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

What this proves:

- Core transition and executable machine APIs compile and pass unit tests.
- Runtime dispatch, command execution, effects, invalid transitions, and command failure policies pass unit tests.
- Android ViewModel helper compiles and passes unit tests.
- Compose effect helper compiles.
- The sample app compiles and exports `.mmd` graphs from real annotated machines.
- Maven Local publishes all five API-tracked library modules plus the Afsm graph Gradle plugin.
- A separate Android Gradle build consumes the published Maven Local artifacts, including the ViewModel AAR, graph Gradle plugin, and KSP processor.
- Kotlin explicit API mode is enabled for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-compose`, and `afsm-graph-ksp`.
- Binary API dumps are checked for the five API-tracked Afsm library modules.

## GitHub CI

The private GitHub repository is:

```text
https://github.com/kez-lab/afsm
```

CI workflow:

```text
.github/workflows/ci.yml
```

The workflow runs on pushes to `main`, pull requests, and manual dispatch. It
uses JDK 17, the Android SDK, Gradle caching, and the same local release gate:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

The CI badge in `README.md` points to this workflow. Because the repository is
private, badge visibility depends on GitHub authentication and repository
access.

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
compile against the Maven Local artifacts. It does not prove sample behavior,
runtime correctness, or binary compatibility by itself; those remain covered by
the module tests, sample tests, graph generation, and `apiCheck` in the local
release gate.

Before starting a pilot, record:

- Product/engineering owner.
- Target app, feature module, and screen flow.
- Success criteria, such as reduced ViewModel transition logic, clearer review
  graph, or fewer missed invalid transitions.
- Stop criteria, such as excessive DSL ceremony, missing restoration support,
  runtime pressure issues, or graph generation friction.
- Upgrade command: `./scripts/verify-release-local.sh --warning-mode all` in
  this repository plus the pilot app's affected test suite.

## Current Pre-Release Artifacts

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
```

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}
```

The graph plugin adds `io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT` to the
app module by default.

`io.github.afsm` is a temporary pre-release group id for local evaluation.

## Maven Local Metadata Audit

Current generated POMs contain:

| Module | Packaging | Internal dependencies resolve to coordinates | Name/description |
|---|---:|---:|---:|
| `afsm-core` | `jar` | None | Yes |
| `afsm-runtime` | `jar` | `io.github.afsm:afsm-core:0.1.0-SNAPSHOT` | Yes |
| `afsm-viewmodel` | `aar` | `io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT` | Yes |
| `afsm-compose` | `aar` | AndroidX Compose/Lifecycle dependencies | Yes |
| `afsm-graph-ksp` | `jar` | None | Yes |
| `afsm-graph-gradle-plugin` | Gradle plugin marker + `jar` | Gradle API / AGP compile-only | Yes |

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
- Keep `.github/workflows/ci.yml` aligned with `./scripts/verify-release-local.sh`.
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
