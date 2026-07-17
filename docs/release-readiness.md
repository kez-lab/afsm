# Afsm Release Readiness

Afsm is an internal beta and has not been publicly released.

## Local release gate

```bash
./scripts/verify-release-local.sh --no-daemon
```

This gate verifies:

- core, runtime, test, ViewModel, KSP, and graph-plugin tests,
- sample compilation, feature tests, and `.mmd` generation,
- binary API dumps,
- Maven Local publication,
- a clean separate Android build consuming published artifacts,
- Draft quickstart transition and ViewModel wiring behavior,
- command failure diagnostics and privacy defaults,
- phase-owned invocation cancellation from an external consumer.

Hosted CI is currently disabled for cost control. The local script is the
authoritative engineering gate.

## Current artifacts

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
```

Graph tooling:

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}
```

The plugin aligns `afsm-graph-ksp` with its own Afsm version.

## Compatibility baseline

| Item | Version |
|---|---|
| JDK | 17 |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.10.1 |
| KSP | 2.0.21-1.0.28 |
| compileSdk / targetSdk | 36 |
| minSdk | 23 |

## What repository verification does not prove

Green builds prove implementation consistency. They do not prove first-use
comprehension, team adoption value, or production pilot outcomes.

The 2026-07-17 relayed human feedback is useful directional evidence but lacks
the participant/task/timing metadata required to count as a controlled
first-use session. The prior constrained AI review also remains AI evidence.

Before claiming product-goal completion, run:

1. a controlled no-coaching
   [Checkout flow-comprehension task](checkout-first-use-participant-task.md)
   followed by the separately timed
   [Android-boundary task](checkout-android-integration-participant-task.md)
   against the commit-pinned current Effect-free revision,
2. the canonical
   [production-like pilot protocol](../wiki/06-project/production-like-pilot-protocol.md)
   with a real Android feature and comparator.

## Internal pilot contract

- Assign an engineering owner.
- Choose a complex flow, not a trivial screen.
- Record baseline comprehension/review evidence.
- Pre-register success and stop criteria.
- Keep Afsm isolated to a feature module for rollback.
- Run the pilot app tests and this repository gate before upgrading.

## Before public publishing

Still required:

- final group and artifact coordinates,
- license, project URL, SCM, and developer metadata,
- signing and repository credentials,
- public release automation,
- evidence-backed API freeze decision.
