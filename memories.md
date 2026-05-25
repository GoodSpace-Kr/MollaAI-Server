# memories.md

이 파일은 MollaAI Server의 중요한 변경사항을 시간순으로 기록하는 작업 메모리이다. 에이전트와 개발자는 작업 전후로 이 파일을 확인하고, 운영이나 제품 동작에 영향을 주는 변경사항을 반드시 남긴다.

실제 secret 값, 개인키, DB 비밀번호, API key, 인증번호, 토큰, 통화 전문 원문은 기록하지 않는다.

## 기록 대상

다음 변경은 반드시 기록한다.

- 환경변수 추가, 삭제, 이름 변경, 의미 변경
- API 엔드포인트 추가, 삭제, 경로 변경, 요청/응답 형식 변경
- 인증, 권한, 보안 정책 변경
- 핵심 비즈니스 로직 변경
- 통화 세션, 리포트 생성, 사용자 메모리, 구독/사용량 계산 변경
- 외부 연동 변경: OpenAI, Naver SENS, Qdrant, MySQL, AWS, GitHub Actions, Docker
- 배포 방식, 포트, 보안그룹, Nginx, Docker 이미지 변경
- 데이터베이스 스키마 또는 마이그레이션 변경
- 운영 장애 대응, 임시 우회, 롤백, hotfix

## 기록 형식

새 기록은 항상 가장 위에 추가한다.

```markdown
## YYYY-MM-DD - 제목

- 구분: 환경변수 | 엔드포인트 | 메인 로직 | 운영 | 배포 | 보안 | 문서 | 기타
- 변경: 무엇이 바뀌었는지
- 영향: 사용자, API, 운영, 데이터에 미치는 영향
- 확인: 실행한 검증 또는 확인한 파일
- 관련 파일: `path/to/file`
- 비고: 남은 작업이나 주의사항
```

## 2026-05-25 - OpenAI 리포트 프롬프트를 새 Report 스키마에 맞게 갱신

- 구분: 메인 로직, AI, test
- 변경: `OpenAiClient.generateReport`의 시스템 프롬프트와 예시 JSON을 새 `Report` 스키마에 맞게 수정했다. `levelPercentage`, `levelAnalysis`, `originSentence`, `keyExpression`를 요구하도록 바꾸고, `weakPoints`는 1~3개 태그형 문구로 제한했다. 함께 `ReportAudioEnricher`, `FeedbackReportViewMapper`, 관련 테스트도 새 필드명에 맞게 정리했다.
- 영향: OpenAI 리포트 생성 결과가 현재 `Report` 레코드 구조와 일치하게 파싱되며, core sentence 오디오 보강과 리포트 응답 매핑도 새 필드 기준으로 동작한다.
- 확인: `./gradlew test --tests com.molla.domain.worker.OpenAiClientGenerateReportTest --tests com.molla.domain.worker.OpenAiClientPromptTest --tests com.molla.domain.feedbackreport.ReportJsonTest --tests com.molla.domain.worker.ReportAudioEnricherTest --tests com.molla.domain.feedbackreport.FeedbackReportViewMapperTest --tests com.molla.domain.worker.CallSessionWorkerTest`
- 관련 파일: `src/main/java/com/molla/domain/worker/OpenAiClient.java`, `src/main/java/com/molla/domain/worker/ReportAudioEnricher.java`, `src/main/java/com/molla/domain/feedbackreport/FeedbackReportViewMapper.java`, `src/test/java/com/molla/domain/worker/OpenAiClientGenerateReportTest.java`, `src/test/java/com/molla/domain/worker/OpenAiClientPromptTest.java`, `src/test/java/com/molla/domain/feedbackreport/ReportJsonTest.java`, `src/test/java/com/molla/domain/worker/ReportAudioEnricherTest.java`, `src/test/java/com/molla/domain/feedbackreport/FeedbackReportViewMapperTest.java`, `src/test/java/com/molla/domain/worker/CallSessionWorkerTest.java`
- 비고: `Report.java` 자체는 이미 별도 변경되어 있었고, 이번 작업은 그 계약에 맞춰 프롬프트와 연동 코드만 따라갔다.

## 2026-05-25 - 데모 premium 기본 구독 한도를 300분으로 조정

