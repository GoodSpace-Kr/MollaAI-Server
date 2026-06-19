# Nginx Reverse Proxy

이 서버의 Spring 애플리케이션은 `8080` 포트에서 동작한다.
외부에서는 `80` 또는 `443` 포트로 받고 Nginx가 Spring 애플리케이션 `127.0.0.1:8080` 으로 프록시한다.
오케스트레이터 agent control용 `/api/v1/agents/control` 과 `/healthz`도 Spring 애플리케이션에서 직접 제공한다.

## 최종 요청 URL

- `POST https://api.mollatalk.com/api/v1/sessions/start`

## 1. Docker Compose 변경

`docker-compose.yml` 에서 백엔드를 외부 전체가 아니라 로컬 루프백에만 바인딩한다.

```yaml
ports:
  - "127.0.0.1:8080:8080"
```

이렇게 해야 외부에서는 `8080` 으로 직접 접근하지 못하고, Nginx를 통해서만 접근하게 된다.

## 2. Nginx 설정 파일 배치

이 저장소의 설정 파일:

- `docs/deploy/nginx.conf`

`wss://api.mollatalk.com/api/v1/agents/control` 테스트는 실제 `443 ssl` 서버 블록에 WebSocket location이 있어야 동작한다. Certbot이 `443` 서버 블록을 별도로 생성한 운영 서버라면 `docs/deploy/nginx.conf`의 `/api/v1/agents/control` location을 해당 `443` 서버 블록에도 동일하게 반영한다.

서버에 복사:

```bash
sudo cp docs/deploy/nginx.conf /etc/nginx/sites-available/molla
sudo ln -sf /etc/nginx/sites-available/molla /etc/nginx/sites-enabled/molla
sudo rm -f /etc/nginx/sites-enabled/default
```

## 3. Nginx 설치 및 재시작

```bash
sudo apt update
sudo apt install -y nginx
sudo nginx -t
sudo systemctl restart nginx
sudo systemctl enable nginx
```

## 4. 애플리케이션 재기동

```bash
docker compose up -d
```

## 5. 확인 명령

Nginx가 `80` 에서 listen 중인지 확인:

```bash
sudo ss -ltnp | grep ':80'
```

Spring 앱이 로컬 `8080` 에서 listen 중인지 확인:

```bash
ss -ltnp | grep ':8080'
```

Spring health check 로컬 확인:

```bash
curl -i http://127.0.0.1:8080/healthz
```

운영 도메인 프록시 확인:

```bash
curl -i https://api.mollatalk.com/healthz
```

오케스트레이터 agent control WSS는 agent token query가 필요하다.

```bash
python ws_test_client.py --url 'wss://api.mollatalk.com/api/v1/agents/control?token=<AI_AGENT_TOKEN>' --log-file agent-control.log
```

## 응답 해석
- `404 Not Found`: Nginx가 다른 서버 블록을 타고 있거나 설정 반영이 안 됨
- `405 Method Not Allowed`: GET으로 호출했을 가능성 큼. 이 API는 `POST` 만 받음
- `400 Bad Request`: JSON 형식, `phoneNumber` 형식 오류, 또는 WebSocket 요청이 Nginx에서 upgrade 헤더 없이 일반 HTTP 프록시로 전달됨
- `403/404` JSON 응답: 애플리케이션까지는 도달했고, 비즈니스 로직에서 실패
- 연결 실패: 프로세스 미기동, 보안그룹 차단, 또는 Nginx 미기동
