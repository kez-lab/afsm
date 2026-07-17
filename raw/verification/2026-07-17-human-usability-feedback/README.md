# Informal Human Usability Feedback Evidence

Date received: 2026-07-17

Repository state when ingested: `b4fe0a7da03e144e0765ac4d5c317fd55c451991`

## Classification

This is feedback relayed by the project owner from a person who reviewed Afsm.
It is human evidence, not an AI review.

The participant's Android experience, prior MVI/FSM experience, exact Afsm
artifacts, task, session date, timing, coaching, and verbatim follow-up answers
were not recorded. The repository commit that the person reviewed is also not
known. This evidence therefore identifies product problems but does not satisfy
the controlled no-coaching first-use protocol or the production-like pilot
gate.

## Relayed Feedback

The following text is preserved as received in the Codex task:

> 1. command 랑 effect 가 굳이 나눠져 있는 이유가 잘 이해가 안감,
> 키워드가 너무 많아서 러닝커브가 굉장히 심할듯.
>
> 2. 상태전이 규칙이 한눈에 보이지 않는데 특히 전이 규칙 정의를 각
> 상태 람다 내부에서 진행해서 어쩔 수 없는 트레이드 오프 같음. -> 이
> 경우 상태 전이도를 보기 위해 mmd 그래프를 만든 이유를 설명드렸는데
> 이 이유등이 리드미와 문서등에 정의가 잘 되어야할듯
>
> 3. 샘플이 너무 mvi 느낌만 강하게 남

## Evidence-Backed Findings

1. One human reader could not infer why host work (`Command`) and one-shot UI
   output (`Effect`) need separate concepts. The total vocabulary felt large
   enough to predict a steep learning curve.
2. The phase-local DSL did not present the complete transition topology in one
   visual scan. The Mermaid graph's role became understandable only after a
   verbal explanation, so the current self-service documentation did not make
   that trade-off discoverable for this reader.
3. The samples left a strong MVI-framework impression even though Afsm's stated
   product position is a focused flow model that keeps ordinary Android
   `ViewModel` architecture.

## What This Does Not Prove

- It does not prove that every Android developer finds these concepts unclear.
- It does not prove whether a terminology-only explanation would be sufficient.
- It does not prove that `Command` and `Effect` should be merged; their delivery
  and ownership contracts differ in the current implementation.
- It does not prove first-use completion time, authoring ability, or preference
  against a practical ViewModel baseline.
- It does not satisfy the production-like pilot requirement.

## Product Use

This evidence is sufficient to reopen the pre-release output-model and sample
positioning hypotheses. The next product cycle must compare the current
`Command`/optional `Effect` design with smaller concept surfaces on a realistic
flow, and must make the machine-versus-graph reading contract explicit. It must
not assume that adding more explanation alone resolves the learning-cost
finding.
