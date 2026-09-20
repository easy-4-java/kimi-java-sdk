#!/usr/bin/env python3
"""Fake Kimi ACP agent for end-to-end and lifecycle-hardening tests.

Optional first argument selects behavior:
  normal           normal ACP replies
  delay-prompt     hold a prompt open long enough to test same-session admission
  malformed-prompt emit malformed JSON then stay alive
  exit-on-prompt   exit the process while a prompt is pending
  hang-list        never answer session/list
  hang-prompt      never answer session/prompt
  concurrent-prompts answer two sessions from background threads
  stubborn-close   ignore SIGTERM and stay alive after stdin EOF
"""
import json
import sys
import time
import signal
import threading


MODE = sys.argv[1] if len(sys.argv) > 1 else "normal"
WRITE_LOCK = threading.Lock()

if MODE == "stubborn-close":
    signal.signal(signal.SIGTERM, signal.SIG_IGN)


def send(payload):
    with WRITE_LOCK:
        sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
        sys.stdout.flush()


def reply(req_id, result):
    send({"jsonrpc": "2.0", "id": req_id, "result": result})


def send_normal_prompt(session_id, req_id):
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


def send_concurrent_prompt(session_id, req_id):
    if session_id.endswith("A"):
        time.sleep(0.08)
        pieces = ["A-1", "A-2"]
    else:
        time.sleep(0.02)
        pieces = ["B-1", "B-2"]
    for piece in pieces:
        send({"jsonrpc": "2.0", "method": "session/update", "params": {
            "sessionId": session_id,
            "update": {"sessionUpdate": "agent_message_chunk",
                       "content": {"type": "text", "text": piece}},
        }})
        time.sleep(0.01)
    reply(req_id, {"stopReason": "end_turn"})


def main():
    workers = []
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
        elif method == "session/list" and MODE == "hang-list":
            time.sleep(5)
        elif method in ("session/list", "session/set_mode", "session/set_model",
                        "authenticate", "logout", "session/close", "session/delete"):
            reply(req_id, {})
        elif method == "session/prompt":
            session_id = params.get("sessionId", "sess_fake")
            if MODE == "delay-prompt":
                time.sleep(3)
                send_normal_prompt(session_id, req_id)
            elif MODE == "malformed-prompt":
                sys.stdout.write("{not-json\n")
                sys.stdout.flush()
                time.sleep(5)
            elif MODE == "exit-on-prompt":
                sys.exit(7)
            elif MODE == "hang-prompt":
                time.sleep(5)
            elif MODE == "concurrent-prompts":
                worker = threading.Thread(target=send_concurrent_prompt,
                                          args=(session_id, req_id))
                worker.daemon = True
                worker.start()
                workers.append(worker)
            else:
                send_normal_prompt(session_id, req_id)
        elif method == "session/cancel":
            pass
        elif req_id is not None:
            send({"jsonrpc": "2.0", "id": req_id,
                  "error": {"code": -32601, "message": "method not found: " + method}})

    for worker in workers:
        worker.join(timeout=1)

    if MODE == "stubborn-close":
        while True:
            time.sleep(1)


if __name__ == "__main__":
    main()
