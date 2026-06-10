from __future__ import annotations

import argparse
import asyncio
import json
import logging
from pathlib import Path
from typing import Any

import websockets


BACKOFF_DELAYS_SECONDS = [1, 2, 5, 10, 30]
DEFAULT_WORKER_ID = "campus-2080ti-test"


def backoff_delay(attempt: int) -> int:
    return BACKOFF_DELAYS_SECONDS[min(attempt, len(BACKOFF_DELAYS_SECONDS) - 1)]


def build_message(message_type: str, worker_id: str, seq: int) -> dict[str, Any]:
    return {
        "type": message_type,
        "workerId": worker_id,
        "seq": seq,
    }


def configure_logging(log_file: str | None) -> None:
    handlers: list[logging.Handler] = [logging.StreamHandler()]
    if log_file:
        Path(log_file).parent.mkdir(parents=True, exist_ok=True)
        handlers.append(logging.FileHandler(log_file, encoding="utf-8"))

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        handlers=handlers,
    )


async def send_json(websocket: websockets.ClientConnection, payload: dict[str, Any]) -> None:
    encoded = json.dumps(payload, ensure_ascii=False)
    await websocket.send(encoded)
    logging.info("sent %s", encoded)


async def heartbeat_loop(
    websocket: websockets.ClientConnection,
    worker_id: str,
    heartbeat_interval: float,
    first_seq: int,
) -> None:
    seq = first_seq
    while True:
        await asyncio.sleep(heartbeat_interval)
        await send_json(websocket, build_message("heartbeat", worker_id, seq))
        seq += 1


async def receive_loop(websocket: websockets.ClientConnection) -> None:
    async for message in websocket:
        logging.info("received %s", message)

    raise RuntimeError("websocket receive loop ended")


async def run_once(args: argparse.Namespace) -> None:
    logging.info("connecting url=%s workerId=%s", args.url, args.worker_id)
    async with websockets.connect(
        args.url,
        open_timeout=args.open_timeout,
        ping_interval=args.ping_interval,
        ping_timeout=args.ping_timeout,
    ) as websocket:
        logging.info("connected url=%s", args.url)

        initial_message = await asyncio.wait_for(websocket.recv(), timeout=args.initial_timeout)
        logging.info("server initial %s", initial_message)

        await send_json(websocket, build_message("worker_hello", args.worker_id, 1))

        receiver = asyncio.create_task(receive_loop(websocket))
        heartbeat = asyncio.create_task(
            heartbeat_loop(websocket, args.worker_id, args.heartbeat_interval, 2)
        )

        done, pending = await asyncio.wait(
            {receiver, heartbeat},
            return_when=asyncio.FIRST_EXCEPTION,
        )
        for task in pending:
            task.cancel()
        await asyncio.gather(*pending, return_exceptions=True)
        for task in done:
            task.result()


async def run_forever(args: argparse.Namespace) -> None:
    attempt = 0
    while True:
        try:
            await run_once(args)
            attempt = 0
        except KeyboardInterrupt:
            raise
        except Exception as exc:
            delay = backoff_delay(attempt)
            logging.warning(
                "connection ended error=%s reconnecting_in=%ss",
                repr(exc),
                delay,
            )
            attempt += 1
            await asyncio.sleep(delay)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="WebSocket outbound connectivity test client")
    parser.add_argument("--url", required=True, help="WebSocket URL, for example wss://api.example.com/workers/ws")
    parser.add_argument("--worker-id", default=DEFAULT_WORKER_ID, help="workerId included in hello and heartbeat messages")
    parser.add_argument("--heartbeat-interval", type=float, default=10.0, help="heartbeat interval in seconds")
    parser.add_argument("--open-timeout", type=float, default=10.0, help="connection open timeout in seconds")
    parser.add_argument("--initial-timeout", type=float, default=10.0, help="initial server message timeout in seconds")
    parser.add_argument("--ping-interval", type=float, default=20.0, help="websocket protocol ping interval in seconds")
    parser.add_argument("--ping-timeout", type=float, default=20.0, help="websocket protocol ping timeout in seconds")
    parser.add_argument("--log-file", help="optional file path to store timestamped logs for long-running checks")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    configure_logging(args.log_file)
    try:
        asyncio.run(run_forever(args))
    except KeyboardInterrupt:
        logging.info("stopped by user")


if __name__ == "__main__":
    main()
