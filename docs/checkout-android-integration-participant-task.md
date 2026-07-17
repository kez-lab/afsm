# Checkout Android Integration Participant Task

Complete this only after you have submitted the timed machine/graph/tests
answers. This is a product-usability exercise, not an architecture quiz. Mark
anything you cannot determine or would implement differently.

## Before You Start

- Record a new start time for this stage.
- Keep the Stage 1 machine, graph, and tests available.
- Open only the five additional files listed below.
- Do not ask the facilitator to explain an Afsm term until your answers and
  ratings are fixed.
- If you use AI assistance, record the product, model, and prompts so the result
  can be classified separately from a no-AI human session.

## Additional Files

1. `README.md`
2. `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutFlow.kt`
3. `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutViewModel.kt`
4. `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutRestoration.kt`
5. `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutScreen.kt`

## Task

Answer in your own words and record the end time:

1. Is a `Command` an external operation itself, or a value requesting work?
   Which code executes `LoadProduct` and `SubmitPayment`?
2. Trace one Pay click from Compose through the ViewModel, machine, payment
   repository result, durable completion state, and route-level UI reaction.
3. Does `CheckoutScreen` construct any `CheckoutEvent` values or call a generic
   `onEvent(Event)` method? What API does the UI call instead?
4. Which responsibilities stay in the Android `ViewModel`, and which rules are
   owned by the plain Kotlin machine?
5. How does process restoration treat a completed payment and an interrupted
   in-flight payment? Does restoration automatically submit payment again?
6. Based on README and the artifacts, why does Afsm generate a Mermaid graph in
   addition to keeping phase-local executable rules and tests?
7. Does this sample feel like it requires a full MVI architecture, or like an
   ordinary Android ViewModel using a focused flow model? Explain which code
   created that impression.
8. What remains ambiguous, and which additional file or documentation would you
   open before adopting this in a real feature?

## Ratings

Rate each statement from 1 (strongly disagree) to 5 (strongly agree):

- I can explain why `Command` exists without a separate `Effect` output type.
- I can explain when to read the graph, machine, and tests.
- The Compose/ViewModel boundary looks like ordinary Android feature code rather
  than a required app-wide MVI event surface.
- I can predict who owns async work, result delivery, and restoration.
- I would consider trying Afsm on a complex flow after reading these artifacts.

Finally record the first place you hesitated, any file hop that felt avoidable,
and the smallest change that would improve first-use understanding.
