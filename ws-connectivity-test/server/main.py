import json
import logging
from datetime import datetime, timezone

from fastapi import FastAPI, WebSocket, WebSocketDisconnect


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)
logger = logging.getLogger("ws-connectivity-server")

app = FastAPI(title="Molla WebSocket Connectivity Test")


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


@app.get("/healthz")
async def healthz():
    return {"status": "ok"}


@app.websocket("/workers/ws")
async def workers_ws(websocket: WebSocket):
    await websocket.accept()
    client = f"{websocket.client.host}:{websocket.client.port}" if websocket.client else "unknown"
    logger.info("websocket accepted client=%s", client)

    await websocket.send_json(
        {
            "type": "connected",
            "message": "worker ws ready",
        }
    )

    try:
        while True:
            text = await websocket.receive_text()
            try:
                received = json.loads(text)
            except json.JSONDecodeError:
                received = text

            logger.info(
                "websocket received client=%s at=%s payload=%s",
                client,
                now_iso(),
                json.dumps(received, ensure_ascii=False),
            )
            await websocket.send_json(
                {
                    "type": "echo",
                    "received": received,
                }
            )
    except WebSocketDisconnect:
        logger.info("websocket disconnected client=%s", client)
