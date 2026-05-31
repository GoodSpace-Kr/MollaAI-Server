# 운영 환경변수

이 문서는 운영 서버에서 `docker-compose.yml`이 참조하는 환경변수를 정리한다. 실제 값은 서버의 `.env`, 배포 시스템, 또는 secret 저장소에서 관리하고, 이 문서에는 절대 기록하지 않는다.

## 기준 파일

- `docker-compose.yml`
- `src/main/resources/application.yml`

## 환경변수 목록

| Name | 필수 | 사용처 | 설명 |
| --- | --- | --- | --- |
| `DB_HOST` | 필수 | Spring Datasource | MySQL 서버 호스트. `application.yml`에는 기본값 `localhost`가 있지만 운영 compose에서는 명시 주입한다. |
| `DB_USERNAME` | 필수 | Spring Datasource | MySQL 접속 사용자명 |
| `DB_PASSWORD` | 필수 | Spring Datasource | MySQL 접속 비밀번호 |
| `JWT_SECRET` | 필수 | JWT | Access Token, Refresh Token 서명에 사용하는 secret |
| `NAVER_SENS_ACCESS_KEY` | 필수 | Naver SENS | SMS 인증번호 발송용 Access Key |
| `NAVER_SENS_SECRET_KEY` | 필수 | Naver SENS | SMS 인증번호 발송용 Secret Key |
| `NAVER_SENS_SERVICE_ID` | 필수 | Naver SENS | SMS 서비스 ID |
| `NAVER_SENS_FROM_NUMBER` | 필수 | Naver SENS | SMS 발신 번호 |
| `OPENAI_API_KEY` | 필수 | OpenAI API | AI 리포트 생성, 메모리 요약, AI 코치 응답, 임베딩 생성에 사용하는 API Key |
| `QDRANT_HOST` | 필수 | Qdrant | Qdrant 서버 호스트. `application.yml`에는 기본값 `localhost`가 있지만 운영 compose에서는 명시 주입한다. |
| `QDRANT_PORT` | 필수 | Qdrant | Qdrant 서버 포트. `application.yml`에는 기본값 `6333`이 있지만 운영 compose에서는 명시 주입한다. |
| `AWS_REGION` | 필수 | AWS S3 Presigner | S3 presigned URL 생성에 사용하는 AWS 리전 |
| `AWS_ACCESS_KEY_ID` | 권장 | AWS S3 Presigner | 표준 AWS Access Key ID. 있으면 백엔드가 이 값을 우선 사용한다. |
| `AWS_SECRET_ACCESS_KEY` | 권장 | AWS S3 Presigner | 표준 AWS Secret Access Key. `AWS_ACCESS_KEY_ID`와 함께 사용한다. |
| `AWS_SESSION_TOKEN` | 선택 | AWS S3 Presigner | STS 임시 자격증명 사용 시 필요한 세션 토큰 |
| `AWS_S3_ACCESS_KEY` | 대체 | AWS S3 Presigner | 기존 운영 호환용 Access Key ID 별칭. 표준 `AWS_ACCESS_KEY_ID`가 없을 때 fallback으로 사용한다. |
| `AWS_S3_SECRET_KEY` | 대체 | AWS S3 Presigner | 기존 운영 호환용 Secret Access Key 별칭. 표준 `AWS_SECRET_ACCESS_KEY`가 없을 때 fallback으로 사용한다. |
| `ORCH_S3_AUDIO_BUCKET` | 필수 | AWS S3 | AI 오케스트레이션 서버가 저장하는 통화 turn 오디오 파일 S3 버킷 이름 |
| `ORCH_S3_AUDIO_PREFIX` | 선택 | AWS S3 | AI 오케스트레이션 서버 오디오 파일 공통 prefix. 현재 백엔드는 presigned URL 생성용으로 직접 사용하지 않지만 운영 환경과 이름을 맞춰 유지한다. |
| `S3_AUDIO_PRESIGN_EXPIRATION_MINUTES` | 선택 | AWS S3 Presigner | presigned audio URL 만료 시간(분). 기본값 `60` |

## 운영 서버 주입 방식

운영 서버의 `docker-compose.yml`은 아래 환경변수를 컨테이너 환경으로 전달한다.

```yaml
environment:
  DB_HOST: ${DB_HOST}
  DB_USERNAME: ${DB_USERNAME}
  DB_PASSWORD: ${DB_PASSWORD}
  JWT_SECRET: ${JWT_SECRET}
  NAVER_SENS_ACCESS_KEY: ${NAVER_SENS_ACCESS_KEY}
  NAVER_SENS_SECRET_KEY: ${NAVER_SENS_SECRET_KEY}
  NAVER_SENS_SERVICE_ID: ${NAVER_SENS_SERVICE_ID}
  NAVER_SENS_FROM_NUMBER: ${NAVER_SENS_FROM_NUMBER}
  OPENAI_API_KEY: ${OPENAI_API_KEY}
  QDRANT_HOST: ${QDRANT_HOST}
  QDRANT_PORT: ${QDRANT_PORT}
  AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
  AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
  AWS_SESSION_TOKEN: ${AWS_SESSION_TOKEN}
  AWS_S3_ACCESS_KEY: ${AWS_S3_ACCESS_KEY}
  AWS_S3_SECRET_KEY: ${AWS_S3_SECRET_KEY}
  AWS_REGION: ${AWS_REGION}
  ORCH_S3_AUDIO_BUCKET: ${ORCH_S3_AUDIO_BUCKET}
  ORCH_S3_AUDIO_PREFIX: ${ORCH_S3_AUDIO_PREFIX}
  S3_AUDIO_PRESIGN_EXPIRATION_MINUTES: ${S3_AUDIO_PRESIGN_EXPIRATION_MINUTES}
```

운영 서버에서 Docker Compose를 실행할 때는 같은 디렉터리의 `.env` 파일 또는 셸 환경변수를 통해 위 값들이 주입되어야 한다.
백엔드는 표준 AWS env(`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`)를 우선 사용하고, 표준 키가 비어 있으면 `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`를 fallback으로 사용한다.

## 보안 기준

- `.env` 파일은 저장소에 커밋하지 않는다.
- `DB_PASSWORD`, `JWT_SECRET`, `NAVER_SENS_SECRET_KEY`, `OPENAI_API_KEY`는 로그, PR 설명, 문서에 실제 값을 남기지 않는다.
- `JWT_SECRET`은 HS256 서명에 충분한 길이와 무작위성을 가진 값을 사용한다.
- 키를 교체한 뒤에는 서버 재기동과 인증/SMS/AI 리포트 생성 동작을 확인한다.
- 운영 장애 분석 중에도 `docker compose config`, `env`, `printenv` 출력에 secret이 섞일 수 있으므로 공유 전 마스킹한다.

## 변경 시 체크리스트

1. `docker-compose.yml`의 `environment` 항목을 갱신한다.
2. `src/main/resources/application.yml`의 참조 키와 이름이 일치하는지 확인한다.
3. GitHub Actions 또는 운영 서버 `.env`에 같은 이름이 등록되어 있는지 확인한다.
4. 이 문서를 갱신한다.
5. 배포 후 애플리케이션 기동, DB 연결, SMS 발송, OpenAI 호출, Qdrant 연결을 확인한다.

## 관련 문서

- AWS 인스턴스와 보안그룹: `docs/ops/AWS.md`
- GitHub Actions secrets and variables: `docs/ops/Github.md`
- 배포와 Nginx 프록시: `docs/deploy/README.md`
