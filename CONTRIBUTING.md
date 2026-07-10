# Contributing to Afsm

Afsm is being built as a small, readable Android/Kotlin FSM library. Contributions should protect API clarity, runtime predictability, and test trust.

## Engineering Principles

- Prefer plain Kotlin in `afsm-core` and `afsm-runtime`.
- Keep Android dependencies isolated to `afsm-viewmodel` and sample apps.
- Keep `ViewModel` as the Android lifecycle adapter, not the place where transition rules accumulate.
- Model async work as commands emitted by the machine and executed by the host.
- Add UI effects only when durable state cannot express the behavior.
- Avoid adding abstractions until they reduce real caller complexity.

## Development Flow

1. Start from the desired behavior or public API.
2. Add or update tests that describe that behavior.
3. Implement the smallest code change that satisfies the tests.
4. Run the relevant module tests.
5. Run the local release gate before release-facing changes.
6. Update docs, API dumps, changelog, and wiki pages when the change is durable.

## Test Integrity

Do not weaken tests just to make implementation pass.

Allowed test changes:

- Add a missing scenario.
- Rename a test for clarity while preserving the asserted behavior.
- Update expectations because a documented spec or decision changed.
- Replace brittle implementation-detail assertions with stronger behavior assertions.

Not allowed:

- Deleting a failing test without replacing its coverage.
- Relaxing an assertion because the implementation is inconvenient.
- Updating golden files or API dumps without reviewing the public impact.
- Treating generated artifacts as proof when the runtime behavior is untested.

When behavior changes intentionally, update the spec or decision log first, then update tests.

## Public API Changes

Afsm uses explicit API mode and binary API validation.

For public API changes:

- Keep names generic and Kotlin/Android-friendly.
- Update `docs/afsm-public-api.md` when the public surface changes.
- Run `./gradlew apiDump` only after reviewing the ABI diff.
- Commit API dump changes with the implementation that requires them.
- Add a `CHANGELOG.md` entry for release-facing changes.

## Verification

Local release gate:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

Use narrower commands while developing, but run the full gate before release-facing commits.

Hosted GitHub Actions CI is disabled for cost control. Run the relevant local
checks before merge and run the full local release gate for release-facing
changes. If hosted CI is deliberately restored, it should call this same script
instead of duplicating the gate.

## Documentation

Update durable docs when the change affects how users understand or operate Afsm:

- `README.md` for user-facing overview.
- `docs/afsm-public-api.md` for API surface.
- `docs/release-readiness.md` for release gates and blockers.
- `CHANGELOG.md` for release notes.
- `wiki/` for evolving architecture and implementation decisions.
