# Afsm Consumer Smoke

This is a separate Android Gradle build that verifies Afsm can be consumed from Maven Local without depending on repository project modules.

Run from the repository root:

```bash
./scripts/verify-consumer-smoke.sh
```

The smoke build covers:

- `io.github.afsm:afsm-core`
- `io.github.afsm:afsm-runtime`
- `io.github.afsm:afsm-viewmodel`
- `io.github.afsm:afsm-graph-ksp`

It intentionally stays small. The goal is dependency and API consumption verification, not sample app behavior coverage.
