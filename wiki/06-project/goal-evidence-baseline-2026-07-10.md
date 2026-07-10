---
title: Afsm Goal Evidence Baseline 2026-07-10
updated: 2026-07-10
type: maintenance
---

# Afsm Goal Evidence Baseline 2026-07-10

This baseline audits the current repository against
[[long-term-goal|Afsm Long-Term Goal]] after commit `4a8f2cf`. It distinguishes
implemented behavior, supporting evidence, and product outcomes that remain
unproven.

## Evidence Scale

- **Strong**: current executable tests or external-consumer verification cover
  the stated behavior.
- **Partial**: repository fixtures and AI reviews support the claim, but scope or
  user evidence is incomplete.
- **Missing**: no current authoritative evidence proves the outcome.

## Outcome Matrix

| Long-term outcome | Current evidence | Status | Largest gap |
|---|---|---|---|
| Readable complex flows | Phase-oriented machines, generated MMD, transition tests, repeated internal first-use reviews | Partial | No measured fresh-developer comprehension baseline; important ignore/invalid behavior is split across machine, graph modes, and tests |
| Useful authoring experience | Draft getting-started path and external `consumer-smoke` compile/run verification | Partial | Compilation does not prove authoring usability; the first machine still exposes generic/typealias/delegation ceremony before product flow |
| Safer flow behavior | Runtime and machine tests cover invalid transitions, queue pressure, stale results, command failure, no-replay effects, and terminal behavior | Strong for tested policies | Process recreation and explicit cancellation remain guidance-heavy; diagnostics are failure-focused rather than full transition observability |
| Natural Android integration | `afsm-viewmodel`, `afsm-compose`, sample ViewModels, Draft ViewModel consumer tests, `SavedStateHandle` initial-state fixture | Partial to strong | No real application pilot; restoration coverage is a small input-state example rather than a representative interrupted flow |
| Verification and visualization coherence | Executable topology, KSP registry, graph plugin functional tests, generated MMD checks, API checks, local release gate | Strong for build drift | Flow graphs intentionally omit some no-op/invalid behavior; public example parity is sampled rather than a complete semantic contract |
| Real usability evidence | Multiple structured AI review rounds and repository samples | Missing for real users | No human first-use observation and no production-like external pilot evidence |
| Evidence-based API freeze | Explicit API mode, API dumps, changelog, consumer build | Not yet applicable | These tools detect change but must not preserve the current unproven API; stabilization criteria have not been met |
| Durable product knowledge | `raw/`, canonical wiki, decision/implementation logs, open questions, public docs, current lint workflow | Partial to strong | Historical terminology still exists by design; every redesign cycle must keep canonical/current pages ahead of logs |

## Current First-Use Cost Signals

These are code-shape signals, not proof of user difficulty:

- The Draft quickstart introduces six domain concepts before hosting:
  `Phase`, `Data`, `State`, `Event`, `Command`, and `Effect`/`AfsmNoEffect`.
- Before the first `afsmMachine { ... }` body, the executable Draft fixture uses
  `DraftState`, `DraftMachine`, object delegation, and a separate factory
  function.
- `DraftMachine` expands to
  `AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>` while the DSL
  builder itself has five generic dimensions: phase, data, event, command, and
  effect.
- Auth repeats the same feature-boundary alias/delegation/factory pattern.
- The current source sizes are useful only as rough complexity signals:
  Draft machine plus ViewModel fixture is 148 lines; Auth contract + machine +
  ViewModel is 352 lines; Checkout contract + machine + ViewModel is 474 lines.
- `consumer-smoke` proves the Draft path compiles, tests, hosts, publishes, and
  generates a graph. It does not prove that a first-time developer would choose
  or author this shape naturally.

## Prior Review Evidence

The 2026-05-19 usability loop already reported that the minimal Draft exposed
heavy generics too early. Later rounds improved onboarding order, terminology,
terminal-phase syntax, read-only scopes, graph automation, and Android-visible
types, but they did not directly remove the feature-boundary generic/typealias/
delegation ceremony.

This makes first-use authoring cost a repeated evidence-backed concern rather
than a new aesthetic preference.

## First Redesign Cycle

### Problem

The current API may require developers to understand and repeat framework type
plumbing before they can read or write the product flow.

### Hypothesis

Afsm can reduce or relocate feature-boundary generic/typealias/delegation
ceremony without hiding `State`, `Event`, host work, UI output, or graphable
flow behavior.

### Required Comparison

Compare at least three shapes against Draft, Auth, and Checkout:

1. Current API baseline.
2. A minimally breaking inference/factory redesign.
3. A more radical contract or machine-declaration redesign unconstrained by the
   current public surface.

Evaluate each shape for:

- flow readability in the machine body,
- concepts required before the first transition,
- Kotlin type inference and compile diagnostics,
- event/command/effect visibility,
- ViewModel hosting clarity,
- test readability,
- graph discoverability,
- escape-hatch complexity,
- migration cost only as implementation work, not as a compatibility veto.

### Proof Before Acceptance

- Compile realistic Draft, Auth, and Checkout prototypes.
- Keep behavior tests equivalent or stronger.
- Record exact boilerplate removed and any new hidden behavior or ambiguity.
- Run a fresh-use comprehension review after prototypes exist.
- Accept a redesign only if readability or authoring usefulness improves without
  weakening flow safety or Android integration.

## External Evidence Boundary

Real first-use and pilot evidence cannot be manufactured from repository tests.
Codex can prepare prototypes, comparison material, tasks, and observation
rubrics autonomously, but a human participant or external pilot remains a
separate evidence source.