- 구분: 메인 로직, 구독, 문서, test
- 변경: 신규 유저 자동 생성 시 붙는 데모 `premium` 기본 구독의 `dailyLimitMinutes`를 `Integer.MAX_VALUE`에서 `300`으로 변경했다. 구독 조회 Swagger 예시와 관련 테스트 기대값도 함께 수정했다.
- 영향: `/api/v1/subscriptions/me`와 `start` 세션 응답의 `dailyLimitMinutes`, `remainingMinutesToday`가 더 이상 최대 정수값이 아니라 300분 기준으로 내려간다.
- 확인: `SubscriptionServiceTest`, `CallSessionServiceTest`, `./gradlew test --tests com.molla.domain.subscription.SubscriptionServiceTest --tests com.molla.domain.callsession.CallSessionServiceTest`
- 관련 파일: `src/main/java/com/molla/domain/subscription/SubscriptionService.java`, `src/main/java/com/molla/controller/dto/subscription/SubscriptionWithRemainingResponse.java`, `src/main/java/com/molla/controller/SubscriptionController.java`, `src/test/java/com/molla/domain/subscription/SubscriptionServiceTest.java`, `src/test/java/com/molla/domain/callsession/CallSessionServiceTest.java`
- 비고: 현재 데모 정책은 premium 300분/일이며, free/premium 정식 정책이 확정되면 별도 플랜 규칙으로 다시 조정할 수 있다.

## 2026-05-25 - 통화 세션 Swagger 문구를 최신 start/end 계약에 맞게 정리

- 구분: 문서, 엔드포인트
- 변경: `CallSessionController`, `CallSessionResponse`, `EndSessionRequest`, `SubscriptionWithRemainingResponse`의 Swagger 설명을 최신 계약에 맞게 갱신했다. start 응답의 구독 정보 포함, end 요청의 `durationMinutes` 사용, 잔여 통화 시간 반영, 3분 미만 워커 스킵 조건을 문서에 반영했다.
- 영향: 내부 AI 오케스트레이션 서버와 프론트가 Swagger만 보고도 start/end 세션의 최신 요청/응답 의미를 더 정확하게 이해할 수 있다.
- 확인: 관련 컨트롤러/DTO의 OpenAPI 어노테이션 문구를 점검하고 `./gradlew testClasses`로 컴파일 검증한다.
- 관련 파일: `src/main/java/com/molla/controller/CallSessionController.java`, `src/main/java/com/molla/controller/dto/callsession/CallSessionResponse.java`, `src/main/java/com/molla/controller/dto/callsession/EndSessionRequest.java`, `src/main/java/com/molla/controller/dto/subscription/SubscriptionWithRemainingResponse.java`
- 비고: 동작 로직 변경 없이 Swagger 문구와 예시만 최신화했다.

## 2026-05-25 - end 세션 요청의 durationMinutes를 통화 시간 집계에 반영

- 구분: 엔드포인트, 메인 로직, 구독, test
- 변경: 내부 `end` 요청 DTO의 통화 시간 필드를 `durationMinutes`로 변경하고, completed 세션 종료 시 서버 계산값 대신 이 분 값을 초 단위로 환산해 `call_sessions.duration_seconds`에 저장하도록 변경했다.
- 영향: 구독 잔여시간은 `call_sessions.duration_seconds` 합계를 기준으로 계산되므로, AI 서버가 보내는 실제 통화 분이 저장되면 이후 구독의 오늘 잔여 통화 시간도 그 값 기준으로 정확히 차감된다.
- 확인: `EndSessionRequestJsonTest`에서 `durationMinutes` 역직렬화를 검증했고, `CallSessionServiceTest`에서 end 요청의 `durationMinutes=3`이 세션과 응답에 `180초`로 반영되는지 검증했다.
- 관련 파일: `src/main/java/com/molla/controller/dto/callsession/EndSessionRequest.java`, `src/main/java/com/molla/domain/callsession/CallSession.java`, `src/main/java/com/molla/domain/callsession/CallSessionService.java`, `src/test/java/com/molla/controller/dto/callsession/EndSessionRequestJsonTest.java`, `src/test/java/com/molla/domain/callsession/CallSessionServiceTest.java`
- 비고: 음수 `durationMinutes`는 `INVALID_REQUEST`로 거부한다.

## 2026-05-25 - 신규 유저 생성 시 데모 premium 구독 자동 부여

