---
title: Afsm Production-Like Pilot Protocol
type: planning
created: 2026-07-11
updated: 2026-07-17
status: ready-needs-target
---

# Afsm Production-Like Pilot Protocol

## Purpose

Determine whether Afsm makes one real complex Android feature easier to read,
author, review, test, and operate than its practical pre-Afsm implementation.

This protocol makes the next evidence step executable. It is not pilot evidence
by itself. A valid result requires a feature outside Afsm's Draft, Auth,
Checkout, ProductEditor, and consumer fixtures, plus an actual feature owner and
reviewer evidence.

## Entry Gate

Start only when all of these are true:

- The feature has meaningful phases, branching, async results, failure or
  retry, and at least one safety concern such as stale results, cancellation,
  restoration, or durable completion.
- An ordinary `ViewModel + StateFlow` implementation or design is available as
  the practical baseline. Do not construct an intentionally poor comparator.
- A feature owner, implementing developer, reviewer, and rollback owner are
  named. One person may hold multiple roles, but reviewer authorship must be
  recorded.
- The Afsm experiment can be isolated to one feature/module/branch and removed
  without changing persisted product data or unrelated application structure.
- The owning team permits the required evidence to be retained. Proprietary
  code does not need to enter this repository; use anonymous structured
  summaries and stable permitted links when necessary.

Reject simple loading/content/error or data-display screens at this gate. A
pilot is not useful if Afsm is obviously the wrong abstraction.

## Pre-Registration

Record this before Afsm implementation starts:

| Field | Required value |
|---|---|
| Pilot id | Anonymous stable id |
| Repository and baseline commit | Permitted URL/path plus immutable commit |
| Feature/module | Exact isolated boundary |
| Business flow | Main success, failures, retries, completion, restoration |
| Roles | Feature owner, author, reviewer, rollback owner |
| Timebox | Start, review date, and stop date |
| Primary comparison | The one outcome that decides keep/reject |
| Secondary comparisons | Readability, authoring, safety, Android friction, graph, diagnostics |
| Success thresholds | Numeric or observable gates fixed before implementation |
| Stop criteria | Conditions that terminate adoption without rescue work |
| Rollback path | Baseline ref/commit and verification commands |
| Evidence location | Local raw directory or permitted stable external store |
| AI use | Tools, prompts, and which artifacts they influenced |

Do not change success thresholds after seeing results. If requirements or the
Afsm API must change during the pilot, record the deviation and restart the
affected comparison instead of silently moving the baseline.

## Fixed Comparison Contract

Both implementations must use the same:

- user-visible requirements and acceptance journey,
- repository/use-case and backend contract,
- navigation and persisted-data semantics,
- relevant failure, retry, duplicate, stale-result, and restoration scenarios,
- build variant and test environment.

Freeze the baseline first. Compare Afsm against the real maintained baseline,
not against a synthetic reducer created only to lose the comparison. Line count
alone is never a success metric.

## Baseline Capture

Before changing the feature, record:

- files and code locations that define flow transitions,
- business-state mutation sites,
- branches that decide allowed/ignored/invalid input,
- async launch, cancellation, result, and stale-result ownership,
- failure, retry, completion, and restoration handling,
- tests that directly prove each important scenario,
- diagnostic path for an invalid transition or failed command,
- commands and elapsed time for affected verification,
- reviewer answers and files opened for the comparison task below.

Keep raw counts with definitions. For example, a mutation site is one distinct
location that changes business-flow state, not every `copy()` call inside the
same transition.

## Afsm Capture

Record the same measurements after implementation, plus:

- machine, graph, transition-test, ViewModel, and UI adapter locations,
- Afsm setup and authoring time separated from feature behavior time,
- DSL terms or ordering that caused hesitation,
- graph metadata written only for tooling,
- runtime diagnostic usefulness and privacy behavior,
- Android integration work for `ViewModel`, `SavedStateHandle`, DI/factories,
  lifecycle collection, and coroutine tests,
- any behavior that remains easier to understand in the baseline.

## Reviewer Comparison Task

Ask the same questions for both implementations without coaching:

1. What are the possible business states or phases?
2. Which events are accepted in each important state, and what is invalid?
3. What starts external work, who owns its lifetime, and how does its result
   return?
