# Checkout First-Use AI Review Metadata Supplement

Received: 2026-07-11

## User-Provided Wording

> 사용한 AI 제품과 모델 gpt 5.6 sol
>
> 너가 보내준 것과 최종 프롬포트: 동일함
>
> 로컬 폴더 연 방식

## Normalized Session Metadata

- AI product/model label: `gpt 5.6 sol`
- Final wrapper prompt: identical to the prompt supplied immediately before the
  review in the project session
- Input method: opened a local folder

The exact final wrapper prompt was:

```text
Afsm이라는 Android FSM 라이브러리의 사용성 테스트를 하고 있어요.

첨부된 네 파일만 읽고 과제를 수행해 주세요.
저장소, Wiki, README, 인터넷이나 다른 파일은 보지 마세요.

사전 설명 없이 과제를 수행하고 다음을 보내주세요.

1. 시작 시간과 종료 시간
2. 과제에 대한 답변 원문
3. 이해하기 어려웠거나 확신하지 못한 부분
4. 추가 자료를 보고 싶었던 순간과 그 이유

정답을 맞히는 시험이 아니라 헷갈리는 지점을 찾는 테스트입니다.
```

## Evidence Boundary

The local input folder was prepared under the Afsm repository. The prompt
explicitly prohibited reading repository, Wiki, README, internet, or other
files, and the submitted answer states that it used only the four supplied
files. However, opening a repository-local folder can make parent repository
context discoverable depending on the AI tool. No independent access log was
captured, so artifact-only use remains self-reported rather than technically
isolated.
