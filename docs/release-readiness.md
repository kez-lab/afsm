# Afsm Release Readiness

This checklist defines the current gate for turning Afsm into a publishable Android/Kotlin library.

## Current Local Release Gate

Run:

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest
./gradlew :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd
./gradlew apiCheck
./scripts/verify-consumer-smoke.sh
```

What this proves:

- Core transition and executable machine APIs compile and pass unit tests.
- Runtime dispatch, command execution, effects, invalid transitions, and command failure policies pass unit tests.
- Android ViewModel helper compiles and passes unit tests.
- The sample app compiles and exports `.mmd` graphs from real annotated machines.
- Maven Local publishes all four library modules.
- A separate Android Gradle build consumes the published Maven Local artifacts, including the ViewModel AAR and KSP processor.
- Kotlin explicit API mode is enabled for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.
- Binary API dumps are checked for all four Afsm library modules.

## Current Pre-Release Artifacts

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
```

`io.github.afsm` is a temporary pre-release group id for local evaluation.

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