- 구분: 메인 로직, 구독, 인증, 통화 세션, test
- 변경: `SubscriptionService`에 활성 구독이 없을 때 `premium` / `expiresAt = null`인 데모 기본 구독을 보장하는 메서드를 추가하고, `startSession` 및 `verifyCode`에서 새 유저를 생성할 때 이를 자동 호출하도록 변경했다.
- 영향: 통화 시작이나 인증 로그인으로 새 유저가 생성되면 즉시 활성 premium 구독도 함께 생성된다. 기존 활성 구독이 있는 유저는 중복 생성되지 않는다.
- 확인: `CallSessionServiceTest`, `AuthServiceTest`, `SubscriptionServiceTest`에서 각각 startSession 경로, verifyCode 경로, 구독 보장 서비스의 생성/스킵 동작을 검증했다.
- 관련 파일: `src/main/java/com/molla/domain/subscription/SubscriptionService.java`, `src/main/java/com/molla/domain/callsession/CallSessionService.java`, `src/main/java/com/molla/domain/auth/AuthService.java`, `src/test/java/com/molla/domain/subscription/SubscriptionServiceTest.java`, `src/test/java/com/molla/domain/auth/AuthServiceTest.java`, `src/test/java/com/molla/domain/callsession/CallSessionServiceTest.java`
- 비고: 현재는 데모 정책으로 모든 신규 유저를 premium 300분/일로 생성하며, free/premium 정식 정책이 확정되면 별도 플랜 규칙으로 대체할 수 있다.

## 2026-05-25 - start 세션 응답에 활성 구독과 오늘 잔여 시간 포함

- 구분: 엔드포인트, 메인 로직, test
- 변경: `CallSessionResponse`에 구독 정보를 추가하고, `startSession` 응답에서 현재 활성 구독과 `remainingMinutesToday`를 함께 내려주도록 변경했다.
- 영향: AI 오케스트레이션 서버는 start 응답만으로 현재 플랜과 오늘 남은 통화 가능 시간을 바로 확인할 수 있다. 다른 세션 응답 경로는 기존처럼 구독 필드가 null일 수 있다.
- 확인: `CallSessionServiceTest`에서 신규/기존 유저 모두 start 응답의 `subscription` 필드가 채워지는지 검증했다.
- 관련 파일: `src/main/java/com/molla/controller/dto/callsession/CallSessionResponse.java`, `src/main/java/com/molla/domain/callsession/CallSessionService.java`, `src/test/java/com/molla/domain/callsession/CallSessionServiceTest.java`, `src/main/java/com/molla/controller/CallSessionController.java`
- 비고: 현재는 start 응답에만 실구독 정보 주입을 보장하고, 목록/상세/종료 응답은 null을 유지한다.

## 2026-05-25 - 통화 시작 시 phoneNumber 기준 유저 자동 생성

- 구분: 메인 로직, 통화 세션, test
- 변경: `startSession`이 들어올 때 `phoneNumber`로 기존 유저가 없으면 `User.createByPhone(...)`로 미가입 유저를 먼저 생성한 뒤, 그 유저 ID를 세션에 연결하도록 변경했다.
- 영향: 이제 단순 통화 시작만으로도 `users` 테이블에 phoneNumber 기반 유저가 생성될 수 있으며, `call_sessions.user_id`는 신규 번호에서도 null이 아니라 생성된 유저 ID를 가지게 된다.
- 확인: `CallSessionServiceTest`에서 phoneNumber가 없을 때 유저를 생성하고, 있을 때는 기존 유저를 재사용하는지 검증했다.
- 관련 파일: `src/main/java/com/molla/domain/callsession/CallSessionService.java`, `src/test/java/com/molla/domain/callsession/CallSessionServiceTest.java`
- 비고: 생성되는 유저는 `registered = false`, `status = active` 상태의 미가입 유저다.

## 2026-05-23 - 로그인 응답 isNewUser Swagger 설명 보강

- 구분: 문서, 엔드포인트
- 변경: 인증번호 확인 API와 `TokenResponse`의 Swagger 설명을 보강해 `isNewUser: false`가 해당 phoneNumber의 기존 유저가 이미 존재해서 추가 가입 없이 바로 로그인 완료되는 경우임을 명시했다.
- 영향: 프론트와 QA가 `isNewUser` 의미를 혼동하지 않고 로그인/회원가입 분기 기준을 더 명확하게 이해할 수 있다.
- 확인: `AuthController`, `TokenResponse`의 OpenAPI 설명 문구를 점검했다.
- 관련 파일: `src/main/java/com/molla/controller/AuthController.java`, `src/main/java/com/molla/controller/dto/auth/TokenResponse.java`
- 비고: 동작 로직 변경은 없고 Swagger 설명만 수정했다.

