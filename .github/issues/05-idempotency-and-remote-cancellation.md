# [RFC] 비동기 신뢰성: 멱등성 키(Idempotency Key) 및 AbortSignal 어댑터를 통한 원격 작업 취소 보장

**Labels**: `enhancement`, `runtime`, `networking`, `rfc`

## 1. 배경 및 문제점 (Problem Statement)
- 현재 Afsm은 `invoke(key)`를 통해 특정 Phase를 벗어날 때 로컬 코루틴 잡을 협력적으로 취소해 줍니다.
- 그러나 이는 **클라이언트 내부의 취소일 뿐**이며, 이미 백엔드로 전송된 네트워크 요청(예: 결제 승인, 대용량 파일 업로드 등)이 실제로 취소되거나 중복 실행을 막아주는 것은 아닙니다.
- 네트워크 계층과의 연결 고리가 없어 사용자가 중복 결제/재시도 방지(Idempotency) 및 원격 취소를 수동으로 구현해야 합니다.

## 2. 제안하는 해결 방안 (Proposed Solution)
1. **Idempotency Key (멱등성 키) 및 Request-ID 자동 주입**:
   - `Command` 생성 시 고유한 `requestId` / `idempotencyKey`를 자동 발급하여, 네트워크 재시도 시 동일 키를 재전송할 수 있도록 표준화.
2. **Ktor / OkHttp / Retrofit 연동 AbortSignal 미들웨어**:
   - `AfsmCommandHandler`에 네트워크 취소 인터셉터를 결합하여, Phase를 벗어나 로컬 코루틴이 취소되는 순간 진행 중이던 HTTP 연결(`Call.cancel()` / `HttpClient` 취소)을 즉시 종료하는 네트워크 어댑터 제공.

## 3. 예상 API 디자인 (Design Draft)

```kotlin
// 멱등성과 네트워크 취소가 보장되는 Safe Command Handler
val host = afsmHost(
    machine = paymentMachine,
    commandHandler = safeHttpCommandHandler(okHttpClient) { command, send, abortSignal ->
        when (command) {
            is PaymentCommand.Execute -> {
                // abortSignal이 OkHttp의 Call.cancel()과 자동 바인딩됨
                val response = apiService.pay(
                    idempotencyKey = command.idempotencyKey,
                    abortSignal = abortSignal
                )
                send(PaymentEvent.Success(response))
            }
        }
    }
)
```

## 4. 기대 효과 (Expected Benefits)
- Phase 이탈 시 실제 백엔드 소켓/커넥션이 즉시 해제되어 불필요한 데이터 및 배터리 소모 방지.
- 네트워크 불안정으로 인한 재시도 시 백엔드 중복 처리(중복 결제 등)를 시스템 레벨에서 원천 차단.

## 5. 완료 기준 (Acceptance Criteria)
- [ ] `afsm-runtime`에 `AfsmAbortSignal` 및 `IdempotencyKey` 기본 인터페이스 추가.
- [ ] OkHttp / Ktor 네트워크 클라이언트 연동 헬퍼 모듈 또는 가이드라인 작성.
- [ ] 결제/업로드 시나리오에 대한 취소 및 재시도 통합 테스트 작성.
