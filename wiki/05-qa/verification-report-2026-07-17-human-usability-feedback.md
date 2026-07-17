---
title: Informal Human Usability Feedback 2026-07-17
updated: 2026-07-17
status: redesign-implemented-controlled-review-still-required
---

# Informal Human Usability Feedback 2026-07-17

## Result

The first relayed human feedback now exists. One reader reported three material
problems: the `Command`/`Effect` distinction and overall vocabulary were hard to
justify, phase-local rules obscured a one-glance view of the full topology until
the Mermaid graph was explained verbally, and the samples felt more like an MVI
framework than a focused Android FSM tool.

Raw evidence:
[Informal Human Usability Feedback](../../raw/verification/2026-07-17-human-usability-feedback/README.md)

## Product Meaning

- The existing AI review's `4/5` command/effect distinction was not merely a
  theoretical documentation concern. A human reader independently experienced
  the separation as concept and learning cost.
- The generated graph has product value, but its role is not sufficiently
  discoverable if a verbal explanation is required to understand why both the
  machine and graph exist.
- Afsm's non-goal of becoming a full MVI framework is not yet visible enough in
  the examples.

These findings reopen the output vocabulary and sample-presentation design.
They do not predetermine a rename or merge: the next experiment must compare
smaller models against command execution, best-effort UI delivery, restoration,
testing, and graph readability requirements.

## Evidence Boundary

This is human evidence, but not a controlled first-use result. Participant
background, reviewed inputs, baseline commit, timing, coaching, and authoring
task were not recorded. It cannot be scored against the existing Checkout
rubric and does not clear the production-like pilot gate.

The formal first-use and pilot requirements remain open. Future feedback intake
should preserve the participant profile, immutable inputs, timing, facilitator
intervention, answers, and debrief separately from the resulting design
decisions.

## Resulting Redesign Cycle

The selected redesign was implemented on 2026-07-17. Afsm removed the Effect
generic, DSL, runtime channel, Compose helper, and feature Effect types; kept
Command as the host-work boundary; documented the machine/graph/tests reading
contract; and changed samples to verb-named ViewModel methods with no feature
Event construction in screen files.

Repository verification is recorded in
[[verification-report-2026-07-17-effect-free-output-model|Effect-Free Output
Model Verification 2026-07-17]]. A controlled human session on the new revision
is still required to determine whether the original learning-curve and MVI
framing problems are actually resolved.