## 2026-05-22 - 3분 미만 completed 통화의 워커 후처리 스킵

- 구분: 메인 로직, 운영, test
- 변경: `CallSessionWorker`가 `durationSeconds < 180` 인 completed 세션이면 리포트 생성과 메모리 포인트 업로드를 모두 건너뛰고 즉시 종료하도록 변경했다.
- 영향: 3분 미만의 짧은 통화는 `feedback_reports` 생성과 `POST /memory/points` 호출이 발생하지 않는다. 180초 이상 세션만 기존 후처리를 계속 수행한다.
- 확인: `CallSessionWorkerTest`에서 179초는 스킵되고 180초는 리포트 생성 및 메모리 업로드가 수행되는지 검증했다.
- 관련 파일: `src/main/java/com/molla/domain/worker/CallSessionWorker.java`, `src/test/java/com/molla/domain/worker/CallSessionWorkerTest.java`
- 비고: 기준 시간은 워커 내부 상수 `180초`로 관리한다.

## 2026-05-21 - 메모리 포인트 nullable payload 계약으로 복귀

- 구분: AI 메모리, 메인 로직, 운영
- 변경: `QdrantClient`의 최근 null 필드 생략 로직과 422 응답 본문 예외 래핑을 롤백하고, `userId`, `assistantText`, `createdAt`, `audioKey`를 다시 payload 키 고정 + nullable 값 형태로 전송하도록 복귀했다.
- 영향: AI 서버는 payload 키 존재를 전제로 로직을 짤 수 있고, null 허용 스키마로 계약을 맞추면 조회 시 필드 부재 문제 없이 일관된 shape를 유지할 수 있다.
- 확인: `QdrantClientTest` 기본 payload 생성 테스트로 `assistantText`, `createdAt`, `audioKey` 필드 shape가 유지되는지 검증한다.
- 관련 파일: `src/main/java/com/molla/domain/worker/QdrantClient.java`, `src/test/java/com/molla/domain/worker/QdrantClientTest.java`
- 비고: `/memory/points` 422 원인은 이제 AI 서버 request model에서 nullable 필드를 허용하도록 맞춰 해결하는 방향이다.

## 2026-05-19 - 리포트 프롬프트에서 핵심 문장 다중 생성 강제

- 구분: 메인 로직, test
- 변경: `OpenAiClient.generateReport` 프롬프트에 `coreSentences`는 여러 문장이어야 하고 최소 3개 이상이어야 한다는 조건을 명시했다.
- 영향: OpenAI 리포트 생성 시 핵심 문장 피드백이 단일 문장이 아니라 복수 문장 배열로 생성될 가능성이 높아진다.
- 확인: 프롬프트 본문과 JSON 예시에 다중 `coreSentences` 구조를 추가했고, 관련 문자열 검증 테스트를 작성했다.
- 관련 파일: `src/main/java/com/molla/domain/worker/OpenAiClient.java`, `src/test/java/com/molla/domain/worker/OpenAiClientPromptTest.java`
- 비고: 이 변경은 프롬프트 제약 강화이며, 모델 응답 품질은 실제 샘플 transcript로 추가 검증할 수 있다.

## 2026-05-19 - 채팅 기능 및 user_memories 제거

- 구분: 메인 로직, 엔드포인트, 문서
- 변경: 채팅 API와 chatmessage 도메인을 제거하고, 통화 후 워커의 Step 2 `user_memories` 갱신을 삭제했다. `usermemory` 도메인도 함께 제거했다.
- 영향: 비동기 워커는 이제 `FeedbackReport` 생성과 `Qdrant` 업서트만 수행한다. `/api/v1/chat` API는 더 이상 제공되지 않는다.
- 확인: 채팅 컨트롤러/DTO/도메인, `usermemory` 패키지, 워커 Step 2, 관련 기획 문서와 드롭 SQL 문서를 함께 수정했다.
- 관련 파일: `src/main/java/com/molla/domain/worker/CallSessionWorker.java`, `src/main/java/com/molla/domain/callsession/CallSessionService.java`, `src/main/java/com/molla/domain/worker/OpenAiClient.java`, `docs/PROJECT_PLAN.md`, `docs/sql/20260519_drop_chat_and_user_memories.sql`
- 비고: 운영 DB 반영 시 `chat_messages`, `user_memories` 테이블 드롭 SQL을 적용해야 한다.

