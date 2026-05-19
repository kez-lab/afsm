# Afsm Consumer Smoke

This is a separate Android Gradle build that verifies Afsm can be consumed from Maven Local without depending on repository project modules.

Run from the repository root:

```bash
./scripts/verify-consumer-smoke.sh
```

The smoke build covers:

- `io.github.afsm:afsm-core`
- `io.github.afsm:afsm-compose`
- `io.github.afsm:afsm-runtime`
- `io.github.afsm:afsm-viewmodel`
- `io.github.afsm:afsm-graph-ksp`
- `io.github.afsm.graph` Gradle plugin

It intentionally stays small. The goal is dependency, plugin, KSP, graph
generation, and API consumption verification, not sample app behavior coverage.

Current compatibility baseline:

- JDK 17
- Kotlin 2.0.21
- Android Gradle Plugin 8.10.1
- KSP 2.0.21-1.0.28
- compileSdk/targetSdk 36
- minSdk 23
