# 테스트 가이드 (Testing Guide)

테스트는 실행 가능한 흐름 사양(Executable Flow Specification)입니다.

---

## 1. 순수 전이 단위 테스트 (Pure Transition Tests)

머신의 핵심 수식은 다음과 같습니다:

```text
현재 State + Event -> 다음 State + Commands + Command Invocations + Decision
```

```kotlin
val result = checkoutMachine.transition(
    state = checkoutState(
        productId = product.id,
        phase = CheckoutPhase.ProductReady,
        data = CheckoutData(productId = product.id, product = product),
    ),
    event = CheckoutEvent.PayClicked,
)

result
    .assertTransitioned()
    .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 1))
    .assertCommands(
        CheckoutCommand.SubmitPayment(requestId = 1, product = product),
    )
```

`afsm-test` 모듈이 제공하는 주요 단언문(Assertion):
- `assertTransitioned()`
- `assertHandled(reason?)`
- `assertIgnored(reason?)`
- `assertInvalid(reason?)`
- `assertState(...)`, `assertPhase(...)`, `assertData(...)`
- `assertCommands(...)`, `assertNoCommands()`
- `assertCommandInvocations(...)`, `assertNoCommandInvocations()`
- `assertNoOutputs()` (모든 커맨드 방출이 없음을 검증)

---

## 2. 최소 엣지 케이스 테스트 매트릭스

실무 수준의 화면에서는 다음 8가지 케이스를 기본적으로 검증해야 합니다:

1. **정상 성공 경로 (Happy Path)**
2. **Phase 변경 없는 유효성 검증 실패** (오류 메시지만 Data에 기록)
3. **Repository/서버 통신 실패**
4. **재시도 (Retry)**
5. **작업 진행 중 사용자의 중복 클릭/이벤트 방어**
6. **오래된 비동기 결과(Stale Result) 방어**
7. **현재 Phase에서 유효하지 않은 이벤트 거부 (Invalid)**
8. **안전하지 않은 작업이 자동 재실행되지 않는 복원 상태 (Restoration)**

---

## 3. ViewModel 통합 테스트 (ViewModel Wiring Tests)

- **머신 테스트**: 비즈니스 흐름과 전이 규칙을 검증합니다 (JVM 고속 실행).
- **ViewModel 테스트**: Command가 올바른 Android 의존성(Repository 등)을 호출하고 결과를 적절한 Event로 머신에 되돌려주는지를 검증합니다.

ViewModel 테스트에서는 내부 Event를 직접 만들지 않고, UI가 호출하는 공개 동사형 메서드를 호출하세요:

```kotlin
viewModel.updateTitle("Plan")
viewModel.save()
mainDispatcher.scheduler.advanceUntilIdle()

assertEquals(listOf("Plan"), repository.savedTitles)
assertEquals(DraftPhase.Saved, viewModel.state.value.phase)
```

머신의 모든 분기 조건을 ViewModel 테스트에서 일일이 중복 검증할 필요는 없습니다.

---

## 4. 런타임 계약 테스트 (Runtime Contract Tests)

런타임 테스트는 FIFO 이벤트 직렬 처리, 커맨드 실행 전 State 우선 게시, 큐 오버플로 방어, 실패 정책, Host 종료, Phase 소유 커맨드 취소 동작을 검증합니다.

---

## 5. 그래프 검증 (Graph Tests)

실행 가능한 머신으로부터 `.mmd` 다이어그램을 생성하고 검증합니다. 그래프는 전체 위상을 보여주지만 일부 no-op 결정은 생략하므로, 그래프가 단위 테스트를 완전히 대체할 수는 없습니다.

---

## 6. TDD 개발 순서

1. 문서 또는 사양에 기대 동작을 먼저 정의합니다.
2. 집중 단위 테스트를 추가하거나 수정합니다.
3. 테스트가 실패하는 것을 확인합니다.
4. 최소한의 프로덕션 코드를 작성합니다.
5. 집중 테스트를 실행하여 통과시킵니다.
6. 전체 모듈 및 릴리스 검증 스크립트를 실행합니다.

빌드를 통과시키기 위해 실패하는 테스트를 무단으로 삭제하거나 완화하지 마세요.