## 2026-05-19 - 피드백 리포트 구조 개편

- 구분: 메인 로직, 엔드포인트, 문서
- 변경: `FeedbackReport`를 한 줄 총평, 핵심 문장 피드백, 습관 분석, 시험 점수 배열, 약점 배열 중심 구조로 개편하고, OpenAI 리포트 생성도 새 `Report` 객체를 기준으로 파싱하도록 수정했다.
- 영향: 리포트 상세/요약 응답의 JSON 필드가 `coreSentences`, `habitAnalyses`, `scores`, `weakPoints` 중심으로 바뀐다.
- 확인: `OpenAiClient`, `CallSessionWorker`, `FeedbackReport`, 응답 DTO, SQL 문서를 함께 수정했다.
- 관련 파일: `src/main/java/com/molla/domain/feedbackreport/Report.java`, `src/main/java/com/molla/domain/feedbackreport/FeedbackReport.java`, `src/main/java/com/molla/domain/worker/OpenAiClient.java`, `src/main/java/com/molla/domain/worker/CallSessionWorker.java`, `src/main/java/com/molla/controller/dto/feedbackreport/FeedbackReportResponse.java`, `src/main/java/com/molla/controller/dto/feedbackreport/FeedbackReportSummaryResponse.java`, `docs/sql/20260519_restructure_feedback_reports_for_report_object.sql`
- 비고: 기존 `grammarCorrections`, `vocabularySuggestions`, `habitAnalysis`, `pronunciationNotes`, `overallScore` 컬럼은 SQL 마이그레이션으로 제거 대상이다.

## 2026-05-19 - 내부 세션 종료 요청 utterances 수신 추가

- 구분: 엔드포인트, 메인 로직, test
- 변경: 내부 `PATCH /api/v1/internal/sessions/{id}/end` 요청 DTO가 `transcript`와 함께 `utterances` 배열도 받을 수 있도록 확장했다.
- 영향: AI 서버가 `status`, `transcript`, `utterances`를 함께 보내도 백엔드 요청 바인딩이 깨지지 않는다.
- 확인: `EndSessionRequest`에 `utterances` 필드를 추가했고, JSON 역직렬화 테스트를 작성했다.
- 관련 파일: `src/main/java/com/molla/controller/dto/callsession/EndSessionRequest.java`, `src/test/java/com/molla/controller/dto/callsession/EndSessionRequestJsonTest.java`
- 비고: 현재는 API 연결 호환성 확보가 목적이며, `utterances` 저장/활용 로직은 아직 추가하지 않았다.

## 2026-05-19 - 작업 단위 커밋 규칙 추가

- 구분: 운영, 문서
- 변경: 모든 작업은 기본적으로 `git add`와 `git commit`까지 완료하고, 커밋 메시지는 `feat`, `fix`, `docs`, `test` 등 전통적인 타입과 명확한 제목을 사용하도록 `AGENTS.md`에 규칙을 추가했다.
- 영향: 에이전트는 향후 작업마다 변경사항을 스테이징하고 커밋하는 것을 기본 완료 기준으로 삼는다.
- 확인: `AGENTS.md`의 커밋 전 기준 섹션을 갱신했다.
- 관련 파일: `AGENTS.md`, `memories.md`
- 비고: 사용자가 커밋 금지를 명시하거나 검증 실패/요구사항 미확정/충돌이 있으면 커밋하지 않고 이유를 보고한다.

## 2026-05-19 - 운영 환경변수 문서 추가

- 구분: 환경변수, 운영, 문서
- 변경: 운영 서버의 환경변수는 `docker-compose.yml`이 참조하는 11개 항목으로 정리했다.
- 영향: 에이전트와 개발자는 운영 환경변수를 확인할 때 `docs/ops/env.md`를 기준으로 본다.
- 확인: `docker-compose.yml`, `src/main/resources/application.yml`, `docs/ops/env.md`를 대조했다.
- 관련 파일: `docs/ops/env.md`, `docker-compose.yml`, `src/main/resources/application.yml`
- 비고: 실제 환경변수 값은 문서에 기록하지 않는다.

## 2026-05-19 - GitHub Actions secrets and variables 문서 추가

