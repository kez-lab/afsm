시작 시간: 2026-07-11 18:58:32 KST  
종료 시간: 2026-07-11 18:59:12 KST  
소요 시간: 40초

## 과제 답변 원문

1. 시작 phase는 `Idle`이다. 머신 자체는 실제 product ID를 제공하지 않는다. 테스트처럼 호출자가 `checkoutState(productId = ...)` 형태의 런타임 상태로 제공해야 한다.

2. 주 성공 경로:

   `Idle` → `ScreenEntered` → `ProductLoading` → `LoadProduct` command → `ProductLoaded` → `ProductReady` → `PayClicked` → `PaymentInProgress` → `SubmitPayment` command → 일치하는 `PaymentSucceeded` → `Completed` + `PaymentCompleted` effect

3. `ProductLoading` 진입 시 `LoadProduct(data.productId)` command가 생성된다. `Idle`에서 `ScreenEntered`를 처리해 해당 phase로 진입하는 것이 계기다.  
   `PaymentInProgress` 진입 시 `SubmitPayment(requestId, product)` command가 생성된다. 상품이 있는 상태의 `PayClicked`, 또는 결제 실패 후 `RetryClicked`가 진입 계기다. 실제 외부 작업을 누가 어떻게 실행하는지는 판단할 수 없음.

4. 로딩 중 `ProductUnavailable`이 들어오면 상품을 `null`로 만들고 `"Product is no longer available."` 오류를 기록한 뒤 `ProductUnavailable` phase로 간다. 테스트상 여기서 `PayClicked`는 `Invalid`이며 출력도 없다.

5. 현재 요청 ID와 일치하는 `PaymentFailed`가 오면 실패 메시지를 기록하고 `PaymentFailed` phase로 간다. 상품이 남아 있을 때 `RetryClicked`를 누르면 request ID를 증가시키고 오류를 지운 뒤 다시 `PaymentInProgress`로 진입하여 새 `SubmitPayment` command를 만든다. 상품이 없다면 phase는 그대로이고 필수 상품 오류만 기록한다.

6. `PaymentInProgress`의 `phase.requestId`와 결제 결과 이벤트의 `requestId`를 비교한다. 다르면 stale result로 `Ignored` 처리한다. 이미 `Completed`가 된 뒤 도착한 성공·실패 결과도 무시한다.

7. 예:

- `Handled`: `Idle`에서 `PayClicked`. 오류 데이터를 갱신하지만 phase 전환이나 출력은 없다.
- `Ignored`: 결제 진행 중 중복 `PayClicked`. 해당 상황을 명시적으로 인식하지만 의도적으로 아무 작업도 하지 않는다.
- `Invalid`: `ProductUnavailable`에서 `PayClicked`. 그 phase에 처리 규칙이 없어서 유효하지 않다.

   따라서 `Handled`는 규칙이 실행된 경우, `Ignored`는 규칙은 있지만 의도적으로 버린 경우, `Invalid`는 현재 phase에 적용 가능한 규칙 자체가 없는 경우로 이해했다.

8. 머신에 `PaymentStatusUnknown`으로 전환하는 규칙이 없고, 테스트가 이 phase의 상태를 직접 복원해 만든다. 따라서 일반 이벤트 흐름이 아니라 복원 시 이미 결제 요청이 나갔지만 결과를 확정할 수 없는 상태를 나타내는 것으로 보인다. 여기서는 entry command가 없어 자동 결제 재시도가 없고, `RetryClicked`도 `Invalid`이며 `primaryAction`도 없다. 실제로 어떤 저장·복원 로직이 이 phase를 선택하는지는 판단할 수 없음.

9. 지속 상태로 표현된 완료 결과는 `CheckoutPhase.Completed(orderId)`이고, 일회성 UI 알림은 `CheckoutEffect.PaymentCompleted(orderId)`이다. 실제 프로세스 종료 이후에도 상태가 영속 저장되는지는 판단할 수 없음.

10. 전체 흐름 파악에는 Mermaid graph, 정확한 조건과 데이터 변경에는 machine, 실행 가능한 동작 증거에는 tests가 가장 유용했다.

11. 판단할 수 없는 내용:

- `CheckoutState`, `CheckoutData`, phase/event/command/effect의 전체 선언
- command를 실행하고 결과 이벤트로 변환하는 주체와 오류 처리
- effect의 소비 및 중복 소비 방지 방식
- 상태의 실제 저장·복원 방식
- `PaymentStatusUnknown`을 선택하는 정확한 복원 조건
- `ProductUnavailable` 이후 화면 이탈이나 재진입 방법
- 동시 이벤트 처리와 스레드 안전성
- 선언되지 않은 이벤트가 모두 동일하게 `Invalid`가 되는지에 대한 Afsm의 일반 규칙

## 5개 평가 문항

1. 주 흐름을 빠르게 찾을 수 있었다: **5/5**
2. 실패·재시도·중복·stale 결과 동작을 예측할 수 있었다: **5/5**
3. command와 effect를 상태 변경과 구분할 수 있었다: **4/5**
4. machine, graph, tests가 서로 일치했다: **5/5**
5. 복잡한 ViewModel과 callback을 추적하는 것보다 이 표현을 선호한다: **4/5**

## 이해하기 어렵거나 확신하지 못한 부분

처음 멈칫한 문법은 `command { ... }`였다. 블록이 그 자리에서 외부 작업을 실행하는지, 아니면 실행할 값을 출력으로 생성하는지만 처음에는 명확하지 않았다. 테스트의 `assertCommands`를 보고 후자로 이해했지만 실제 실행 계약은 판단할 수 없다.

또한 `updateData` 직후 `transitionTo` 블록에서 읽는 `data.nextPaymentRequestId`가 갱신 전 값인지 갱신 후 값인지 코드만 처음 읽을 때 확신하기 어려웠다. 테스트 결과는 갱신 후 값이 사용된다는 해석을 뒷받침한다.

## 추가 자료를 보고 싶었던 순간과 이유

머신 선언의 `CheckoutState`, `CheckoutEvent`, `CheckoutCommand`, `CheckoutEffect`를 처음 본 순간 이 타입들의 정의 파일을 열어보고 싶었다. 전체 이벤트 집합, 데이터 기본값, phase별 payload, command와 effect의 의미를 확인하고 싶었기 때문이다. 정확한 파일명은 첨부 자료만으로 판단할 수 없음.

## 코드 리뷰 설명 개선점

머신 코드와 같은 리뷰 단위에 `State/Data`, `Phase`, `Event`, `Command`, `Effect`의 짧은 역할표를 추가하면 좋겠다. 특히 command는 외부 작업 요청이고 effect는 일회성 UI 출력이라는 점을 바로 명시하면 흐름 설명이 쉬워진다.

이 결과는 실제 인간 참가자의 사용성 테스트가 아니라, 첨부된 네 파일만 사용해 수행한 **AI review**다. 다음 단계로는 동일 과제를 인간 Android 개발자에게 수행하게 하고, 이 AI 결과와 최초 혼란 지점을 비교하는 것을 권장한다.
