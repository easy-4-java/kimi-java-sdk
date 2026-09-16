#!/usr/bin/env python3
"""Fake Kimi ACP agent for end-to-end tests.

Speaks newline-delimited JSON-RPC on stdio exactly like `kimi acp`:
answers `initialize`, `session/new`, `session/list`, `session/fork`,
`session/load`, `session/resume`, `session/close`, `session/delete`,
`authenticate`, `logout`, `session/set_model`, `session/set_mode`; on
`session/prompt` streams a few `session/update` notifications (one unknown
kind, one agent_message_chunk, another agent_message_chunk) and answers with
stop_reason `end_turn`. `session/cancel` notifications are ignored.
"""
import json
import sys


def send(payload):
    sys.stdout.write(json.dumps(payload) + "\n")
    sys.stdout.flush()


def reply(req_id, result):
    send({"jsonrpc": "2.0", "id": req_id, "result": result})


def main():
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            frame = json.loads(line)
        except ValueError:
            continue
        method = frame.get("method", "")
        req_id = frame.get("id")
        params = frame.get("params") or {}

        if method == "initialize":
            reply(req_id, {
                "protocolVersion": 1,
                "agentInfo": {"name": "FakeKimi", "version": "0.0.0-test"},
                "agentCapabilities": {"loadSession": True},
                "authMethods": [{"type": "terminal", "id": "login"}],
            })
        elif method == "session/new":
            reply(req_id, {"sessionId": "sess_fake", "configOptions": [], "modes": {}})
        elif method in ("session/load", "session/resume"):
            reply(req_id, {"sessionId": params.get("sessionId", "sess_fake")})
        elif method == "session/fork":
            reply(req_id, {"sessionId": "sess_forked"})
        elif method in ("session/list", "session/set_mode", "session/set_model",
                        "authenticate", "logout", "session/close", "session/delete"):
            reply(req_id, {})
        elif method == "session/prompt":
            session_id = params.get("sessionId", "sess_fake")
            send({"jsonrpc": "2.0", "method": "session/update", "params": {
                "sessionId": session_id,
                "update": {"sessionUpdate": "tool_call", "title": "ignored"},
            }})
            send({"jsonrpc": "2.0", "method": "session/update", "params": {
                "sessionId": session_id,
                "update": {"sessionUpdate": "agent_message_chunk",
                           "content": {"type": "text", "text": "你好"}},
            }})
            send({"jsonrpc": "2.0", "method": "session/update", "params": {
                "sessionId": session_id,
                "update": {"sessionUpdate": "agent_message_chunk",
                           "content": {"type": "text", "text": "世界"}},
            }})
            reply(req_id, {"stopReason": "end_turn"})
        elif method == "session/cancel":
            pass
        elif req_id is not None:
            send({"jsonrpc": "2.0", "id": req_id,
                  "error": {"code": -32601, "message": "method not found: " + method}})


if __name__ == "__main__":
    main()