- 구분: 운영, 배포, 문서
- 변경: GitHub Actions에 등록된 secrets and variables 이름을 `docs/ops/Github.md`에 정리했다.
- 영향: 배포 자동화 관련 작업 시 필요한 secret 이름과 용도를 확인할 수 있다.
- 확인: 사용자 제공 목록을 문서화했다.
- 관련 파일: `docs/ops/Github.md`
- 비고: 실제 secret 값은 GitHub Settings에서만 관리한다.

## 2026-05-19 - AWS 운영 정보 문서화

- 구분: 운영, 보안, 문서
- 변경: EC2 Public IPv4 `43.202.22.150`과 인바운드 규칙 `443`, `3306`, `22`, `80`을 `docs/ops/AWS.md`에 기록했다.
- 영향: 운영 서버 접속, 배포, 보안그룹 확인 시 기준 문서로 사용한다.
- 확인: 사용자 제공 정보를 문서화했다.
- 관련 파일: `docs/ops/AWS.md`
- 비고: `22`와 `3306`이 `0.0.0.0/0`에 열려 있으므로 운영 보안상 제한이 필요하다.

## 2026-05-19 - AGENTS.md 하네스 문서 추가

- 구분: 운영, 문서
- 변경: 작업 시작 전 읽는 에이전트 운영 지침으로 `AGENTS.md`를 추가했다.
- 영향: 에이전트는 Observe, Plan, Act, Verify, Report 루프와 검증 중심 규칙을 따라 작업한다.
- 확인: `AGENTS.md` 내용을 읽고 미완성 표식 검색을 수행했다.
- 관련 파일: `AGENTS.md`
- 비고: 프로젝트 구조, 검증 명령, 운영 정책이 바뀌면 함께 갱신한다.

## 2026-05-19 - 프로젝트 기획 문서 추가

- 구분: 문서
- 변경: MollaAI Server의 제품 목표, 사용자 흐름, 기능 범위, 시스템 구성, API 범위, 비기능 요구사항을 정리했다.
- 영향: 제품/도메인 변경 전 `docs/PROJECT_PLAN.md`를 기준 문서로 확인한다.
- 확인: 현재 컨트롤러, 도메인, 배포 문서를 읽고 기획 문서를 작성했다.
- 관련 파일: `docs/PROJECT_PLAN.md`
- 비고: API나 도메인 모델이 변경되면 함께 갱신한다.

## 2026-05-21 - S3 환경변수명을 운영 .env 기준으로 정렬

- 구분: 운영 환경변수, S3, 설정
- 변경: 백엔드가 기대하는 S3 버킷 환경변수명을 `S3_AUDIO_BUCKET`에서 `ORCH_S3_AUDIO_BUCKET`으로 변경하고 `ORCH_S3_AUDIO_PREFIX`도 설정 키로 반영했다.
- 영향: 운영 서버 `.env`에 이미 있는 `AWS_REGION`, `ORCH_S3_AUDIO_BUCKET`, `ORCH_S3_AUDIO_PREFIX`를 그대로 사용해 앱이 기동된다.
- 확인: `application.yml`, `application-test.yml`, `docs/ops/env.md`를 함께 갱신했다.
- 관련 파일: `src/main/resources/application.yml`, `src/test/resources/application-test.yml`, `docs/ops/env.md`
- 비고: 현재 prefix 값은 presigned URL 생성에 직접 쓰지 않지만 운영 환경과 이름을 맞추기 위해 유지한다.

## 2026-05-21 - 테스트 프로필에 S3 placeholder 추가

- 구분: 테스트, 설정
- 변경: `application-test.yml`에 `aws.region`, `aws.s3.audio-bucket`, `aws.s3.presign-expiration-minutes` 더미 값을 추가했다.
- 영향: `MollaApplicationTests.contextLoads()`가 S3 설정 placeholder 누락으로 실패하지 않고 CI에서 정상 기동된다.
- 확인: `./gradlew test --tests com.molla.MollaApplicationTests`로 검증했다.
- 관련 파일: `src/test/resources/application-test.yml`
- 비고: 테스트 전용 값이라 실제 AWS 자격 증명이나 버킷이 필요하지 않다.

## 2026-05-21 - 메모리 포인트 업로드 메서드를 POST로 변경

