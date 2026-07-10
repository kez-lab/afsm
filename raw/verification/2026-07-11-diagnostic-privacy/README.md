# Diagnostic Privacy Verification

Date: 2026-07-11

Specification commit: `b7e2538`

Implementation commit: `0837c28`

## Red Evidence

`AfsmDiagnosticPrivacyTest` was added before production changes. The focused
runtime test failed compilation because diagnostic codes, decision categories,
type fields, value policy, and grouped values did not exist.

The fixtures intentionally used:

- password `secret-password`,
- email `ada@example.com`,
- exception detail `token=private-token`.

## Implemented Contract

- `AfsmDiagnosticDataPolicy.TypesOnly` is the default.
- Safe top-level diagnostics expose code, decision category, fixed message,
  simple Kotlin type names, and library-owned metadata.
- Raw state/event/command/reason/throwable values are absent by default.
- `AfsmDiagnosticDataPolicy.IncludeValues` explicitly creates grouped
  `AfsmDiagnosticValues`.
- `AfsmDiagnostic.toString()` and all safe top-level fields are verified not to
  contain the credential-like fixture values.
- Old raw top-level getters and the public diagnostic constructor were removed.

## Passing Verification

```bash
./gradlew :afsm-runtime:test --no-daemon
./gradlew :afsm-runtime:apiDump --no-daemon
./gradlew :afsm-runtime:check :afsm-runtime:apiCheck --no-daemon
./scripts/verify-release-local.sh --no-daemon
```

Result: passed, including API checks, Maven Local publication, and the clean
external Draft consumer privacy assertion.

The API dump contains raw getters only on the explicitly gated
`AfsmDiagnosticValues` type; `AfsmDiagnostic` itself exposes no raw value,
reason, or throwable getter.

## Evidence Boundary

This proves Afsm-owned diagnostics are types-only by default. It cannot make an
application logger safe if the application explicitly selects `IncludeValues`,
logs arbitrary application exceptions, or combines type metadata with its own
sensitive attributes.
