# Afsm Goal Completion Audit Evidence

Date: 2026-07-11

Audited repository commit: `dd54490`

## Current Repository Gate

```bash
./scripts/verify-release-local.sh --no-daemon
```

Result: passed.

The script ran graph Gradle plugin tests, core/runtime/test/ViewModel tests,
sample compile and unit tests, graph generation, all API checks, Maven Local
publication, and the clean external consumer build. The existing SDK XML and
Gradle deprecation warnings remained non-blocking.

## Safety Requirement Mapping

The current tests directly cover:

- invalid transitions: `AfsmHostTest` Record/Throw policies and Auth invalid
  command-result behavior,
- expected duplicates: Checkout in-flight pay/retry ignore behavior,
- stale async results and retry identity: Checkout request-id tests,
- cooperative cancellation and lifecycle closure: phase-owned invocation
  runtime tests and ProductEditor ViewModel tests,
- process recreation and durable completion: Checkout `SavedStateHandle`
  restoration tests,
- effect delivery loss: no replay for late collectors plus restored durable
  completion without effect replay,
- command failure: Record/Throw runtime tests and external consumer fixture,
- event/command pressure: command queue and command-result event overflow
  tests,
- results after lifecycle end: command-result drop after host close and active
  invocation cancellation,
- diagnostic privacy: credential-like types-only default tests and explicit
  value opt-in.

The 2026-07-11 ProductEditor device journey separately proves the visible demo
cancel action is installable, reachable, tappable, and returns to the retained
draft.

## Missing Completion Evidence

Repository search found no completed `raw/pilots/` directory and no human
Checkout participant result. The only first-use protocol evidence is the
facilitator dry run, which explicitly produced no answers or scores.

Therefore current evidence does not prove:

- no-prior-Afsm human comprehension or authoring behavior,
- comparison against a practical ViewModel baseline in a feature outside Afsm
  fixtures,
- real team review and adoption friction,
- real transport/SDK cancellation behavior,
- measured rollback in a pilot application,
- that the current API is worth freezing.

## Conclusion

The repository, consumer, and emulator evidence pass, but the long-term product
Goal is not complete. A real Android developer session and production-like
feature pilot remain required. No sample, AI review, or additional speculative
API work can substitute for those results.
