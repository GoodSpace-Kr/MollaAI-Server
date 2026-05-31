# GitHub 운영 정보

이 문서는 MollaAI Server의 GitHub Actions 실행에 필요한 Secrets and Variables 구성을 기록한다. 실제 값은 GitHub 저장소의 Settings에서 관리하며, 이 문서에는 키 이름과 용도만 남긴다.

## GitHub Actions Secrets and Variables

현재 GitHub Actions에는 아래 항목이 등록되어 있다.

| Name | 용도 |
| --- | --- |
| `SERVER_HOST` | 배포 대상 서버 호스트 또는 Public IPv4 |
| `SERVER_SSH_KEY` | 배포 서버 접속용 SSH private key |
| `SERVER_USER` | 배포 서버 SSH 사용자명 |
| `AWS_ACCESS_KEY_ID` | AWS API 접근용 Access Key ID |
| `AWS_S3_ACCESS_KEY` | AWS S3 Access Key ID 대체 이름. 표준 키가 없을 때 fallback으로 사용 |
| `AWS_S3_SECRET_KEY` | AWS S3 Secret Access Key 대체 이름. 표준 키가 없을 때 fallback으로 사용 |
| `AWS_SECRET_ACCESS_KEY` | AWS API 접근용 Secret Access Key |
| `AWS_SESSION_TOKEN` | AWS STS 임시 자격증명 사용 시 필요한 Session Token |
| `DOCKERHUB_TOKEN` | Docker Hub 로그인 또는 이미지 push용 토큰 |
| `DOCKERHUB_USERNAME` | Docker Hub 사용자명 |
| `EC2_SSH_KEY` | EC2 접속 또는 배포 자동화에 사용하는 SSH key |

## 관리 기준

- 실제 secret 값은 문서, 코드, 로그, PR 설명에 적지 않는다.
- GitHub Actions 로그에 secret 값이 출력되지 않도록 echo/debug 명령을 주의한다.
- secret을 교체하면 관련 워크플로우가 정상 배포되는지 확인한다.
- 배포 서버, Docker Hub 계정, AWS IAM 권한이 바뀌면 이 문서의 이름과 용도를 갱신한다.
- AWS 권한은 배포에 필요한 최소 권한으로 제한한다.

## 관련 문서

- AWS 인스턴스와 보안그룹: `docs/ops/AWS.md`
- 서버 배포와 Nginx 프록시: `docs/deploy/README.md`
