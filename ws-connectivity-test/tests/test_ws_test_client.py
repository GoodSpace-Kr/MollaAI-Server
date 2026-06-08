import importlib.util
import unittest
from pathlib import Path


CLIENT_PATH = Path(__file__).resolve().parents[1] / "client" / "ws_test_client.py"


def load_client_module():
    spec = importlib.util.spec_from_file_location("ws_test_client", CLIENT_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class WsTestClientHelpersTest(unittest.TestCase):
    def test_backoff_delays_follow_required_sequence_and_cap_at_thirty_seconds(self):
        client = load_client_module()

        delays = [client.backoff_delay(attempt) for attempt in range(8)]

        self.assertEqual(delays, [1, 2, 5, 10, 30, 30, 30, 30])

    def test_worker_messages_include_type_worker_id_and_sequence(self):
        client = load_client_module()

        hello = client.build_message("worker_hello", "campus-2080ti-test", 1)
        heartbeat = client.build_message("heartbeat", "campus-2080ti-test", 2)

        self.assertEqual(
            hello,
            {
                "type": "worker_hello",
                "workerId": "campus-2080ti-test",
                "seq": 1,
            },
        )
        self.assertEqual(
            heartbeat,
            {
                "type": "heartbeat",
                "workerId": "campus-2080ti-test",
                "seq": 2,
            },
        )


if __name__ == "__main__":
    unittest.main()
