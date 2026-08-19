# [RFC] 안드로이드 에코시스템 통합: 선언적 Compose Navigation 및 kotlinx.serialization 자동 상태 복원

**Labels**: `enhancement`, `android`, `compose`, `viewmodel`, `rfc`

## 1. 배경 및 문제점 (Problem Statement)
- 현재 Afsm은 Jetpack Compose Navigation과의 공식 연동 레이어가 없어, 화면 전환을 위해 UI 단에서 `LaunchedEffect(state)`로 상태를 관찰하고 수동으로 `navController.navigate()`를 호출해야 합니다.
- 또한 프로세스 사망(Process Death) 시 `SavedStateHandle`을 통한 상태 복원 로직을 Feature ViewModel마다 수동으로 필드 매핑해야 하는 번거로움이 있습니다.

## 2. 제안하는 해결 방안 (Proposed Solution)
1. **`afsm-navigation` 모듈**:
   - `Phase`와 `NavGraph` 라우트를 1:1로 매핑하는 선언적 `AfsmNavHost` 컴포저블 제공.
   - 상태 머신의 Phase 전이가 곧 Compose Navigation의 화면 이동/백스택 팝과 자동으로 동기화되도록 바인딩.
2. **`afsm-savedstate` (자동 직렬화 복원)**:
   - `kotlinx.serialization`과 연계하여 `@Serializable`이 붙은 `AfsmState`를 1줄의 코드로 `SavedStateHandle`에 저장 및 자동 복원해 주는 `saveableAfsmHost(...)` 팩토리 함수 제공.

## 3. 예상 API 디자인 (Design Draft)

```kotlin
// 1. kotlinx.serialization 기반 0줄 상태 복원
class CheckoutViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val host = savedStateHandle.saveableAfsmHost(
        key = "checkout_fsm",
        machine = checkoutMachine
    )
}

// 2. 선언적 Compose Navigation 연동
@Composable
fun CheckoutNavRoute(viewModel: CheckoutViewModel) {
    AfsmNavHost(host = viewModel.host) {
        phase(CheckoutPhase.AddressInput) { AddressInputScreen(...) }
        phase(CheckoutPhase.PaymentInput) { PaymentInputScreen(...) }
        phase(CheckoutPhase.Processing) { LoadingScreen(...) }
        phase(CheckoutPhase.Success) { orderId -> OrderCompleteScreen(orderId) }
    }
}
```

## 4. 기대 효과 (Expected Benefits)
- 화면 이동과 상태 머신의 단계를 완벽히 동기화하여 화면 전환 타이밍 버그 제거.
- 복잡한 프로세스 복원 보일러플레이트를 완전히 제거하고 타입 안정적인 복원 보장.

## 5. 완료 기준 (Acceptance Criteria)
- [ ] `afsm-navigation` 모듈 신설 및 `NavHost` 연동 컴포저블 구현.
- [ ] `SavedStateHandle.saveableAfsmHost` 팩토리 함수 및 직렬화 어댑터 구현.
- [ ] `sample-shop` 내의 멀티 스텝 화면(Checkout 등)에 적용하여 E2E 및 화면 회전/프로세스 복원 검증.
