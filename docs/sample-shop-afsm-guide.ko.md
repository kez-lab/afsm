# Sample Shop 가이드 (Sample Shop Guide)

`sample-shop`은 실무 Android 앱 환경에서의 Afsm 활용 패턴을 보여주는 참조 애플리케이션입니다. 모든 화면을 억지로 상태 머신으로 만들지 않고, 복잡도에 따라 적절한 아키텍처를 선택하는 기준을 제시합니다.

---

## 1. 화면별 아키텍처 분기

| 화면 기능 | 적용 아키텍처 | 이유 |
|---|---|---|
| **Auth** | Afsm | 조건부 폼 제출 및 비동기 로그인/회원가입 처리 |
| **Checkout** | Afsm | 네비게이션 인자 주입, 결제 재시도, stale 응답 방어, 상태 복원 |
| **Product Editor** | Afsm | 긴 워크플로우, Phase 소유의 협력적 업로드 취소 (`invoke`) |
| **Catalog / 단순 화면** | 일반 ViewModel + Flow | 단순 조회/목록 화면은 일반 패턴이 더 간결함 |

---

## 2. 파일별 역할 분담

각 Afsm 기능 모듈은 다음과 같은 표준 파일 구조를 따릅니다:

```text
FeatureFlow.kt          State, Event, Command 및 UI 렌더 모델 정의
FeatureStateMachine.kt  순수 비즈니스 흐름 전이 규칙 및 그래프 위상 정의
FeatureViewModel.kt     Android 의존성 관리, Command 실행 및 결과 전송
FeatureScreen.kt        Compose UI 상태 수집 및 사용자 액션 콜백 연결
FeatureStateMachineTest.kt 순수 JVM 상태 머신 단위 테스트
```

UI는 동사형 ViewModel 메서드를 호출하며, 범용 `onEvent(Event)` MVI 함수를 노출하지 않습니다.

---

## 3. 다이어그램 생성 및 검토 순서

```bash
./gradlew :sample-shop:generateAfsmMmd
```

새로운 기능을 검토할 때는 다음 순서를 따릅니다:
1. **그래프**: 전체 흐름 위상 조망
2. **머신 코드**: 세부 전이 및 커맨드 방출 규칙
3. **테스트**: 엣지 케이스 및 오류 방어 증명
4. **ViewModel**: Repository 연동 및 복원 정책
5. **Screen**: Compose UI 연동
