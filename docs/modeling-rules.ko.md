# 모델링 규칙 (Modeling Rules)

## Afsm은 선택적으로 사용하세요

현재 Phase에 따라 허용되는 동작과 유효성이 달라지는 다단계 고분기 흐름에 Afsm을 적용하세요. 단순한 데이터 조회/표시 화면은 Android의 일반적인 StateFlow 패턴이 더 직관적입니다.

---

## State는 Phase와 Data의 결합입니다

- `Phase`: "현재 어떤 비즈니스 단계에 있는가?"를 나타냅니다.
- `Data`: 여러 Phase에 걸쳐 유지되어야 하는 비즈니스 데이터(제목, 입력값 등)를 보관합니다.
- Android 프레임워크 객체(Context, View, Compose State 등)는 둘 다에 절대 넣지 마세요.
- Phase에 페이로드 데이터를 둘 때는 `PaymentInProgress(requestId)`처럼 해당 Phase가 활성화되어 있을 때만 존재하는 값에 한정하세요.

동일한 도메인 데이터를 여러 Phase 생성자에 중복 선언하는 것은 피해야 합니다.

---

## Event는 입력입니다

Event는 사용자의 의도나 비동기 Command 작업의 완료 결과를 나타냅니다. Event의 이름은 UI 위젯(버튼 등)이 아니라 **"어떤 일이 일어났는가"**를 설명해야 합니다.

머신 Event는 내부 흐름 어휘입니다. Android UI는 Event를 직접 알지 못하게 하고, `pay()`, `retry()`, `updateEmail(value)`와 같은 ViewModel의 동사형 함수를 호출하도록 설계하세요.

---

## Command는 외부 작업입니다

순수 Reducer 내부에서 실행할 수 없는 Repository, DB, 타이머, 네트워크, 외부 SDK 호출은 Command로 발행합니다. ViewModel이 Command를 실행하고, 결과를 새 Event로 머신에 전달합니다.

어떤 Phase에 진입했을 때 즉시 시작되어야 하는 작업은 `onEnter` 진입 커맨드를 활용하세요:

```kotlin
phase(CheckoutPhase.ProductLoading) {
    onEnter {
        command("LoadProduct") {
            CheckoutCommand.LoadProduct(data.productId)
        }
    }
}
```

이렇게 하면 상태 수집(Re-collection) 과정에서 작업이 불필요하게 재실행되는 위험 없이, 명시적인 비즈니스 Phase와 작업을 직관적으로 연결할 수 있습니다.

### StateMachine ➔ Command ➔ ViewModel ➔ Event 구조의 설계 배경과 이점

Afsm은 상태 머신(순수 비즈니스 규칙)과 ViewModel(비동기 I/O 실행)을 명확히 분리합니다:

1. **모킹 없는 0.1ms 단위의 순수 JVM 단위 테스트**: 머신 내부에서 Repository를 직접 호출하거나 코루틴을 실행하지 않으므로, 복잡한 Mockito나 테스트 디스패처 없이 모든 분기(`Handled`, `Ignored`, `Invalid`)를 순수 데이터만으로 서브 밀리초 단위로 100% 검증할 수 있습니다.
2. **상태 전이 규칙의 단일 공급원 (Single Source of Truth)**: 화면의 모든 비즈니스 흐름은 `*StateMachine.kt`와 Mermaid 다이어그램 한곳에만 존재합니다. ViewModel은 비즈니스 분기(`if/else`) 없이 Command를 실행하고 결과 Event를 회신하는 단순 어댑터(Thin Bridge)가 되어 복잡성이 사라집니다.
3. **화면 복잡도에 따른 유연한 선택**: 단순한 조회/표시 화면(`Loading -> Content / Error`)에는 일반 `ViewModel + StateFlow` 패턴을 권장합니다. 다단계 고분기, 결제 재시도, 늦게 도착한 비동기 응답(Stale result) 방어가 필요한 복잡한 화면에 Afsm을 적용할 때 진가가 발휘됩니다.