4. What happens on failure and retry?
5. How are duplicate and stale results handled?
6. What represents durable completion versus a one-shot UI action?
7. What is restored after process recreation, and what work is not restarted?
8. Which files were necessary for the overview, exact rules, and executable
   proof?
9. What behavior is still ambiguous?
10. Which representation would you prefer to review and maintain, and why?

Record time, answer accuracy, critical misconceptions, file hops, questions,
and confidence. Prefer two reviewers with counterbalanced order. If only one is
available, record the order and learning bias rather than presenting the times
as a controlled comparison.

## Safety Matrix

For every row, record `proved`, `not proved`, or `not applicable` with a reason
and link to the executable test or runtime evidence:

| Safety concern | Baseline | Afsm | Evidence |
|---|---|---|---|
| Invalid transition | | | |
| Expected duplicate event | | | |
| Stale async result | | | |
| Retry request identity | | | |
| Command or phase-owned cancellation | | | |
| Process recreation | | | |
| Durable completion | | | |
| Durable outcome or one-shot UI action delivery loss | | | |
| Command failure | | | |
| Event/command pressure | | | |
| Result after lifecycle end | | | |

Do not add irrelevant mechanisms merely to fill the table. A justified
`not applicable` is better evidence than artificial complexity.

## Required Verification

At minimum, preserve:

- the same acceptance journey on baseline and Afsm branches,
- focused transition and ViewModel/integration tests,
- the pilot app's affected broader suite,
- generated graph and a graph/runtime drift check,
- Afsm repository release gate for the consumed commit,
- one diagnostic exercise for a realistic invalid transition or command
  failure,
- one rollback drill followed by the baseline verification commands.

## Rollback Drill

Rollback is measured, not promised:

1. Keep the baseline ref immutable.
2. Isolate Afsm dependency, graph plugin, machine, and adapter changes in
   reviewable commits.
3. Restore the baseline feature without retaining compatibility aliases solely
   for Afsm.
4. Remove unused Afsm dependencies/generated wiring.
5. Run the baseline tests and acceptance journey.
6. Record elapsed time, files changed, residual coupling, and any data or build
   migration that prevents clean rollback.

If rollback cannot be demonstrated within the pre-registered timebox, the
pilot cannot pass its adoption gate.

## Stop Criteria

Stop and preserve the result when any pre-registered condition occurs,
including:

- the feature proves too simple for Afsm,
- requirements change enough to invalidate the baseline,
- the team cannot isolate adoption or rollback,
- DSL/runtime behavior remains less predictable after normal documentation,
- Android lifecycle, DI, restoration, or build integration cost exceeds the
  accepted limit,
- graph/test/runtime drift appears,
- a safety regression or remote-cancellation false promise remains unresolved,
- owner/reviewer time or evidence permission is withdrawn.

Do not hide a stopped pilot or turn it into a new sample success story. A
rejection is valid product evidence.

## Outcome Classification

Classify the result only after raw evidence exists:

- `keep`: pre-registered primary threshold passes, safety does not regress,
  Android integration is acceptable, and rollback succeeds.
- `revise`: the feature shows value but a repeatable Afsm/API/documentation
  problem blocks adoption; open one bounded redesign experiment.
- `reject-for-feature`: the baseline remains clearer or cheaper for this flow.
- `invalid-pilot`: comparator, requirements, roles, or evidence changed enough
  that no conclusion is defensible.

One successful pilot does not freeze the API or prove broad market preference.
Repeated friction across independent evidence is stronger than a single
preference score.

## Raw Evidence Layout

Create a new immutable directory only when a real pilot begins:

```text
raw/pilots/YYYY-MM-DD-<pilot-id>/
  README.md
  pre-registration.md
  baseline.md
  afsm-implementation.md
  reviewer-comparison.md
  safety-matrix.md
  verification.md
  rollback.md
  decision.md
```

`README.md` must identify the repository commits, anonymized roles, evidence
permissions, and whether any artifact is stored externally. Do not create a
filled result from repository fixtures or AI review.

## Current Blocker

The protocol is ready. A target feature outside this repository's fixtures,
its owner, reviewer, rollback owner, baseline commit, and evidence permission
are not yet selected. That external coordination is required before a valid
pilot result can exist.
