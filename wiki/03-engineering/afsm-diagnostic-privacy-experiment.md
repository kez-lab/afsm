---
title: Afsm Diagnostic Privacy Experiment
updated: 2026-07-11
status: candidate-c-implemented
---

# Afsm Diagnostic Privacy Experiment

## Problem

The public `AfsmDiagnostic` contract carries raw state, event, command, reason,
and throwable objects. These objects are convenient in tests but can contain
passwords, email addresses, form text, tokens, payment data, or exception
messages. A logger or crash SDK can stringify them without the feature author
realizing that Afsm crossed a sensitive-data boundary.

Concrete repository examples:

- `AuthForm.password`, `AuthEvent.PasswordChanged`, and
  `AuthCommand.Login/Register` contain credentials,
- `DraftData.title` and `DraftCommand.SaveDraft(title)` contain user text,
- thrown repository exceptions may embed request or account details.

## Invariants

- Default diagnostics contain no raw state, event, command, throwable, or
  application-authored reason.
- Default messages are fixed library text and never interpolate domain values.
- Developers can still identify diagnostic category, decision category, and
  involved Kotlin types.
- Queue capacity and other library-owned safe metadata remain inspectable.
- Access to raw values requires one explicit configuration choice whose name
  communicates the privacy risk.
- Invalid/overflow exceptions and Record-policy logger diagnostics use the same
  envelope.
- Command failure `Throw` may still propagate the original application
  exception; this experiment governs Afsm diagnostic payloads, not arbitrary
  exceptions thrown by application code.

## Candidate A: Keep Raw Values And Document Caution

Verdict: rejected. Documentation cannot prevent a logger or crash SDK from
stringifying the public fields. The unsafe path remains the easiest path.

## Candidate B: Remove Raw Values Entirely

Expose only fixed message, code, decision category, and type names.

Benefit: strongest privacy boundary and smallest diagnostic object.

Cost: teams cannot deliberately inspect full values in local-only debugging or
specialized tests without instrumenting their reducer/handler separately.

Verdict: safe but unnecessarily rigid while the library already offers runtime
configuration.

## Candidate C: Types-Only Default With Explicit Raw Opt-In

Replace the current shape with:

```kotlin
enum class AfsmDiagnosticDataPolicy {
    TypesOnly,
    IncludeValues,
}

enum class AfsmDiagnosticCode {
    InvalidTransition,
    IgnoredTransitionOutputDropped,
    CommandFailure,
    CommandQueueOverflow,
    CommandResultQueueOverflow,
    CommandResultDroppedHostClosed,
}

enum class AfsmDiagnosticDecision {
    Transitioned,
    Handled,
    Ignored,
    Invalid,
}

class AfsmDiagnostic {
    val code: AfsmDiagnosticCode
    val decision: AfsmDiagnosticDecision
    val message: String
    val stateType: String
    val eventType: String
    val commandType: String?
    val failureType: String?
    val metadata: Map<String, String>
    val values: AfsmDiagnosticValues?
}
```

`AfsmDiagnosticValues` groups the raw state/event/command/reason/throwable and
is `null` under the default `TypesOnly` policy. `IncludeValues` is an explicit
privacy-risk escape hatch.

Verdict: selected. Safe inspection remains useful, while raw access is no
longer accidental or spread across top-level diagnostic properties.

## Candidate D: Require A Custom Redactor/Mapper

Make every consumer supply a projection function from raw values to attributes.

Verdict: defer. It provides flexible safe identifiers but adds first-use
ceremony. A future pilot may justify a custom attribute mapper after the safe
default envelope is proven.

## Acceptance Criteria

1. A default invalid-transition diagnostic containing Auth-like password/email
   values exposes only safe code, fixed message, categories, and type names.
2. A default command-failure diagnostic exposes exception type but no exception
   instance or message.
3. Default overflow/lifecycle diagnostics expose library-owned capacity/closed
   metadata without raw domain values.
4. `IncludeValues` restores raw values only after explicit configuration and is
   documented as unsuitable for production logs without redaction.
5. `AfsmDiagnostic.toString()` cannot leak raw values in either policy; raw
   values require explicit `diagnostic.values` access.
6. Runtime tests, consumer smoke, API dumps, docs, and the full local release
   gate pass.
7. No compatibility alias preserves the old raw top-level getters.

## Evidence Boundary

This experiment can prove default payload safety for Afsm-owned diagnostics. It
cannot guarantee that application-authored logger code, exception messages, or
explicit `IncludeValues` use is privacy-safe.

## Implementation Result

Candidate C is implemented:

- default diagnostics expose safe codes, decision categories, fixed messages,
  simple type names, and Afsm-owned metadata,
- raw values are grouped under nullable `AfsmDiagnostic.values`,
- `values` exists only with explicit `IncludeValues`,
- old raw top-level getters and the public constructor were removed,
- password/email/token fixtures prove safe top-level fields and diagnostic
  string output do not contain the raw values,
- runtime/API checks and the full Maven Local external-consumer gate pass.

The result is the current pre-release direction, not a production privacy
certification. A custom safe-attribute mapper remains evidence-dependent.
