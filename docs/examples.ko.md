# 예제 학습 가이드 (Examples)

## 예제 선택 가이드

| 예제 | 주요 학습 포인트 | 잘못된 추론 방지 |
|---|---|---|
| **Draft 퀵스타트** | 최소 상태 머신, 커맨드 핸들러, 동사형 ViewModel API | 모든 단순 폼에 머신이 필요하다는 의미가 아님 |
| **Auth** | 유효성 검증 분기 및 지속되는 완료 상태 | 앱 전체에 MVI를 강제해야 한다는 의미가 아님 |
| **Checkout** | 동적 초기 상태, 재시도, stale 결과 방어, 상태 복원 | 모든 화면 이동 콜백이 머신 안에 있어야 한다는 의미가 아님 |
| **Product Editor** | 긴 워크플로우, UI 렌더 상태, Phase 소유의 취소 가능한 작업 (`invoke`) | 첫 기능부터 `invoke`가 필요하다는 의미가 아님 |

---

## 추천 열람 순서

1. **그래프 (`.mmd`)**: 전체 흐름 위상 파악
2. **머신 코드 (`*StateMachine.kt`)**: 정확한 전이 규칙 및 커맨드 방출
3. **단위 테스트 (`*StateMachineTest.kt`)**: 엣지 케이스 및 오류 방어 증명
4. **ViewModel (`*ViewModel.kt`)**: 실제 작업 실행 및 결과 반환
5. **Screen (`*Screen.kt`)**: Compose UI 연동

가이드 목록:
- [Draft 튜토리얼 (Getting Started)](getting-started.md)
- [Auth 가이드 (Auth Walkthrough)](auth-walkthrough.md)
- [Checkout 가이드 (Checkout Walkthrough)](checkout-walkthrough.md)
- [Product Editor 가이드 (Product Editor Walkthrough)](product-editor-walkthrough.md)
