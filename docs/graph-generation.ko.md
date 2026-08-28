# 그래프 자동 생성 (Graph Generation)

Afsm은 실행 가능한 상태 머신의 위상(Topology) 정의로부터 Mermaid 상태 전이도 다이어그램을 자동으로 생성합니다.

---

## 생성되는 다이어그램에 포함되는 요소

- 초기 Phase
- 모든 Phase 노드
- 외부 전이(Transition) 화살표
- 이름 있는 `case` 조건 가드
- 방출되는 Command 라벨
- `onEnter` / `onExit` 진입/이탈 커맨드 노트
- Phase 소유 비동기 작업의 시작/취소 노트

---

## 1. 머신에 어노테이션 지정

```kotlin
@AfsmGraph(
    id = "Checkout",
    fileName = "CheckoutStateMachine.mmd",
)
internal val checkoutMachine:
    AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand> =
    afsmMachine(initialPhase = CheckoutPhase.Idle) {
        // phases and rules
    }
```

---

## 2. KSP 및 Gradle 플러그인 설정

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0"
}
```

KSP 플러그인을 Afsm 그래프 플러그인보다 먼저 적용하세요. 그래프 플러그인은
자신과 버전이 맞는 `afsm-graph-ksp` processor 의존성을 기본으로 추가합니다.

로컬 프로젝트 의존성이나 별도 processor를 사용한다면 기본 추가를 끄고 KSP를
직접 설정합니다.

```kotlin
afsmGraph {
    addProcessorDependency.set(false)
}

dependencies {
    ksp(project(":afsm-graph-ksp"))
}
```

---

## 3. 다이어그램 생성

```bash
./gradlew :sample-shop:generateAfsmMmd
```

산출물:
```text
sample-shop/build/generated/afsm/mmd/
├── AuthStateMachine.mmd
├── CheckoutStateMachine.mmd
└── ProductEditorStateMachine.mmd
```

---

## 4. 커밋된 Baseline과 자동 검증

생성된 다이어그램을 git에 커밋해 두고, 매 빌드마다 머신 코드와 다이어그램이 일치하는지 자동으로 검증합니다.

```bash
./gradlew :sample-shop:updateAfsmMmd   # 생성된 다이어그램을 커밋용 디렉터리로 복사
./gradlew :sample-shop:verifyAfsmMmd   # 커밋된 다이어그램이 낡았으면 빌드 실패
```

Baseline 디렉터리가 존재하면 `verifyAfsmMmd`가 `./gradlew check`에 자동으로 연결되므로, 머신 코드를 수정하고 다이어그램을 갱신하지 않으면 CI 빌드가 diff와 함께 실패합니다.
