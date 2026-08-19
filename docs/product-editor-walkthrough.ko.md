# Product Editor 화면 가이드 (Product Editor Walkthrough)

Product Editor는 복잡한 다단계 워크플로우와 Phase 소유의 취소 가능한 비동기 작업(`invoke`)을 다루는 심화 예제입니다.

소스 코드 위치:
- `feature/editor/ProductEditorFlow.kt`
- `feature/editor/ProductEditorStateMachine.kt`
- `feature/editor/ProductEditorViewModel.kt`
- `feature/editor/ProductEditorScreen.kt`
- `ProductEditorStateMachineTest.kt`
- `ProductEditorViewModelTest.kt`

---

## 1. 전체 흐름 (Flow)

```text
EditingDraft -> SavingDraft -> DraftSaved
     |
     +-> ImageUploadInProgress -> ReviewSubmissionInProgress
                                      |             |
                                   Rejected      Approved
                                      |             |
                                      +--retry      +-> PublishInProgress -> Published
```

---

## 2. Phase 소유의 비동기 작업 (`invoke`)

이미지 업로드는 `ImageUploadInProgress`에 진입할 때 시작되고, 사용자가 취소를 눌러 해당 Phase를 벗어날 때 **협력적으로 자동 취소**됩니다:

```kotlin
phase(ProductEditorPhase.ImageUploadInProgress) {
    onEnter {
        invoke(
            key = productEditorImageUploadInvocationKey,
            label = "StartImageUpload",
        ) {
            ProductEditorCommand.StartImageUpload(data.draft)
        }
    }

    on<ProductEditorEvent.CancelUploadClicked> {
        transitionTo(ProductEditorPhase.EditingDraft)
    }
}
```

### `invoke`를 사용하는 기준
- 작업이 특정 Phase에 진입했기 때문에 시작됨.
- 작업이 해당 Phase에 종속되어 있어 Phase를 벗어나면 즉시 취소되어야 함.
- 임의의 가짜 오류 이벤트 없이 취소 처리가 가능함.

일반적인 순차 Repository 호출(임시 저장, 심사 제출, 게시)에는 일반 `command`를 사용하고, 사용자가 취소 가능한 로컬 작업(업로드 등)에만 `invoke`를 적용합니다.

---

## 3. UI 렌더 상태 (Render State)

Compose UI가 `ReviewSubmissionInProgress(uploadToken)`과 같은 머신 내부 페이로드에 직접 의존하지 않도록, `ProductEditorRenderState`로 변환하여 UI에 필요한 상태만 제공합니다.

---

## 4. Done 버튼이 머신을 거치지 않는 이유

`Published(productId, title)` 상태가 이미 도메인의 완료 진실을 담고 있습니다. 화면 상단의 "완료(Done)" 버튼 클릭은 단순히 에디터 화면을 닫는 순수 UI 동작이므로, 불필요하게 머신에 이벤트를 보내지 않고 Route의 `onDone` 콜백을 직접 호출합니다.
