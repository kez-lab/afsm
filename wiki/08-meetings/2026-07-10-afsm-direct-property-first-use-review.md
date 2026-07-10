---
title: Afsm Direct Property First-Use Review
updated: 2026-07-10
type: ai-fresh-use-review
---

# Afsm Direct Property First-Use Review

This review evaluates Candidate E after implementation. It is a structured
repository-based fresh-use review, not evidence from a human Android developer.

## Review Path

Read only the material a new adopter should need:

1. README quick start and `docs/getting-started.md`.
2. Draft contract, machine, transition tests, and ViewModel.
3. Auth machine, tests, and graph.
4. Checkout machine, ViewModel, tests, and graph.
5. Graph setup and generated-registry contract.

The review asked whether the flow could be located and traced without knowing
the KSP processor, runtime implementation, or historical API drafts.

## Verdict

Candidate E is preferable to the old alias + delegated object + factory shape
for the current pre-release product. The declaration and executable flow are
contiguous, the ViewModel still receives an ordinary `AfsmMachine`, and graph
identity now belongs to the real machine value.

This is not enough to freeze the API. Human first-use and production-like pilot
evidence are still missing.

## Findings

| Priority | Finding | Impact | Decision |
|---|---|---|---|
| High | Checkout's stable machine owns `CheckoutData(productId = 0)` even though the real product id is a navigation input | Calling the convenient `afsmHost(machine)` overload by mistake can start a valid-looking but wrong flow | Make dynamic initial-state safety the next API experiment |
| Medium | Direct machine properties use PascalCase names such as `DraftStateMachine` | A `val` reads like a type or object and hides that Candidate E is ordinary value composition | Rename machine properties to lower camel case |
| Medium | The four-channel `AfsmMachine<State, Event, Command, Effect>` type remains visible | It is long, but each channel is behaviorally meaningful and gives good expected-type inference | Keep it until real authoring evidence justifies a smaller contract |
| Low | Graph properties require explicit stable visibility/backing-field rules | The rules are additional setup knowledge | Keep the rules in the graph guide; focused KSP diagnostics are adequate |

## What Improved

- The first phase follows one declaration instead of an alias, wrapper, and
  factory indirection.
- Graph tooling references the executable machine directly.
- ViewModel and transition test call sites did not gain wrapper types or token
  concepts.
- Auth and Checkout flow bodies remain unchanged, so the authoring cleanup did
  not hide commands, effects, invalid results, or stale-result policy.

## Next Actions

1. Rename top-level machine values to Kotlin lower camel case and rerun the
   complete graph/sample/consumer gate.
2. Specify alternatives that let a graphable dynamic feature avoid a fake
   default data value and make omission of the host initial state impossible or
   explicit.
3. Run a human first-use observation separately; do not count this AI review as
   that evidence.

## Implementation Follow-Up

The high-priority dynamic initialization finding is resolved in the current
pre-release API: `AfsmMachine` no longer promises a default state,
`AfsmDefaultMachine` adds one only for static flows, and Checkout declares
`initialPhase = Idle` without `productId = 0`. Omitting Checkout-like runtime
state now fails at the ViewModel host call during compilation.
