---
title: Afsm Diagnostic Privacy Fresh-Use Review
updated: 2026-07-11
status: repository-pass
---

# Afsm Diagnostic Privacy Fresh-Use Review

## Scope

Review the new diagnostic API as an Android developer who wants actionable
invalid-transition and command-failure logs without leaking domain values.

This is a repository-based AI review, not a human or production privacy audit.

## What Became Clearer

- `AfsmDiagnosticCode` answers what runtime problem occurred without parsing a
  message.
- `AfsmDiagnosticDecision` has no embedded reason or domain value.
- `stateType`, `eventType`, `commandType`, and `failureType` give enough first
  orientation for most failures.
- `metadata` carries only Afsm-owned values such as queue capacity.
- `values == null` makes the safe default directly testable.
- `IncludeValues` names the privacy tradeoff and groups every unsafe field under
  one explicit access point.

## What Became Harder

- Types-only diagnostics cannot distinguish enum instances such as
  `ResultOne` and `ResultThree`; both intentionally report `PressureEvent`.
- Application-authored invalid reasons are unavailable under the safe default.
- The public surface adds four advanced types, although ordinary ViewModels do
  not need to import them.

## Verdict

Accept Candidate C as the current pre-release direction. The default is safer
and still locates the machine boundary involved. The extra types remain behind
advanced logger configuration and do not enter Draft-first onboarding.

Do not add a custom safe-attribute mapper until a real pilot shows that
code/type/metadata context is insufficient. Do not count this AI review as
evidence that production logging is compliant.

## Remaining Evidence

- Human developer interpretation of the new diagnostic fields.
- A production-like logger/crash-tool integration review.
- Whether a safe custom attribute mapper is worth its authoring cost.
