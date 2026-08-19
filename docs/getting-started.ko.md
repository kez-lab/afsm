# 시작하기 (Getting Started)

이 가이드는 Afsm을 사용하여 가장 작고 실용적인 Android 화면 기능을 구현하는 방법을 설명합니다. 완전한 컴파일 코드는 `consumer-smoke/app/src/main/kotlin/.../DraftQuickstart.kt`에서 확인할 수 있습니다.

설치 방법, 5분 Draft 퀵스타트, API 빠른 참조 및 인터랙티브 시뮬레이터는 [공식 문서 허브](index.html)에서 확인하세요.

---

## 1. 모듈 추가

Afsm은 Maven Central을 통해 배포됩니다. 추가 저장소 설정 없이 바로 추가할 수 있습니다.

```kotlin
dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0")
    implementation("io.github.afsm:afsm-runtime:0.1.0")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0")
    testImplementation("io.github.afsm:afsm-test:0.1.0")
}
```

---

## 2. 상태 머신이 필요한 화면인지 판단하기

화면에 명확한 **Phase(단계)**가 존재하고, 현재 단계에 따라 허용되는 규칙이 달라질 때 Afsm을 사용하세요. 단순한 Loading / Content / Error 수준의 화면이라면 일반적인 `ViewModel + StateFlow` 구성이 더 간결합니다.

임시 저장(Draft) 에디터의 경우 다음과 같은 흐름을 가집니다:

```text
Editing --SaveClicked--> Saving --DraftSaveCompleted--> Saved
                           |
                           +--DraftSaveFailed--> Editing
```

---

## 3. 흐름 모델(Flow Type) 정의

이 타입들은 `DraftFlow.kt`와 같은 화면 전용 파일에 정의합니다.

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
    data object DraftSaveCompleted : DraftEvent
    data class DraftSaveFailed(val message: String) : DraftEvent
}

sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
}
```

공개 어휘는 `State`, `Event`, `Command` 세 가지입니다. `Phase`와 `Data`는 상태의 형태를 이룹니다.

---

## 4. 상태 머신 작성

```kotlin
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
                    updateData { copy(errorMessage = "제목을 입력하세요.") }
                }
            }
        }

        phase(DraftPhase.Saving) {
            onEnter {
                command("SaveDraft") { DraftCommand.SaveDraft(data.title) }
            }

            on<DraftEvent.DraftSaveCompleted> {
                transitionTo(DraftPhase.Saved)
            }

            on<DraftEvent.DraftSaveFailed> {
                updateData { data, event -> data.copy(errorMessage = event.message) }
                transitionTo(DraftPhase.Editing)
            }
        }

        phase(DraftPhase.Saved)
    }
```

`SaveClicked`처럼 하나의 이벤트에 대해 이름 있는 조건 분기 결과가 여러 개 있을 때만 `case`를 사용합니다. 조건 없는 규칙이라면 `on<Event>` 블록 내부에서 `updateData`, `command`, `transitionTo`를 직접 호출하세요.

---

## 5. 순수 머신 단위 테스트

```kotlin
@Test
fun `valid draft starts save command`() {
    val editing = AfsmState(
        phase = DraftPhase.Editing,
        data = DraftData(title = "Plan"),
    )

    draftMachine.transition(editing, DraftEvent.SaveClicked)
        .assertTransitioned()
        .assertPhase(DraftPhase.Saving)
        .assertCommands(DraftCommand.SaveDraft("Plan"))
}
```

`onEnter` 진입 커맨드는 해당 상태로 전이하는 전이 결과에 자동으로 포함됩니다. 유효하지 않은 입력, 재시도, 중복 이벤트 방어, 오래된 결과 수신 방어 동작을 각각 독립된 테스트 케이스로 검증합니다.

---

## 6. ViewModel에서 호스팅

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftMachine,
        commandHandler = { command: DraftCommand, send ->
            when (command) {
                is DraftCommand.SaveDraft -> repository.save(command.title).fold(
                    onSuccess = { send(DraftEvent.DraftSaveCompleted) },
                    onFailure = { error ->
                        send(
                            DraftEvent.DraftSaveFailed(
                                error.message ?: "저장에 실패했습니다.",
                            ),
                        )
                    },
                )
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun updateTitle(value: String) = host.send(DraftEvent.TitleChanged(value))
    fun save() = host.send(DraftEvent.SaveClicked)
}
```

`send`는 비동기 작업 결과를 순차 처리 머신으로 되돌려주는 커맨드 핸들러의 권한입니다. 일반적인 UI 콜백이 아닙니다.

UI에는 `fun onEvent(event: DraftEvent)` 형태 대신 **기능의 의도를 드러내는 동사형 메서드(`updateTitle`, `save`)**를 노출하세요. 이를 통해 머신 이벤트를 내부에 캡슐화하고 평범하고 자연스러운 Android 코드를 유지할 수 있습니다.

---

## 7. Compose UI 연결

```kotlin
@Composable
fun DraftRoute(viewModel: DraftViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DraftScreen(
        title = state.data.title,
        errorMessage = state.data.errorMessage,
        isSaving = state.phase == DraftPhase.Saving,
        onTitleChange = viewModel::updateTitle,
        onSave = viewModel::save,
    )
}
```

포커스, 스크롤 위치, 시트 애니메이션 상태, SnackbarHostState 등 비즈니스 흐름을 변경하지 않는 순수 UI 상태는 Compose 레이어에서 관리합니다. 완료된 비즈니스 결과 상태만 머신 상태로 유지합니다.

---

## 8. 안전한 상태 복원 (SavedStateHandle)

프로세스가 종료되었을 때 비동기 커맨드가 실행 중이던 중간 상태(`Saving`)를 무조건 그대로 복원하지 마세요. 비즈니스적으로 가장 안전한 상태를 먼저 구성하고, 사용자 또는 검증된 결과를 통해 다음 커맨드가 실행되도록 유도합니다.

Draft 예제의 경우:
- 저장 완료된 데이터 -> `Saved`
- 미저장 편집 중이던 데이터 -> `Editing`
- 저장 성공 여부가 불확실한 경우 -> 오류 안내 문구와 함께 `Editing` 복원

초기 상태 구성 시에는 `onEnter`가 실행되지 않으므로, `Saving` 상태로 복원하더라도 자동으로 재저장이 시작되지 않습니다. 이는 의도된 동작입니다: 부작용이 있는 작업은 복구 규칙이 정의된 후 명시적인 이벤트를 통해서만 재시작되어야 합니다.

---

## 9. 머신 코드, 그래프, 테스트를 함께 읽기

머신이 복잡해지면 `.mmd` 다이어그램을 생성하세요. 전체 흐름의 위상(Topology)은 다이어그램에서, 세부 데이터 업데이트와 조건 규칙은 머신 코드에서, 엣지 케이스 및 이벤트 무시/거부 동작은 테스트 코드에서 확인합니다.

다음 가이드:
- [모델링 규칙 (Modeling Rules)](modeling-rules.md)
- [테스트 가이드 (Testing Guide)](testing-guide.md)
- [복원과 UI 정책 (Restoration & UI Policy)](restoration-command-ui-policy.md)
- [그래프 생성 (Graph Generation)](graph-generation.md)
