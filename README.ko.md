# Afsm

![Status](https://img.shields.io/badge/status-internal%20beta-orange)
![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7F52FF?logo=kotlin)
![Android](https://img.shields.io/badge/android-AGP%208.10.1-3DDC84?logo=android)
![Distribution](https://img.shields.io/badge/distribution-Maven%20Local-lightgrey)

[English](README.md) | **한국어**

Afsm은 Android 팀이 복잡한 화면 흐름을 더 쉽게 읽고 검증하며 안전하게
변경할 수 있도록 만드는 도구입니다. `ViewModel`의 `state.copy(...)`,
코루틴, 콜백, 테스트에 흩어진 비즈니스 흐름 규칙을 하나의 순수 Kotlin
상태 머신으로 옮기되, Android `ViewModel`은 lifecycle, `StateFlow`, saved
state, repository와 UI 어댑터 역할을 그대로 담당합니다.

의미 있는 단계, 재시도, 동시 또는 오래된 비동기 결과, 현재 단계에 따라
달라지는 규칙이 있는 화면에 Afsm을 사용하세요. 단순한 화면은 일반
`ViewModel + StateFlow`가 더 명확하다면 그대로 두는 것이 좋습니다.

## Afsm을 만들기 시작한 이유

복잡한 Android 화면은 각각의 핸들러는 합리적으로 보여도 전체 흐름은
코드 곳곳에 흩어져 한눈에 존재하지 않는 상태가 되곤 합니다. “지금 무엇이
가능한가?”에 답하려면 `ViewModel`, UI 콜백, repository 호출, 결과 콜백과
테스트를 오가며 규칙을 다시 조립해야 합니다.

Afsm은 그 답을 한곳에서 읽고 실제로 실행할 수 있게 만들려는 시도에서
출발했습니다. `ViewModel`을 대체하거나 Kotlin `copy()`를 감추거나 앱 전체에
MVI 아키텍처를 강제하지 않습니다. 복잡한 feature 하나의 비즈니스 흐름을
실행하고 테스트하고 상태 전이도로 볼 수 있게 만드는 것이 목적입니다.

## 세 가지 개념

공개 머신 어휘는 의도적으로 작게 유지합니다.

| 개념 | 의미 |
|---|---|
| `State` | 현재 `Phase`와 지속되는 비즈니스 `Data` |
| `Event` | 머신 입력. 사용자 의도 또는 외부 작업 결과 |
| `Command` | Android host에 외부 작업 실행을 요청하는 값 |

`Phase`와 `Data`는 보통 `AfsmState<Phase, Data>`로 묶습니다. 외부 작업을
시작하지 않는 화면은 `AfsmNoCommand`를 사용할 수 있습니다.

### Command가 분리된 이유

순수 머신은 suspend repository, database, timer, SDK를 직접 호출하면 안
됩니다. 대신 `Command`를 반환하고 `ViewModel`이 실행한 뒤 결과를 새
`Event`로 전달합니다.

```text
UI 의도 -> Event -> 순수 머신 -> State + Command
                                      |
                                      v
                              ViewModel이 작업 실행
                                      |
                                      v
                                   결과 Event
```

이 분리 덕분에 전이는 결정적이고 JVM에서 테스트할 수 있습니다. 또한 state를
다시 수집하거나 복원했다는 이유만으로 외부 작업이 우연히 재실행되는 것도
막을 수 있습니다.

Afsm에는 별도의 `Effect` 출력 채널이 없습니다. 제품 완료 결과는 state에
남깁니다. Done 클릭 뒤 editor 닫기처럼 UI에서 시작해 UI에서 끝나는 동작은
직접 UI 콜백으로 유지합니다. navigation이 필요하면 route가 durable 완료
state를 `LaunchedEffect`로 관찰할 수 있습니다.

## 최소 머신

```kotlin
sealed interface DraftPhase {
    data object Editing : DraftPhase
    data object Saving : DraftPhase
    data object Saved : DraftPhase
}

data class DraftData(
    val title: String = "",
    val errorMessage: String? = null,
)

typealias DraftState = AfsmState<DraftPhase, DraftData>

sealed interface DraftEvent {
    data class TitleChanged(val value: String) : DraftEvent
    data object SaveClicked : DraftEvent
    data object SaveCompleted : DraftEvent
}

sealed interface DraftCommand {
    data class Save(val title: String) : DraftCommand
}

val draftMachine: AfsmDefaultMachine<DraftState, DraftEvent, DraftCommand> =
    afsmMachine {
        initial(DraftPhase.Editing, DraftData())

        phase(DraftPhase.Editing) {
            on<DraftEvent.TitleChanged> {
                updateData { data, event ->
                    data.copy(title = event.value, errorMessage = null)
                }
            }

            on<DraftEvent.SaveClicked> {
                case("valid title", condition = { data.title.isNotBlank() }) {
                    transitionTo(DraftPhase.Saving)
                }
                case("missing title", condition = { data.title.isBlank() }) {
                    updateData { copy(errorMessage = "Title is required.") }
                }
            }
        }

        phase(DraftPhase.Saving) {
            onEnter {
                command("Save") { DraftCommand.Save(data.title) }
            }
            on<DraftEvent.SaveCompleted> {
                transitionTo(DraftPhase.Saved)
            }
        }

        phase(DraftPhase.Saved)
    }
```

규칙 내부에서는 일반 Kotlin을 사용하세요. 하나의 event에 생성 그래프에도
나타나야 하는 이름 있는 조건 결과가 여러 개 있을 때만 `case(...)`를
사용합니다.

## Android 경계

머신 내부에서는 event를 사용하지만 Compose UI가 event 타입을 알 필요는
없습니다. `ViewModel`에는 기능을 드러내는 동사형 메서드를 노출합니다.

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftMachine,
        commandHandler = { command: DraftCommand, dispatchEvent ->
            when (command) {
                is DraftCommand.Save -> {
                    repository.save(command.title)
                    dispatchEvent(DraftEvent.SaveCompleted)
                }
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun updateTitle(value: String) = host.dispatch(DraftEvent.TitleChanged(value))
    fun save() = host.dispatch(DraftEvent.SaveClicked)
}
```

```kotlin
@Composable
fun DraftRoute(viewModel: DraftViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DraftScreen(
        title = state.data.title,
        onTitleChange = viewModel::updateTitle,
        onSave = viewModel::save,
    )
}
```

이는 의도적인 Android 코드입니다. Afsm이 하나의 범용
`onEvent(Event)` MVI 경계를 공개하도록 요구하지 않습니다.

## 머신, 그래프, 테스트가 모두 필요한 이유

phase 내부 람다는 규칙을 해당 규칙이 유효한 상태 가까이에 둡니다. 반면 큰
머신의 전체 흐름을 한눈에 훑기에는 적합하지 않습니다. 그래서 Afsm은 세
산출물을 하나의 읽기 계약으로 봅니다.

| 산출물 | 가장 잘 답하는 질문 |
|---|---|
| 생성된 `.mmd` 그래프 | 이름 있는 조건과 entry command를 포함한 전체 topology는 무엇인가? |
| 머신 소스 | 정확히 어떤 data, guard, command와 순서를 사용하는가? |
| 전이 테스트 | payload 세부 사항과 그래프에 보이지 않는 `Handled`, `Ignored`, `Invalid` 동작이 증명됐는가? |

그래프는 장식도 아니고 코드를 대체하지도 않습니다. phase-local 규칙의
지역성이라는 트레이드오프를 보완해 reviewer에게 생성된 전체 머신 지도를
제공합니다. 실행 가능한 머신에서 생성되므로 손으로 관리하는 그림과 달리
build가 drift를 검사할 수 있습니다.

## 처음 시작하는 순서

1. 화면이 머신을 둘 만큼 복잡한지 먼저 판단합니다.
2. DSL을 쓰기 전에 phase와 중요한 전이를 그립니다.
3. 제품 역할이 드러나는 `*Flow.kt`에 `State`, `Event`, `Command`를 정의합니다.
4. phase-local 규칙을 구현하고 실제 조건이 있을 때만 `case`를 사용합니다.
5. 순수 머신을 먼저 테스트합니다.
6. 일반 Android `ViewModel`에서 실행하고 동사형 메서드를 노출합니다.
7. 머신·그래프·테스트를 나란히 검토합니다.

[시작 가이드](docs/getting-started.md)에서 시작한 뒤
[모델링 규칙](docs/modeling-rules.md)과
[그래프 생성](docs/graph-generation.md)을 읽으세요.

## 모듈

| 모듈 | 역할 |
|---|---|
| `afsm-core` | 순수 Kotlin 머신, DSL, topology, Mermaid 렌더링 |
| `afsm-runtime` | 직렬 event 처리와 command 실행 |
| `afsm-viewmodel` | `ViewModel.afsmHost(...)` 통합 |
| `afsm-test` | 전이 assertion helper |
| `afsm-graph-ksp` | 생성 graph registry |
| `afsm-graph-gradle-plugin` | `.mmd` export task |
| `sample-shop` | Android reference flow |
| `consumer-smoke` | 외부 Maven Local 컴파일·동작 gate |

## 빌드와 검증

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-test:test
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd
./scripts/verify-release-local.sh --no-daemon
```

Afsm은 아직 공개 배포되지 않았습니다. 실제 Android 팀의 사용성과 안전성을
더 높인다는 근거가 있다면 API는 변경될 수 있습니다.

[공개 API](docs/afsm-public-api.md), [테스트](docs/testing-guide.md),
[예제](docs/examples.md), [Auth](docs/auth-walkthrough.md),
[Checkout](docs/checkout-walkthrough.md),
[Product editor](docs/product-editor-walkthrough.md)를 이어서 읽으세요.
