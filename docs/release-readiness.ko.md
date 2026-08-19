# Afsm 릴리스 준비 상태 (Release Readiness)

Afsm은 Maven Central에 `0.1.0`으로 공개된 pre-1.0 베타 버전입니다.

---

## 1. 로컬 릴리스 검증 게이트

```bash
./scripts/verify-release-local.sh --no-daemon
```

이 검증 스크립트는 다음 항목들을 종합적으로 검증합니다:
- core, runtime, test, ViewModel, KSP, graph 플러그인 단위 테스트
- sample 빌드, 기능 테스트 및 `.mmd` 생성
- 바이너리 API 덤프 일치 여부 (`apiCheck`)
- Maven Local 발행
- 발행된 아티팩트를 소비하는 독립된 Android 앱(`consumer-smoke`) 빌드
- Draft 퀵스타트 상태 전이 및 ViewModel 연동 동작
- 커맨드 실패 진단 및 프라이버시 기본값
- 외부 소비 환경에서의 Phase 소유 비동기 작업 취소

---

## 2. 배포 아티팩트

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0")
implementation("io.github.afsm:afsm-runtime:0.1.0")
implementation("io.github.afsm:afsm-viewmodel:0.1.0")
testImplementation("io.github.afsm:afsm-test:0.1.0")
```

그래프 도구:
```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0"
}
```

---

## 3. 호환성 기준 (Compatibility Baseline)

| 항목 | 버전 |
|---|---|
| JDK | 17 |
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.10.1 |
| KSP | 2.0.21-1.0.28 |
| compileSdk / targetSdk | 36 |
| minSdk | 23 |
