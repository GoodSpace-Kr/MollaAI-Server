# WebRTC Test Page

Standalone browser test page for the app call flow.

## Run

From this repository:

```bash
cd docs/dev
python3 -m http.server 3000
```

Open:

```text
http://localhost:3000/webrtc-test.html
```

## Flow

The page verifies:

1. `POST /api/v1/sessions/start`
2. Browser microphone permission
3. Browser WebRTC offer creation
4. `POST /api/v1/sessions/{id}/webrtc/offer`
5. Cloudflare answer application
6. Browser ICE/PeerConnection state
7. `PATCH /api/v1/sessions/{id}/end`

Use a normal app access token, without the `Bearer ` prefix.
