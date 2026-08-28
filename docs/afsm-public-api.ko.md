# Afsm 전체 공개 API (Public API Reference)

상태: pre-release.

---

## 1. 핵심 타입 (Core Types)

```kotlin
data class AfsmState<P : Any, D : Any>(
    val phase: P,
    val data: D,
)

fun interface AfsmReducer<S : Any, E : Any, C : Any> {
    fun transition(state: S, event: E): AfsmTransition<S, C>
}

interface AfsmMachine<S : Any, E : Any, C : Any> :
    AfsmReducer<S, E, C>,
    AfsmGraphSource

interface AfsmDefaultMachine<S : Any, E : Any, C : Any> :
    AfsmMachine<S, E, C> {
    val initialState: S
}
```

- `AfsmMachine`: 화면 인자나 복원 상태로부터 동적 초기 상태를 주입받는 머신.
- `AfsmDefaultMachine`: 정적 기본 초기 상태를 갖는 머신.

---

## 2. DSL (Domain Specific Language)

```kotlin
afsmMachine<Phase, Data, Event, Command> {
    initial(Phase.Idle, Data())

    phase(Phase.Idle) {
        on<Event.Start> {
            updateData { copy(error = null) }
            command("StartWork") { Command.StartWork }
            transitionTo(Phase.Working)
        }
    }
}
```

### 주요 DSL 연산자

| API | 목적 |
|---|---|
| `initial(phase, data)` | 정적 초기 상태 선언 |
| `phase(value) { ... }` | 싱글톤 객체(Object) Phase의 규칙 등록 |
| `phase<Phase.Payload> { ... }` | 페이로드를 가진 Class Phase의 규칙 등록 |
| `on<Event.Type> { ... }` | 현재 Phase에서 특정 Event 처리 |
| `updateData { ... }` | Data 업데이트 |
| `transitionTo(...)` | 다른 Phase로 전이 |
| `command(label) { ... }` | Host가 실행할 Command 작업 방출 |
| `case(label, condition) { ... }` | 조건 분기에 이름 부여 |
| `ignore(reason, condition?)` | 의도적인 no-op(무시) 허용 |
| `invalid(reason, condition?)` | 명시적으로 Event 거부 |
| `onEnter`, `onExit` | Phase 진입/이탈 시 실행할 작업 정의 |
| `invoke(key, label) { ... }` | Phase 소유의 취소 가능한 비동기 작업 시작 |

---

## 3. 전이 결과 (AfsmTransition)

```kotlin
class AfsmTransition<S : Any, C : Any> {
    val state: S
    val commands: List<C>
    val commandInvocations: List<AfsmCommandInvocation<C>>
    val decision: AfsmDecision
}
```

- `Transitioned`: Phase가 변경됨.
- `Handled`: 동일 Phase를 유지하며 Data 갱신 또는 Command 방출.
- `Ignored`: 정상적인 무시.
- `Invalid`: 현재 규칙에서 거부됨.

---

## 4. 런타임 호스트 (AfsmHost)

```kotlin
class AfsmHost<S : Any, E : Any, C : Any>(
    initialState: S,
    reducer: AfsmReducer<S, E, C>,
    commandHandler: AfsmCommandHandler<C, E>,
    scope: CoroutineScope,
    config: AfsmConfig = AfsmConfig(),
) {
    val state: StateFlow<S>
    val isActive: Boolean
    fun send(event: E)
    fun trySend(event: E): Boolean
    operator fun invoke(event: E)
    fun close()
}
```

Host는 이벤트를 FIFO 큐로 직렬화하여 처리하고, Command 실행 전에 승인된 State를 먼저 게시하며, Command를 순차 실행하고 결과를 `send`로 되돌려받습니다.

### 런타임 실패 정책 (AfsmConfig)

| 정책 | 대상 | `Record` (기본값) | `Throw` (`AfsmConfig.strict()`) |
|---|---|---|---|
| `invalidTransitionPolicy` | Invalid 결정, Reducer 예외 | 진단 기록 후 Host 유지 | 예외 던지고 Host 중단 |
| `commandFailurePolicy` | 커맨드 핸들러 미처리 예외 | 진단 기록 후 Host 유지 | 예외 던지고 Host 중단 |
| `overflowPolicy` | 이벤트/커맨드 큐 포화 | 진단 기록 후 작업 드롭 | 큐 오버플로 예외 |

---

## 5. ViewModel 연동

```kotlin
fun <S : Any, E : Any, C : Any> ViewModel.afsmHost(
    machine: AfsmDefaultMachine<S, E, C>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C>
```

`ViewModel`이 `StateFlow`, `viewModelScope`, Repository, `SavedStateHandle`, Command 실행을 담당하고 UI에는 동사형 함수를 노출합니다.
