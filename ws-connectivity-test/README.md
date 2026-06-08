# WebSocket Connectivity Test

교내 PC에서 외부 백엔드 WebSocket으로 outbound 연결이 가능한지 확인하기 위한 독립 테스트 도구이다.

## 구조

```text
ws-connectivity-test/
  server/
    main.py
    requirements.txt
  client/
    ws_test_client.py
    requirements.txt
  README.md
```

## 서버 실행

외부 서버에서 실행한다. 운영 도메인 테스트에서는 FastAPI 서버를 외부에 직접 열지 말고, 로컬 루프백 `127.0.0.1:8000`에 띄운 뒤 Nginx가 `wss://DOMAIN/workers/ws`를 이 서버로 프록시하게 둔다.

```bash
cd ws-connectivity-test/server
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 127.0.0.1 --port 8000
```

상태 확인:

```bash
curl http://127.0.0.1:8000/healthz
```

응답:

```json
{
  "status": "ok"
}
```

WebSocket 엔드포인트:

```text
ws://127.0.0.1:8000/workers/ws
```

운영 도메인 뒤에 배치할 때는 Nginx, ALB, Cloudflare 같은 프록시에서 WebSocket upgrade 헤더가 전달되어야 한다.

## 운영 도메인 연결

`wss://api.molla.ai/workers/ws`로 테스트하려면 외부 서버의 Nginx 443 서버 블록에 `/workers/ws`를 FastAPI 테스트 서버로 보내는 location이 있어야 한다. 이 저장소의 예시 설정은 `docs/deploy/nginx.conf`에 들어 있다.

핵심 설정:

```nginx
location = /workers/ws {
    proxy_pass http://127.0.0.1:8000/workers/ws;
    proxy_http_version 1.1;

    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}
```

`/healthz`도 테스트 서버로 확인하려면 다음 location을 같은 서버 블록에 둔다.

```nginx
location = /healthz {
    proxy_pass http://127.0.0.1:8000/healthz;
    proxy_http_version 1.1;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Connection "";
}
```

설정 반영:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

도메인 health check:

```bash
curl https://api.molla.ai/healthz
```

응답이 `{"status":"ok"}`이면 도메인이 FastAPI 테스트 서버까지 프록시되고 있다.

## 클라이언트 실행

교내 PC에서 실행한다.

```bash
cd ws-connectivity-test/client
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python ws_test_client.py --url wss://api.molla.ai/workers/ws --log-file connectivity.log
```

기본 동작:

- 접속 성공 로그 출력
- 서버의 `connected` 초기 메시지 출력
- `worker_hello` 전송
- 10초마다 `heartbeat` 전송
- 서버 echo 응답 출력
- 연결 종료 시 1초, 2초, 5초, 10초, 최대 30초 간격으로 자동 재연결
- 모든 로그에 timestamp 포함

30분 이상 안정성 확인:

```bash
python ws_test_client.py --url wss://api.molla.ai/workers/ws --log-file connectivity.log
```

다른 터미널에서 로그를 확인한다.

```bash
tail -f connectivity.log
```

성공적으로 유지되면 `sent {"type": "heartbeat"...}` 와 `received {"type":"echo"...}` 로그가 10초마다 반복된다.

## `ws://IP:PORT` 테스트와 `wss://DOMAIN` 테스트 차이

`ws://IP:PORT`는 암호화 없이 특정 IP와 포트에 직접 접속한다. 서버 프로세스, 보안그룹, 방화벽, 포트 포워딩, 라우팅을 빠르게 확인할 때 유용하다.

`wss://DOMAIN`은 TLS 인증서와 도메인을 사용하는 운영에 가까운 경로이다. DNS, 인증서, 443 포트, 프록시의 WebSocket upgrade 설정까지 함께 검증한다.

운영 테스트는 반드시 `wss://DOMAIN:443` 경로로 해야 한다. 교내망, 기업망, 공용 Wi-Fi는 비표준 포트를 차단하는 경우가 흔하므로 `8000`, `8080` 같은 포트의 성공/실패만으로 운영 가능 여부를 판단하면 안 된다.

예:

```bash
python ws_test_client.py --url wss://api.molla.ai/workers/ws --log-file connectivity.log
```

## 메시지 예시

서버 초기 응답:

```json
{
  "type": "connected",
  "message": "worker ws ready"
}
```

클라이언트 hello:

```json
{
  "type": "worker_hello",
  "workerId": "campus-2080ti-test",
  "seq": 1
}
```

클라이언트 heartbeat:

```json
{
  "type": "heartbeat",
  "workerId": "campus-2080ti-test",
  "seq": 2
}
```

서버 echo:

```json
{
  "type": "echo",
  "received": {
    "type": "heartbeat",
    "workerId": "campus-2080ti-test",
    "seq": 2
  }
}
```

## 에러 해석

`Connection refused`:
서버 프로세스가 떠 있지 않거나, 포트가 열려 있지 않거나, 라우팅/보안그룹이 잘못되었을 가능성이 크다.

`Timeout`:
학교망, 서버 방화벽, 보안그룹, 프록시가 outbound/inbound 연결을 막고 있을 수 있다.

`SSL/TLS error`:
인증서, 도메인, SNI, 프록시 TLS 종료 설정 문제일 수 있다. `wss://` 도메인과 인증서의 CN/SAN이 맞는지 확인한다.

`HTTP 403`:
프록시, 방화벽, WAF, 인증 정책이 WebSocket upgrade 요청을 거부했을 가능성이 있다.

`connected` 후 주기적으로 끊김:
idle timeout, Wi-Fi 불안정, 프록시 timeout, heartbeat/reconnect 설계 부족을 의심한다. 이 클라이언트는 10초 heartbeat와 자동 재연결 로그를 남기므로 끊김 주기와 재연결 성공 여부를 같이 본다.

## 배포 프록시 체크 포인트

Nginx를 사용할 경우 WebSocket 경로에 다음 설정이 필요하다.

```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
proxy_read_timeout 3600s;
proxy_send_timeout 3600s;
```
