# Checkout First-Use AI Review Evidence

Date: 2026-07-11

Repository baseline: `6b5126218f3215efd18a5d8891da4818a37ddd69`

## Classification

This is an AI-assisted constrained review, not a human Android developer
first-use session. Its timing and ratings must not be combined with the human
participant gates in the Checkout comprehension protocol.

The submitted result reports that the AI used only the participant task and
the three Checkout artifacts. Follow-up metadata identifies the product/model
label as `gpt 5.6 sol`, confirms the exact final wrapper prompt, and states that
the session opened a local folder. See `metadata-supplement.md`.

## Reported Timing

- Start: `2026-07-11 18:58:32 KST`
- End: `2026-07-11 18:59:12 KST`
- Duration: `40 seconds`
- Participant type: AI
- AI product/model label: `gpt 5.6 sol`
- Input method: local folder
- Human experience profile: not applicable
- Facilitator intervention: not reported

## Constrained Inputs

| Role | Repository source | SHA-256 |
|---|---|---|
| Participant task | `docs/checkout-first-use-participant-task.md` | `2dd2cb556cdd0ac7e05a88fe7ffd966d7284f74ab5938239d16490c67ea36ade` |
| Machine | `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachine.kt` | `e791cb89f90bed813fe69a1845fdadc5ef4a31904f8e7b05904727477289de45` |
| Graph | `sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd` | `218f0bbd89d3136419d68a5b8170e5b747f324dd118f75fe0f77e5dbccaa68a8` |
| Tests | `sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt` | `bb1bdf75d1ae871cc0f6b060f7dc5253733279c0ca79b19b1945e5eed8c1443c` |

The repository-local convenience copies under `afsm-blind-review-input/` had
matching hashes when prepared. The final prompt explicitly prohibited opening
repository, Wiki, README, internet, or other files. Because the session opened
a folder inside the Afsm repository, parent repository context could have been
discoverable depending on the AI tool. The answer's claim that no other context
was used is therefore self-reported rather than technically isolated.

## Evidence Files

- `submission.md`: the submitted answer, timing, ratings, hesitation, and
  requested-extra-context text preserved verbatim.
- `score.md`: facilitator scoring and product-finding classification performed
  after the submitted answer was fixed.
- `metadata-supplement.md`: follow-up product/model, exact prompt, input method,
  and the resulting context-isolation boundary.

## Evidence Boundary

The result proves that one prompt-constrained AI review reconstructed the
intended Checkout semantics and surfaced three first-read hypotheses. It does not
prove human comprehension time, human preference, first-author usability, or
production-like adoption. The long-term Goal therefore remains blocked on a
real no-coaching Android developer session and a production-like pilot.