자세한 토론은 [Architecture FAQ Discussion #62](https://github.com/kez-lab/afsm/discussions/62)에서 확인할 수 있습니다.

---

## UI 동작을 위한 제4의 출력 타입(Effect)은 두지 않습니다

- 비즈니스 완료 결과는 State에 저장합니다.
- 화면 이동(Navigation)이 필요한 경우 Route 컴포저블에서 `LaunchedEffect` 등을 통해 지속되는 완료 State를 관찰합니다.
- UI에서 시작해 UI로 끝나는 단순 인터랙션(완료 버튼 클릭 후 다이얼로그 닫기 등)은 직접 UI 콜백으로 처리합니다.
- 중복 처리가 위험한 경우에만 State에 확인(Acknowledgement) 플래그를 둡니다.

이를 통해 프로세스 재생성이나 UI Collector 수집 공백 시 발생하기 쉬운 1회성(One-shot) Effect 채널의 모호성을 방지합니다.

---

## 실제 조건 분기가 있을 때만 case를 사용하세요

조건이 없는 단순 전이는 `on<Event>` 블록 내에 직접 작성합니다:

```kotlin
on<CheckoutEvent.ProductLoaded> {
    updateData { data, event -> data.copy(product = event.product) }
    transitionTo(CheckoutPhase.ProductReady)
}
```

하나의 이벤트에 대해 다이어그램에서도 구분되어 보여야 할 명확한 조건 분기가 있을 때만 `case`를 사용합니다:

```kotlin
on<CheckoutEvent.PaymentSucceeded> {
    case("matching request", condition = { phase.requestId == event.requestId }) {
        transitionTo<CheckoutPhase.Completed> {
            CheckoutPhase.Completed(event.receipt.orderId)
        }
    }
    ignore(
        reason = "Stale payment result.",
        condition = { phase.requestId != event.requestId },
    )
}
```

---

## 전이 결정(Decision)의 의미

| Decision | 의미 |
|---|---|
| `Transitioned` | 규칙이 승인되어 다른 Phase로 전이함 |
| `Handled` | 동일한 Phase를 유지하면서 Data를 업데이트하거나 Command 작업을 방출함 |
| `Ignored` | 이벤트를 인식했으나 의도적으로 아무 작업도 수행하지 않음 (정상 동작) |
| `Invalid` | 현재 Phase에서 유효한 규칙이 없거나 명시적으로 이벤트를 거부함 |

예상 가능한 중복 클릭이나 이전 요청의 늦은 응답(Stale Result)에는 `Ignored`를 사용하세요. 프로그래밍 실수나 논리적으로 불가능한 타이밍의 호출에는 `Invalid`를 사용합니다.

---

## 늦게 도착한 비동기 결과(Stale Result)를 명시적으로 방어하세요

활성 Phase나 State에 고유한 `requestId`를 부여하세요. 현재 요청과 일치하는 결과만 수락하고 이전 요청의 응답은 `Ignored`로 처리합니다. 두 경로 모두 단위 테스트로 검증하세요.

---

## 비즈니스 상태와 UI 상태의 분리

- **머신 상태 (Business Flow State)**: 결제 진행 상태, 재시도 가능 여부, 선택된 상품, 완료된 주문 번호, 흐름을 바꾸는 유효성 검증 결과.
- **UI 상태 (Pure UI State)**: 포커스 여부, 스크롤 위치, 애니메이션 진행률, 바텀시트 펼침 상태, `SnackbarHostState`.

---

## 읽기 계약 (Readability Contract)

- **그래프 (Mermaid Graph)**: 전체 위상(Topology)과 조건 분기 조망.
- **머신 코드 (Machine Source)**: 세부 데이터 조작 및 작업 실행 규칙.
- **테스트 (Unit Tests)**: 엣지 케이스 및 이벤트 무시/거부 동작의 실행 가능한 증명.

Phase 단위 람다는 로컬 규칙의 응집도를 높이지만 전체 파일 스캔성을 떨어뜨립니다. 자동 생성되는 Mermaid 그래프는 이를 완벽히 보완하는 필수 아티팩트입니다.