- 구분: 엔드포인트, AI 메모리
- 변경: `https://orch.mollatalk.com/memory/points` 호출 메서드를 `PUT`에서 `POST`로 변경했다.
- 영향: 백엔드는 AI 서버 FastAPI 계약과 동일하게 POST 요청으로 메모리 포인트를 업로드한다.
- 확인: `QdrantClient` 호출부를 수정하고 기존 `QdrantClientTest`로 body 생성 로직이 유지되는지 확인한다.
- 관련 파일: `src/main/java/com/molla/domain/worker/QdrantClient.java`
- 비고: 요청 body 스키마는 변경하지 않았다.

## 2026-05-21 - 메모리 포인트 업로드 엔드포인트를 AI 서버로 전환

- 구분: AI 메모리, 엔드포인트, 메인 로직
- 변경: 백엔드의 `QdrantClient`가 직접 Qdrant에 PUT하지 않고 `https://orch.mollatalk.com/memory/points` 엔드포인트로 메모리 포인트 body를 전송하도록 변경했다.
- 영향: 메모리 적재 책임은 AI 서버가 맡고, 백엔드는 `userText` 임베딩과 payload 조립 후 AI 서버 엔드포인트에 업로드한다.
- 확인: `QdrantClientTest`로 업로드 body 스키마가 유지되는지 검증했다.
- 관련 파일: `src/main/java/com/molla/domain/worker/QdrantClient.java`, `src/test/java/com/molla/domain/worker/QdrantClientTest.java`
- 비고: 이전 직접 Qdrant 호출용 주석 코드는 제거했다.

## 2026-05-21 - Qdrant turns payload를 대화 문맥 중심으로 단순화

- 구분: Qdrant, AI 메모리, 메인 로직
- 변경: `upsertTurns()`가 `userText`만 임베딩하고, payload는 `userId`, `phoneNumber`, `userText`, `assistantText`, `createdAt`, `audioKey`만 담도록 변경했다.
- 영향: Qdrant 검색 결과는 사용자 발화 의미를 기준으로 찾고, payload의 `userText`와 `assistantText`로 문맥을 복원하는 구조가 된다.
- 확인: `QdrantClientTest`로 생성되는 point body 스키마와 vector 입력 텍스트를 검증했다.
- 관련 파일: `src/main/java/com/molla/domain/worker/QdrantClient.java`, `src/test/java/com/molla/domain/worker/QdrantClientTest.java`
- 비고: 실제 Qdrant PUT 호출은 주석 처리해 두었고, AI 서버 FastAPI 엔드포인트 연결 시 그 부분을 대체하면 된다.

## 2026-05-20 - audioKey 기반 S3 audioUrl 응답 전환

- 구분: API, S3, 리포트, 운영 환경변수
- 변경: 세션 종료 turns와 core sentence 오디오 필드를 base64 `audio`에서 `audioKey` 기반으로 전환하고, 응답 시 S3 presigned `audioUrl`을 생성하도록 변경했다.
- 영향: 프론트는 리포트 상세 응답에서 `audioUrl`을 받아 바로 음성을 재생할 수 있고, 큰 base64 payload를 end 요청과 응답에서 제거한다.
- 확인: `EndSessionRequestJsonTest`, `ReportAudioEnricherTest`, `FeedbackReportViewMapperTest`, `ReportJsonTest`로 audioKey 저장과 audioUrl 매핑을 검증한다.
- 관련 파일: `src/main/java/com/molla/domain/worker/S3AudioUrlService.java`, `src/main/java/com/molla/config/S3Config.java`, `src/main/resources/application.yml`, `src/main/java/com/molla/domain/feedbackreport/Report.java`
- 비고: 런타임에는 `AWS_REGION`, `S3_AUDIO_BUCKET`, `S3_AUDIO_PRESIGN_EXPIRATION_MINUTES` 환경변수가 필요하다.

## 2026-05-20 - 코어 문장에 turn audio 첨부

- 구분: 리포트, 메인 로직, API
- 변경: OpenAI가 반환한 `sourceTurnIndex`를 기준으로 해당 turn의 사용자 오디오를 `Report.coreSentences`에 붙이도록 변경했다.
- 영향: 리포트 상세 응답의 각 core sentence는 `sampleRate`, `encoding`, `audio`를 함께 내려주므로 프론트가 바로 재생 기능을 붙일 수 있다.
- 확인: `ReportAudioEnricherTest`, `ReportJsonTest`, `FeedbackReportViewMapperTest`로 오디오 첨부와 응답 매핑을 검증했다.
- 관련 파일: `src/main/java/com/molla/domain/worker/ReportAudioEnricher.java`, `src/main/java/com/molla/domain/feedbackreport/Report.java`, `src/main/java/com/molla/domain/worker/CallSessionWorker.java`, `src/main/java/com/molla/controller/dto/feedbackreport/FeedbackReportResponse.java`
- 비고: 현재는 `audio` 원문(base64)을 그대로 응답하며, 추후 용량 이슈가 있으면 object storage URL 방식으로 전환할 수 있다.

