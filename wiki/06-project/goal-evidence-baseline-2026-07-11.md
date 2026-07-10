---
title: Afsm Goal Evidence Baseline 2026-07-11
updated: 2026-07-11
type: maintenance
---

# Afsm Goal Evidence Baseline 2026-07-11

This audit supersedes the 2026-07-10 starting baseline for current planning. It
compares the repository after Checkout restoration commit `dff5f28` with
[[long-term-goal|Afsm Long-Term Goal]]. Historical evidence remains in the
earlier baseline.

## Evidence Scale

- **Strong**: current executable tests, generated artifacts, or external
  consumer verification cover the claim.
- **Partial**: repository evidence is coherent but human, device, or real-pilot
  scope is missing.
- **Missing**: no authoritative evidence proves the outcome.

## Outcome Matrix

| Long-term outcome | Current evidence | Status | Largest remaining gap |
|---|---|---|---|
| Readable complex flows | Direct machine properties, generated graphs, business-language tests, Checkout three-artifact review, explicit restoration phase | Strong in repository; partial for people | No unaided human comprehension result; restoration-only orphan state still needs human interpretation evidence |
| Useful authoring experience | Draft-first docs, external consumer, removed wrapper/factory ceremony, lower-camel machine values, static/dynamic machine split | Partial to strong | Compilation and AI review do not prove first-time authoring time or preference |
| Safer flow behavior | Invalid/ignored decisions, stale ids, queue bounds, command failure, lifecycle result drop, durable completion, restored unknown payment | Strong for covered behavior | `AfsmDiagnostic` exposes raw state/event/command/throwable objects; real command cancellation remains guidance-heavy |
| Natural Android integration | `ViewModel`, StateFlow, Compose collection, real repository wiring, SavedStateHandle Checkout restoration, coroutine tests, APK assemble | Strong in repository; partial on device | Android CLI could not discover the booted emulator; no real app pilot |
| Verification and visualization coherence | Executable topology, KSP/Gradle tests, graph registry, API dumps, Maven Local consumer, full release gate | Strong | Restoration source is represented by an orphan state rather than a typed graph entry source; human usefulness unmeasured |
| Real usability evidence | Fixed participant task, facilitator rubric, two preparation checks, repeated AI reviews | Missing for real users | No human session and no production-like external pilot |
| Evidence-based API freeze | Pre-release design freedom remains explicit; API/ABI tooling detects intentional changes | Not applicable yet | Human and pilot evidence must justify the surface before freeze |
| Durable product knowledge | Raw evidence, canonical/current Wiki, decision/implementation logs, open questions, public docs, repeated lint/sync | Strong | Each redesign must continue updating current pages ahead of historical logs |

## Largest Autonomous Gap

`AfsmDiagnostic` currently exposes these raw values publicly:

- `state: Any`,
- `event: Any`,
- `command: Any?`,
- `throwable: Throwable?`,
- dynamic `reason` and a decision object that can also carry a reason.

This contradicts the long-term requirement that diagnostics help locate flow
errors without exposing sensitive data by default. Real sample values include
Auth email/password fields and Draft titles. `AfsmLogger.None` as the default
logger does not solve the issue: the moment a team selects a Record policy and
logger, raw domain values enter the logging boundary automatically.

## Next Cycle

Redesign diagnostics around safe codes, decision categories, and type names.
Raw domain values and throwable details may exist only behind an explicit
privacy-risk opt-in. Verify both the default redaction and the opt-in path with
password-like realistic data, runtime tests, API dumps, docs, and the external
consumer.

## Cycle Result

Completed in specification commit `b7e2538` and implementation commit
`0837c28`:

- `TypesOnly` is now the runtime default,
- safe code/decision/type/metadata fields replace raw top-level values,
- `IncludeValues` is the only grouped raw-value path,
- credential-like runtime tests, API validation, Maven Local publication, and
  the external Draft consumer pass,
- a repository fresh-use review accepts the design provisionally while keeping
  human interpretation and production logger review explicitly unproven.

The next largest autonomously testable runtime gap is command cancellation.
The repository currently documents explicit cancel commands and request ids,
but has not yet compared that approach with phase-owned invoked-service
semantics using a realistic interruptible Android flow.

## Invocation Cycle Result

Completed in specification commit `8146e65` and implementation commit
`b0fd60a`:

- the ineffective queued cancel-command guidance was removed as a false
  runtime promise,
- bounded `onEnter { invoke(...) }` work is automatically cancelled on phase
  exit and host closure,
- ProductEditor exposes a visible cancel-upload edge without ViewModel job
  plumbing,
- machine, graph, runtime, ViewModel, API, Maven Local, and clean external
  consumer verification pass,
- the fresh-use review accepts the result provisionally while keeping explicit
  key ceremony and real network/SDK cancellation unproven.

After this cycle, the binding Goal gaps are human no-coaching comprehension and
a production-like pilot. The next smaller API questions are whether an explicit
invocation key earns its authoring cost and whether active invocation capacity
needs a first-class bound. Neither should be changed without realistic authoring
or workload evidence.

## External Evidence Boundary

These repository cycles can improve default safety and executable evidence.
They cannot replace the human first-use session or production-like pilot still
required for Goal completion.
