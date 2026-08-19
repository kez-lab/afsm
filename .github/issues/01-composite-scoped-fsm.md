# [RFC] Composite & Scoped FSM: Sub-Machine 합성을 통한 상태 폭발(State Explosion) 방지

**Labels**: `enhancement`, `architecture`, `core`, `rfc`

## 1. 배경 및 문제점 (Problem Statement)
- 현재 Afsm은 단일 레벨(Flat) 상태 머신 모델을 사용합니다.
- 복잡한 화면에서 여러 개의 독립적인 상태(예: 입력 폼 작성 상태 + 백그라운드 오디오 재생 상태 + 바텀시트 열림 상태 등)가 공존할 경우, 상태 조합이 직교 곱(Cartesian Product)으로 폭증하여 `Editing_Playing_SheetOpen`과 같이 수많은 상태를 정의해야 하는 **상태 폭발(State Explosion)** 문제가 발생할 수 있습니다.

## 2. 제안하는 해결 방안 (Proposed Solution)
- **Reducer / Machine Composition (합성)** 패턴 도입:
  - 독립적인 기능 도메인별로 작은 단위의 `SubMachine`을 각각 정의.
  - 상위 `ScreenMachine`에서 `combineMachines` 또는 `scope` 연산자를 통해 하위 머신들을 레고 블록처럼 결합.
- Swift의 *The Composable Architecture (TCA)* `Scope` 또는 *XState*의 `Actor / Parallel Statechart` 개념을 Kotlin DSL에 맞게 최적화.

## 3. 예상 API 디자인 (Design Draft)

```kotlin
// 1. 하위 서브 머신 정의 (독립적인 오디오 플레이어 머신)
val playerMachine: AfsmMachine<PlayerState, PlayerEvent, PlayerCommand> = afsmMachine { ... }

// 2. 부모 머신에서 Scoped 합성
val screenMachine: AfsmMachine<ScreenState, ScreenEvent, ScreenCommand> = afsmMachine {
    // 부모 State/Event를 하위 머신의 State/Event로 매핑
    scope(
        machine = playerMachine,
        toState = { it.playerState },
        updateState = { state, subState -> state.copy(playerState = subState) },
        toSubEvent = { (it as? ScreenEvent.PlayerAction)?.event },
        toParentCommand = { ScreenCommand.Player(it) }
    )

    // 부모 화면 고유의 Phase/Event 전이 정의
    phase(ScreenPhase.Main) { ... }
}
```

## 4. 기대 효과 (Expected Benefits)
- 복잡한 대규모 화면에서도 기능별로 머신을 쪼개어 독립적으로 개발 및 단위 테스트 가능.
- 상태 폭발 없이 병렬/직교 상태(Orthogonal Regions)를 안전하게 관리.

## 5. 완료 기준 (Acceptance Criteria)
- [ ] `afsm-core`에 `scope` 및 `combineMachines` 합성 연산자 추가.
- [ ] 하위 머신 이벤트 전파 및 State 업데이트의 격리성 보장.
- [ ] 서브 머신 합성에 대한 `.mmd` Mermaid 다이어그램 서브그래프(Subgraph) 렌더링 지원.
- [ ] JVM 단위 테스트 추가 (`afsm-test`).
