# Checkout First-Use Participant Task

This is a product-comprehension exercise, not a Kotlin quiz. There is no
penalty for marking something unclear. The unclear parts are the most useful
result.

## Before You Start

- You should be comfortable with Android `ViewModel`, Kotlin sealed types, and
  coroutine-based repository work.
- Prior Afsm experience is not expected.
- Record the start time.
- During the timed part, open only the three files listed below.
- Use your normal editor navigation and search. If you use AI assistance, note
  the tool and prompts so the result can be classified separately.
- Do not read Afsm documentation or ask the facilitator to explain an Afsm term
  until the timed part ends.

## Files

1. `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachine.kt`
2. `sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd`
3. `sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt`

## Task

Without running the app, explain the Checkout business flow in your own words.
Answer these questions and record the end time:

1. What is the starting phase? Does the machine provide a real product id, or
   must its caller provide one with runtime state?
2. Draw or list the main success path from opening Checkout to completion.
3. What external work starts during product loading and payment? What causes it
   to start?
4. What happens when the product cannot be loaded?
5. What happens when payment fails and the user retries?
6. How does the flow prevent an old payment result from changing newer state?
7. Give one example each of `Handled`, `Ignored`, and `Invalid` behavior. State
   why the three decisions are different.
8. Why does `PaymentStatusUnknown` have no normal incoming graph edge, and what
   automatic work or user action is intentionally unavailable there?
9. Which value represents durable completion? Can you find any separate
   one-shot completion output in these files?
10. Which artifact was most useful for the overview, exact rule details, and
   executable proof?
11. List anything you still cannot determine from these three files.

## After the Timed Part

Rate each statement from 1 (strongly disagree) to 5 (strongly agree):

- I could find the main flow quickly.
- I could predict failure, retry, duplicate, and stale-result behavior.
- Commands were distinguishable from state changes.
- The machine, graph, and tests agreed with one another.
- I would prefer this representation over tracing the same rules across a
  complex ViewModel and its callbacks.

Finally record:

- the first Afsm term or syntax that made you hesitate,
- the first place you wanted to open another file,
- one change that would make the flow easier to explain in code review.
