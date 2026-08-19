# [RFC] KSP 메타프로그래밍 기반 코드 생성: 어노테이션 기반 Zero-Boilerplate DSL

**Labels**: `enhancement`, `dx`, `ksp`, `tooling`, `rfc`

## 1. 배경 및 문제점 (Problem Statement)
- 현재 Afsm을 도입할 때 `Phase`, `Data`, `Event`, `Command`, `State`, `afsmHost` 등 선언해야 하는 타입과 파일의 양이 많습니다.
- 이러한 보일러플레이트는 특히 주니어 개발자나 신규 팀원에게 "간단한 기능을 만드는데 선언할 게 너무 많다"는 진입 장벽과 피로도를 유발합니다.

## 2. 제안하는 해결 방안 (Proposed Solution)
- **KSP(Kotlin Symbol Processing)** 기반의 코드 자동 생성 어노테이션 프로세서 도입:
  - ViewModel 메서드에 `@Transition`, `@FsmAction` 등의 어노테이션만 붙이면, 컴파일 타임에 `Event`, `Command` 클래스 및 전이 머신 바인딩 코드를 백그라운드에서 자동 생성.
  - (선택지) 어노테이션 방식 외에도 Kotlin 2.0+ Context Parameter를 활용한 초경량 인라인 DSL 제공.

## 3. 예상 API 디자인 (Design Draft)

```kotlin
@FsmScreen
class CheckoutViewModel : ViewModel() {

    // KSP가 CheckoutEvent.PayClicked, CheckoutCommand.ExecutePayment 등을 자동 생성
    @Transition(
        from = [CheckoutPhase.Idle::class],
        to = CheckoutPhase.Paying::class
    )
    fun pay(orderId: String) = command {
        paymentRepository.pay(orderId)
    }

    @Transition(
        from = [CheckoutPhase.Paying::class],
        to = CheckoutPhase.Completed::class
    )
    fun onPaymentSuccess(receipt: Receipt) {
        updateData { copy(receipt = receipt) }
    }
}
```

## 4. 기대 효과 (Expected Benefits)
- 화면 하나를 FSM으로 작성할 때 필요한 타이핑량 및 선언 코드(Boilerplate) 70% 이상 절감.
- 직관적인 ViewModel 메서드 형태를 유지하면서도 내부적으로는 엄격한 FSM 안전성을 획득.

## 5. 완료 기준 (Acceptance Criteria)
- [ ] `afsm-compiler-ksp` 모듈 신설 및 어노테이션 기반 심볼 파싱 구현.
- [ ] `@Transition`으로부터 불변 Machine 및 Event/Command 데이터 클래스 자동 생성.
- [ ] 컴파일 타임에 유효하지 않은 전이 경로(Invalid Transition Target) 에러 검출.
- [ ] 생성된 코드에 대한 컴파일 및 런타임 테스트 슈트 작성.
