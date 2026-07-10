# Checkout Product-Goal Fit Follow-Up

Date: 2026-07-10

Repository baseline: `aa7bf3b` (`test: cover Checkout graph-invisible policies`)

## Added Evidence

The Checkout transition test now names and verifies the four policies missing
from the initial constrained review:

- pay/retry before product load are handled in `Idle` with a prerequisite
  error,
- `ProductUnavailable` rejects payment as invalid,
- duplicate pay/retry while payment is in flight are ignored,
- late payment success/failure after completion are ignored.

## Verification

```bash
./gradlew :sample-shop:testDebugUnitTest \
  --tests 'afsm.sample.shop.feature.checkout.CheckoutStateMachineTest' \
  :sample-shop:generateAfsmMmd \
  --no-daemon
```

Result: `BUILD SUCCESSFUL`.

The generated Mermaid graph did not change. A repeat constrained read of the
machine, graph, and tests can now recover the macro flow and the important
graph-invisible handled/ignored/invalid policies.

## Evidence Boundary

This upgrades the repository-based product-goal verdict to pass. It still does
not measure a real Android developer's first-use preference, comprehension
time, or adoption cost.