## 2026-05-20 - 리포트 생성과 Qdrant 적재를 turns 기반으로 전환

- 구분: API, 메인 로직, AI 리포트, Qdrant, 데이터베이스
- 변경: 세션 종료 시 transcript를 만들지 않고 `turns` 원본을 `call_sessions.turns_json`에 저장한 뒤, 워커가 이를 읽어 OpenAI 리포트 생성과 Qdrant 적재를 모두 처리하도록 변경했다.
- 영향: OpenAI에는 `ReportTurnInput(index, userText, assistantText)` DTO를 넘기며, 리포트의 `coreSentences`는 `sourceTurnIndex`를 포함하게 된다.
- 확인: `EndSessionRequestJsonTest`, `OpenAiClientGenerateReportTest`, `OpenAiClientPromptTest`, `ReportJsonTest`로 turns 직렬화/역직렬화, OpenAI 요청 포맷, 리포트 파싱을 검증한다.
- 관련 파일: `src/main/java/com/molla/domain/callsession/CallSession.java`, `src/main/java/com/molla/domain/callsession/CallSessionTurn.java`, `src/main/java/com/molla/domain/worker/ReportTurnInput.java`, `src/main/java/com/molla/domain/worker/OpenAiClient.java`, `src/main/java/com/molla/domain/worker/CallSessionWorker.java`, `src/main/java/com/molla/domain/worker/QdrantClient.java`, `docs/sql/20260520_add_turns_json_to_call_sessions.sql`
- 비고: 운영 DB에는 `docs/sql/20260520_add_turns_json_to_call_sessions.sql`을 반영해야 하며, 기존 `transcript` 컬럼은 더 이상 사용하지 않는다.

## 2026-05-20 - 세션 종료 요청을 turns 기반으로 변경

- 구분: API, DTO, 통화 세션
- 변경: 내부 종료 API의 `EndSessionRequest`가 `transcript`, `utterances` 대신 `turns` 배열을 받도록 변경했다.
- 영향: AI 서버는 사용자/어시스턴트 턴이 포함된 `turns` 객체를 보내고, 서버는 이를 기반으로 내부 transcript를 조립해 기존 리포트/Qdrant 흐름을 유지한다.
- 확인: `EndSessionRequestJsonTest`에서 `turns` 역직렬화와 `renderTranscript()` 결과를 검증했다.
- 관련 파일: `src/main/java/com/molla/controller/dto/callsession/EndSessionRequest.java`, `src/main/java/com/molla/domain/callsession/CallSessionService.java`, `src/test/java/com/molla/controller/dto/callsession/EndSessionRequestJsonTest.java`
- 비고: 현재는 `turns`를 영속화하지 않고 transcript만 조립해서 저장한다.

## 2026-05-19 - 리포트 응답 DTO를 Report 기반 구조로 변경

- 구분: API, DTO, 리포트
- 변경: `FeedbackReportResponse`, `FeedbackReportSummaryResponse`가 JSON 문자열 대신 `Report` 기반의 구조화된 배열과 객체를 반환하도록 변경했다.
- 영향: 프론트는 `coreSentences`, `habitAnalyses`, `scores`, `weakPoints`를 문자열 파싱 없이 바로 사용할 수 있다.
- 확인: `FeedbackReportViewMapperTest`, `ReportJsonTest`로 구조화 응답 매핑과 JSON 역직렬화를 검증했다.
- 관련 파일: `src/main/java/com/molla/controller/dto/feedbackreport/FeedbackReportResponse.java`, `src/main/java/com/molla/controller/dto/feedbackreport/FeedbackReportSummaryResponse.java`, `src/main/java/com/molla/domain/feedbackreport/FeedbackReportViewMapper.java`, `src/main/java/com/molla/domain/feedbackreport/FeedbackReportService.java`
- 비고: DB 저장 형식은 JSON 문자열 컬럼을 유지하고, API 응답 시점에만 구조화해서 내려준다.
