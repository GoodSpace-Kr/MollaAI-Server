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
