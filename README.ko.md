# Afsm

![Status](https://img.shields.io/badge/status-internal%20beta-orange)
![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7F52FF?logo=kotlin)
![Android](https://img.shields.io/badge/android-AGP%208.10.1-3DDC84?logo=android)
![Distribution](https://img.shields.io/badge/distribution-Maven%20Local-lightgrey)

[English](README.md) | **한국어**

Afsm은 복잡한 `ViewModel` 곳곳에 암묵적으로 흩어진 비즈니스 흐름의
상태 변경을 명시적인 `Phase`와 `Event` 전이 규칙으로 바꾸는 Android 중심
유한 상태 머신 도구입니다. 개발자는 하나의 실행 가능한 머신 정의에서
어떤 이벤트가 유효한지, `Phase`와 `Data`가 어떻게 바뀌는지, 어떤
호스트 실행 `Command`와 선택적 UI `Effect`가 뒤따르는지를 읽고 테스트할
수 있습니다. `ViewModel`은 계속 Android 수명주기와 UI 통합 어댑터 역할을
담당합니다.

의미 있는 단계, 재시도, 비동기 결과, 유효하지 않은 전이, 여러 단계의
동작이 있는 화면에 Afsm을 사용하세요. 일반 `ViewModel + StateFlow`가 더
명확한 단순 상품 목록, 상세 화면, 좋아요, 리뷰 목록, 기본적인
loading/content/error 화면에 억지로 적용하지 마세요.

## Afsm을 만들기 시작한 이유

복잡한 Android 화면은 어느 순간 비즈니스 흐름이 코드 전체에 존재하지만,
정작 한눈에 읽을 수 있는 곳은 없는 상태가 되곤 합니다. 각각의
`state.copy(...)`, 이벤트 핸들러, 코루틴 실행, 저장소 콜백은 합리적으로
보이지만 화면 전체를 이해하려면 `ViewModel`, UI collector, 비동기 결과
핸들러와 테스트를 오가며 규칙을 다시 조립해야 합니다. 이 문제를 해결하고
싶어서 Afsm을 만들기 시작했습니다.

이 상태에서는 단순한 질문도 답하기 어려워집니다. 화면은 지금 어떤 단계에
있는가? 현재 어떤 이벤트가 유효한가? 외부 작업은 무엇이 시작하는가?
실패와 재시도는 어떻게 처리되는가? 오래된 결과가 새로운 상태를 덮어쓸 수
있는가? 어떤 완료 정보는 지속 상태이고 어떤 출력은 일회성 UI 동작인가?

Afsm은 이 답들을 한곳에서 읽을 수 있고 실제로 실행되게 만들려는
시도에서 출발했습니다. Android `ViewModel`을 대체하거나 Kotlin `copy()`를
감추거나 앱 전체 아키텍처를 강제하는 것이 목표가 아닙니다. 복잡한 화면의
비즈니스 흐름 규칙을 하나의 순수 Kotlin 머신으로 옮겨 runtime이 실행하고,
테스트가 검증하고, graph 도구가 시각화하게 만드는 것이 목표입니다.
`ViewModel`은 계속 lifecycle, `StateFlow`, repository, saved state와 UI
연결을 담당합니다. 단순한 화면은 기존 Android 상태 관리가 더 명확하다면
그 방식을 그대로 사용해야 합니다.

## 처음 시작하는 경로

첫 Afsm 화면을 만든다면 [docs/getting-started.md](docs/getting-started.md)부터
시작하세요.

이 README는 전체 구조를 빠르게 파악하기 위한 지도입니다. 첫 실제 Draft
파일은 [docs/getting-started.md](docs/getting-started.md)에서 복사하세요. 해당
가이드는 `consumer-smoke`에 그대로 반영되며 release gate에서 Maven Local에
게시된 artifact를 대상으로 컴파일됩니다.

짧게 요약하면 다음과 같습니다.

1. 먼저 phase를 그립니다.
2. 지속되는 화면 데이터는 모든 phase 생성자가 아니라 `Data`에 둡니다.
3. UI 입력과 repository 결과를 `Event`로 처리합니다.
4. `transitionTo(...)`로 phase를 이동합니다.
5. 일반적으로 `onEnter`의 `command(...)`에서 repository 작업을 시작합니다.
6. 순수 머신을 일반 JVM 전이 테스트로 검증합니다.
7. `ViewModel`에서 `afsmHost(...)`로 머신을 실행합니다.
8. command 실행과 결과 event를 검증하는 ViewModel 연결 테스트 하나를 추가합니다.

여기까지가 최소 첫 단계입니다. 그다음 Compose를
`collectAsStateWithLifecycle`로 연결하고, UI가 phase에서 동작을 추론하기
시작할 때만 render state를 추가하세요. effect, saved state, config, graph도
화면에 실제로 필요할 때만 추가하세요.

실제 예제를 고를 때는 [docs/examples.md](docs/examples.md)를 사용하세요.
운영 화면을 모델링하기 전에는
[docs/modeling-rules.md](docs/modeling-rules.md)를 읽으세요.

## 최소 머신

핵심 개념은 다음과 같습니다.

| 개념 | 의미 |
|---|---|
| `Phase` | `Editing`, `Saving` 같은 유한 상태 다이어그램의 노드 |
| `Data` | phase 사이에서 유지되는 확장 상태 데이터. `android.content.Context`가 아님 |
| `State` | Android가 관찰하는 snapshot. 일반적으로 `AfsmState<Phase, Data>` |
| `Event` | 사용자 입력 또는 command 결과 |
| `Command` | repository 호출이나 timer처럼 host가 실행할 작업 |
| `Effect` | 선택적인 일회성 UI 출력 |

host에서 실행할 작업을 전혀 내보내지 않는 머신은 `AfsmNoCommand`를
사용하세요. 일회성 UI 출력을 내보내지 않는 머신은 `AfsmNoEffect`를
사용하세요.

일상적인 선택 기준은 다음과 같습니다.

| 상황 | 사용할 것 |
|---|---|
| 비즈니스 단계가 바뀜 | `transitionTo(Phase.X)` |
| 같은 단계에서 form/error 데이터만 바뀜 | `updateData { ... }` |
| 이벤트에 이름 있는 여러 조건 분기가 있음 | `case(label, condition = ...) { ... }` |
| repository, database, timer, SDK 작업을 실행해야 함 | 주로 `onEnter` 안의 `command(label) { ... }` |
| 선택적인 navigation/snackbar/close 동작이 필요함 | `effect(label) { ... }` |
| 예상된 중복 이벤트나 오래된 이벤트를 무해하게 처리해야 함 | 제한적으로 `ignore(reason)` |

작은 머신부터 정의하세요. graph/KSP부터 시작하지 마세요. 아래 코드는
전체 형태를 이해하기 위한 예제입니다. 완전한 `DraftStateMachine.kt`와
`DraftViewModel.kt`를 복사할 때는
[docs/getting-started.md](docs/getting-started.md)를 사용하세요.

```kotlin
import afsm.core.AfsmDefaultMachine
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine

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

val draftStateMachine: AfsmDefaultMachine<
    DraftState,
    DraftEvent,
    DraftCommand,
    AfsmNoEffect,
    > = afsmMachine {
    initial(
        phase = DraftPhase.Editing,
        data = DraftData(),
    )

    phase(DraftPhase.Editing) {
        on<DraftEvent.TitleChanged> {
            updateData { data, event ->
                data.copy(
                    title = event.value,
                    errorMessage = null,
                )
            }
        }

        on<DraftEvent.SaveClicked> {
            case(
                label = "valid title",
                condition = { data.title.isNotBlank() },
            ) {
                transitionTo(DraftPhase.Saving)
            }

            case(
                label = "missing title",
                condition = { data.title.isBlank() },
            ) {
                updateData { copy(errorMessage = "Title is required.") }
            }
        }
    }

    phase(DraftPhase.Saving) {
        onEnter {
            command(label = "SaveDraft") {
                DraftCommand.SaveDraft(data.title)
            }
        }

        on<DraftEvent.DraftSaveCompleted> {
            transitionTo(DraftPhase.Saved)
        }

        on<DraftEvent.DraftSaveFailed> {
            updateData { data, event ->
                data.copy(errorMessage = event.message)
            }
            transitionTo(DraftPhase.Editing)
        }
    }

    phase(DraftPhase.Saved)
}
```

`AfsmDefaultMachine`은 이 정적인 Draft 흐름이 실제 기본 상태를 소유한다는
뜻입니다. navigation 인자나 복원된 데이터가 필요한 feature는 기본
`AfsmMachine`과 `afsmMachine(initialPhase = ...)`를 사용하며, 이 경우 host가
초기 상태를 명시적으로 전달해야 합니다.

`case(...)`는 조건부 대안에만 사용합니다. `condition`은 필수이며 선언
순서대로 검사됩니다. 어떤 case도 일치하지 않으면 해당 이벤트는 현재
phase에서 유효하지 않습니다. 하나의 `on<Event>` 안에 직접 쓴 `updateData`,
`command`, `effect`, `transitionTo`는 하나의 무조건 branch로 합성됩니다.
같은 이벤트 handler에서 direct action과 조건부 case를 섞을 수 없습니다.

phase가 바뀌는 전이는 다음 순서로 실행됩니다.

```text
onExit -> branch actions -> target phase factory -> onEnter
```

초기 상태 생성은 `onEnter`를 실행하지 않습니다. 시작 작업은
`ScreenEntered` 같은 명시적 이벤트로 시작하세요.

## ViewModel

```kotlin
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

interface DraftRepository {
    suspend fun save(title: String): Result<Unit>
}

class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftStateMachine,
        commandHandler = { command: DraftCommand, dispatch ->
            when (command) {
                is DraftCommand.SaveDraft -> repository.save(command.title).fold(
                    onSuccess = {
                        dispatch(DraftEvent.DraftSaveCompleted)
                    },
                    onFailure = { error ->
                        dispatch(
                            DraftEvent.DraftSaveFailed(
                                error.message ?: "Draft save failed.",
                            ),
                        )
                    },
                )
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun onEvent(event: DraftEvent) {
        host.dispatch(event)
    }
}
```

시작 상태가 navigation 인자, deep link, repository 복원 또는
`SavedStateHandle`에서 온다면 같은 머신 언어를 유지하면서 초기 상태를
명시적으로 전달하세요.

```kotlin
private val host = afsmHost(
    machine = draftStateMachine,
    initialState = restoredDraftState,
    commandHandler = draftCommandHandler,
)
```

이 패턴을 `SavedStateHandle`과 함께 테스트한 예제는
[docs/getting-started.md](docs/getting-started.md#add-initial-state-from-savedstatehandle-later)를
참고하세요.

## 설치

저장소 내부에서 개발할 때:

```kotlin
dependencies {
    implementation(project(":afsm-core"))
    implementation(project(":afsm-runtime"))
    implementation(project(":afsm-viewmodel"))
    implementation(project(":afsm-compose")) // optional
    ksp(project(":afsm-graph-ksp")) // optional graph registry
}
```

Maven Local로 평가할 때:

```bash
./gradlew publishToMavenLocal
./gradlew -p afsm-graph-gradle-plugin publishToMavenLocal # only needed for optional graph plugin
```

```kotlin
repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")

    testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
    testImplementation("junit:junit:4.13.2")
}
```

선택적인 Compose 및 graph 도구:

Maven Local graph plugin을 해석하려면 `settings.gradle.kts`의
`pluginManagement.repositories`에 `mavenLocal()`을 추가하세요.

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
}
```

graph plugin은 기본적으로 앱 모듈에 `afsm-graph-ksp`를 추가하고
`generateAfsmMmd` task를 등록합니다.

전체 로컬 consumer 검사는 plugin을 게시하고 외부 Android build에서 graph
생성을 검증합니다. 또한 [docs/getting-started.md](docs/getting-started.md)의
Draft quickstart 머신과 ViewModel을 컴파일하므로 첫 사용 예제가 게시된 Maven
Local artifact와 달라질 수 없습니다.

```bash
./scripts/verify-consumer-smoke.sh --warning-mode all
```

Android consumer는 AndroidX를 활성화해야 합니다.

```properties
android.useAndroidX=true
```

`io.github.afsm`은 현재 pre-release group id입니다. 최종 Maven Central
좌표는 아직 제품 결정을 거쳐야 합니다.

## 현재 상태

Afsm은 private internal beta 상태입니다. 로컬 release gate는 통과하고,
Maven Local 평가가 가능하며, sample-shop은 Auth, Checkout, ProductEditor
흐름을 보여줍니다. Checkout은 자동 결제 재시도 없이 최소한의 stable/pending
`SavedStateHandle` 복원도 보여줍니다. license, 최종 좌표, SCM metadata,
signing, release ownership이 결정될 때까지 안정 OSS/Maven Central 게시는
의도적으로 보류합니다. 내부 pilot 규칙은
[docs/release-readiness.md](docs/release-readiness.md)에 있습니다. Afsm을 처음
접하는 사람을 위한 Checkout 이해도 과제는
[docs/checkout-first-use-participant-task.md](docs/checkout-first-use-participant-task.md)에
있습니다.

의도적으로 graph를 만들 수 없는 사용자 정의 `AfsmReducer`를 사용한다면
호출부에서 reducer임을 명확히 표현하세요.

```kotlin
private val host = afsmHost(
    reducer = LegacyCheckoutReducer(),
    initialState = restoredCheckoutState,
    commandHandler = checkoutCommandHandler,
)
```

## 테스트 우선

상태 머신 테스트는 일반 JVM 테스트입니다.

```kotlin
import afsm.test.assertCommands
import afsm.test.assertPhase
import afsm.test.assertTransitioned
import org.junit.Test

@Test
fun `SaveClicked enters Saving and emits SaveDraft`() {
    val result = draftStateMachine.transition(
        state = DraftState(
            phase = DraftPhase.Editing,
            data = DraftData(title = "Plan"),
        ),
        event = DraftEvent.SaveClicked,
    )

    result
        .assertTransitioned()
        .assertPhase(DraftPhase.Saving)
        .assertCommands(DraftCommand.SaveDraft("Plan"))
}
```

좋은 feature 테스트는 보통 다음을 다룹니다.

- 유효한 phase 전이
- 유효하지 않은 전이
- command 출력
- command 실패 결과 경로
- effect 출력
- 오래된 command 결과 처리

머신 테스트가 통과한 다음 `onEvent(event)`를 호출하고 repository command
실행을 검증하며 결과 `state.value`를 관찰하는 ViewModel 연결 테스트 하나를
추가하세요. 나머지 전이 matrix는 순수 머신 테스트에 유지하세요.

[docs/testing-guide.md#viewmodel-tests](docs/testing-guide.md#viewmodel-tests)와
실제로 실행되는
[`consumer-smoke` Draft ViewModel test](consumer-smoke/app/src/test/kotlin/afsm/consumer/smoke/DraftViewModelTest.kt)를
참고하세요.

## 긴 Command의 안전성

Command는 순수 머신 바깥에서 실행되며 늦게 끝날 수 있습니다. 늦은 결과를
명시적으로 모델링하세요.

권장 패턴:

- command에 `requestId` 또는 입력 snapshot을 포함합니다.
- 성공/실패 event에 같은 id를 반환합니다.
- 현재 활성 요청과 id가 일치할 때만 결과를 받아들입니다.
- 이전 요청에 속한 오래된 결과는 `ignore`합니다.

sample checkout 흐름은 mock payment 재시도에 이 정책을 사용합니다.

## Compose Effect

UI effect를 내보내는 머신은 route 수준에서 effect를 collect하세요. 위의
최소 Draft 머신은 `AfsmNoEffect`를 사용하므로 이 패턴은 feature가 실제
effect 타입을 정의할 때만 적용됩니다.

`AfsmNoEffect`에서 실제 feature effect 타입으로 처음 이동하는 방법은
[docs/getting-started.md](docs/getting-started.md#add-the-first-effect-later)를
참고하세요.

effect가 있는 feature에만 Compose helper를 추가하세요.

```kotlin
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
```

route 수준에서 effect를 collect하세요. Navigation, snackbar 표시, focus,
scroll, animation 상태는 비즈니스 흐름의 일부가 아니라면 UI에 유지해야
합니다.

```kotlin
import afsm.compose.CollectAfsmEffects

@Composable
fun EditorRoute(
    viewModel: EditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectAfsmEffects(viewModel.effects) { effect ->
        when (effect) {
            ProductEditorEffect.CloseEditor -> onDone()
        }
    }

    EditorScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
```

Effect는 best-effort 일회성 출력입니다. recreation 이후에도 남아야 하는
정보는 state와 acknowledgement event로 표현하세요.

## 선택적 MMD Graph

머신이 동작한 다음 graph 생성을 선택적으로 추가하세요.

현재 pre-release graph export는 KSP discovery와 Afsm graph Gradle plugin을
사용합니다. graph를 만들 머신에 annotation을 추가하고 task 하나를
실행하면 plugin이 작은 unit-test writer를 내부적으로 생성하므로 앱 팀이
export test를 직접 유지할 필요가 없습니다. 전체 설정은
[docs/graph-generation.md](docs/graph-generation.md)를 참고하세요.

```kotlin
@AfsmGraph(
    id = "Draft",
    fileName = "DraftStateMachine.mmd",
)
val draftStateMachine: AfsmDefaultMachine<
    DraftState,
    DraftEvent,
    DraftCommand,
    AfsmNoEffect,
    > = afsmMachine {
    // same executable machine body
}
```

생성된 다이어그램에 출력을 표시하고 싶을 때만 `command(...)` 또는
`effect(...)`에 `label = ...`을 전달하세요. label과 runtime 출력이 같은
문장에 있으므로 다이어그램과 실제 동작이 달라질 가능성이 줄어듭니다.

plugin 적용:

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}

afsmGraph {
    variant.set("debug") // default
    outputDir.set(layout.buildDirectory.dir("generated/afsm/mmd")) // default
    mmdOptions.set("Flow") // default; use "Full" for every internal edge
}
```

출력 예제:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

`Flow`는 text 변경 같은 이름 없는 일반 internal self-loop를 숨기지만 이름
있는 condition, command, effect edge는 유지합니다. 모든 internal edge가
필요할 때는 `Full`을 사용하세요.

```bash
./gradlew :sample-shop:generateAfsmMmd -PafsmMmdOptions=Full
```

## Runtime 정책

- `dispatch(event)`는 suspend하지 않으며 FIFO event 처리로 직렬화됩니다.
- `tryDispatch(event)`는 event queue가 닫혔거나 가득 찼을 때 `false`를 반환합니다.
- Event는 기본값 `64`인 bounded queue를 사용합니다.
- Command 결과 event도 같은 bounded event queue를 사용합니다. queue가 가득
  차면 command 처리를 suspend하는 대신 host가
  `AfsmEventQueueOverflowException`을 던집니다. host가 이미 닫혔다면 화면
  lifecycle이 끝났으므로 결과 event를 버리고 log를 남깁니다.
- Command는 이후 event reduction을 막지 않으면서 순차적으로 실행됩니다.
- Command도 기본값 `64`인 bounded queue를 사용합니다. 가득 차면 event
  processor를 suspend하는 대신 host가 `AfsmCommandQueueOverflowException`을
  던집니다.
- 한 phase가 소유하는 긴 작업은
  `onEnter { invoke(key, label) { command } }`를 사용할 수 있습니다. 추적되는
  child job에서 실행되며 phase 이탈 또는 host 종료 시 자동으로 취소됩니다.
- Invocation 취소는 로컬의 cooperative 취소입니다. remote, callback, SDK,
  blocking 작업이 coroutine보다 오래 살아남을 수 있다면 request id와
  idempotency를 유지하세요.
- Command 결과는 typed event로 host에 다시 dispatch해야 합니다.
- Domain 실패는 exception을 던지는 대신 domain event가 되어야 합니다.
- 예상하지 못한 command exception은 `AfsmCommandFailurePolicy`를 사용합니다.
- 유효하지 않은 전이는 순수 머신 테스트에서 검증할 수 있습니다. host의
  invalid transition은 개발 중 흐름 버그가 보이도록 기본적으로 throw합니다.
- Runtime diagnostic은 기본적으로 types-only입니다. code, decision category,
  고정 message, Kotlin type 이름과 Afsm 소유 metadata는 사용할 수 있지만 raw
  state/event/command/reason/throwable 값은 버립니다.
- `AfsmDiagnosticDataPolicy.IncludeValues`는 개인정보 노출 위험을 감수하는
  명시적 opt-in입니다. 앱이 직접 소유한 redaction boundary 없이
  `diagnostic.values`를 운영 log나 crash 도구로 보내지 마세요.
- `CancellationException`은 항상 다시 던집니다.
- Effect는 기본적으로 replay가 없는 best-effort 일회성 출력입니다.

## 모듈

| 모듈 | 용도 | Android 의존성 |
|---|---|---|
| `afsm-core` | 순수 Kotlin 전이 타입, reducer 계약, 실행 가능한 머신 DSL, invocation 출력, graph metadata | 없음 |
| `afsm-runtime` | Coroutine host, 직렬 dispatch loop, 순차 command, phase 소유 invocation job, effect 전달 | 없음 |
| `afsm-test` | Afsm 전이 동작을 위한 Kotlin 테스트 assertion helper | 없음 |
| `afsm-viewmodel` | `viewModelScope`를 사용하는 얇은 `ViewModel.afsmHost(...)` adapter | 있음 |
| `afsm-compose` | Lifecycle-aware Compose effect collection helper | 있음 |
| `afsm-graph-ksp` | `@AfsmGraph` 머신을 찾는 KSP discovery | Android runtime 의존성 없음 |
| `sample-shop` | 실제 사용을 검증하는 Compose + Room sample app | 있음 |
| `consumer-smoke` | Maven Local에서 Afsm을 가져오는 독립 Android consumer build | 있음 |

## 검증

```bash
./scripts/verify-release-local.sh --warning-mode all
```

`consumer-smoke`는 의도적으로 별도 Gradle build입니다. Android 프로젝트가
project module shortcut 없이 Maven Local artifact를 가져올 수 있는지
검증합니다.

[docs/examples.md](docs/examples.md),
[docs/afsm-public-api.md](docs/afsm-public-api.md),
[docs/restoration-effect-command-policy.md](docs/restoration-effect-command-policy.md),
[docs/graph-generation.md](docs/graph-generation.md),
[docs/auth-walkthrough.md](docs/auth-walkthrough.md),
[docs/checkout-walkthrough.md](docs/checkout-walkthrough.md),
[docs/product-editor-walkthrough.md](docs/product-editor-walkthrough.md),
[docs/sample-shop-afsm-guide.md](docs/sample-shop-afsm-guide.md),
[docs/release-readiness.md](docs/release-readiness.md),
[CHANGELOG.md](CHANGELOG.md), [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.
