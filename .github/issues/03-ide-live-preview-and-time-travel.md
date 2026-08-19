# [RFC] 개발자 툴링: Android Studio Live Graph Preview 및 타임트래블(Time-Travel) 디버깅 트레이서

**Labels**: `tooling`, `dx`, `debugging`, `rfc`

## 1. 배경 및 문제점 (Problem Statement)
- Phase-local DSL 특성상 코드가 `phase(A)`, `phase(B)`로 쪼개져 있어 코드만 읽었을 때 전체 화면 흐름을 한눈에 조망하기 어렵습니다.
- 또한 `Event ➔ Reducer ➔ Command ➔ Result`로 이어지는 4~5단계의 간접 호출로 인해 런타임 버그 디버깅 시 콜스택 추적이 번거롭습니다.

## 2. 제안하는 해결 방안 (Proposed Solution)
1. **Android Studio / IntelliJ IDE 플러그인 (Live Graph Preview)**:
   - Compose Preview 탭처럼 코드를 작성하는 즉시 우측 툴 윈도우에 Mermaid 상태 전이도를 실시간 인터랙티브 그래프로 렌더링.
   - 그래프의 노드/엣지 클릭 시 해당 DSL 소스 코드로 양방향 점프(Bi-directional navigation).
2. **타임트래블 디버깅 트레이서 (Time-Travel Debug Tracer)**:
   - `AfsmHost`에 장착 가능한 디버그 인터셉터 및 Android Studio App Inspection / Flipper 연동 탭 제공.
   - 발생한 모든 이벤트, 상태 변경, 명령 실행 이력을 타임라인으로 기록하고, 특정 시점으로 상태를 롤백/재현(Replay)할 수 있는 디버깅 뷰어 제공.

## 3. 예상 UI 및 동작 시나리오

```text
[ Android Studio Editor ]            │ [ Afsm Live Graph Preview ]
1: phase(CheckoutPhase.Idle) {       │
2:   on<PayClicked> {                │   (Idle) ──[PayClicked]──► (Paying)
3:     transitionTo(Paying)          │                              │
4:   }                               │                              ▼ [Success]
5: }                                 │                         (Completed)
                                     │
-------------------------------------------------------------------------
[ Afsm Debug Timeline Inspector ]
[10:24:01.102] 🟢 Event: PayClicked (amount=50000)
[10:24:01.104] 🔄 State: Idle ──► Paying
[10:24:01.105] ⚡ Command: ExecutePayment
[10:24:02.340] 🟢 Event: PaymentSuccess (receiptId=RC_1234)
[10:24:02.342] 🔄 State: Paying ──► Completed
```

## 4. 기대 효과 (Expected Benefits)
- 코드 작성과 동시에 다이어그램을 보면서 아키텍처를 설계할 수 있어 가독성 극대화.
- 복잡한 비동기 레이스 컨디션 및 전이 버그를 타임라인 로그 분석으로 수초 내에 원인 규명 가능.

## 5. 완료 기준 (Acceptance Criteria)
- [ ] `afsm-runtime`에 `AfsmTraceInterceptor` 및 이벤트 히스토리 로거 API 구현.
- [ ] IntelliJ / Android Studio 플러그인 프로토타입 작성 (WebView 기반 Mermaid 렌더링).
- [ ] 디버그 빌드 전용 타임트래블 트레이스 UI 또는 Logcat 포맷터 제공.
