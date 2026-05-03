import asyncio
import json
from typing import Dict, Set
from fastapi import WebSocket


class ConnectionManager:
    def __init__(self):
        # device_id -> set of websockets subscribed to that device
        self._device_subs: Dict[str, Set[WebSocket]] = {}
        # websockets subscribed to all devices
        self._global_subs: Set[WebSocket] = set()

    async def connect_global(self, ws: WebSocket):
        await ws.accept()
        self._global_subs.add(ws)

    async def connect_device(self, ws: WebSocket, device_id: str):
        await ws.accept()
        self._device_subs.setdefault(device_id, set()).add(ws)

    def disconnect(self, ws: WebSocket, device_id: str | None = None):
        self._global_subs.discard(ws)
        if device_id:
            subs = self._device_subs.get(device_id, set())
            subs.discard(ws)

    async def broadcast(self, payload: dict, device_id: str | None = None):
        message = json.dumps(payload, default=str)
        targets: Set[WebSocket] = set(self._global_subs)
        if device_id:
            targets |= self._device_subs.get(device_id, set())
        dead: Set[WebSocket] = set()
        for ws in targets:
            try:
                await ws.send_text(message)
            except Exception:
                dead.add(ws)
        for ws in dead:
            self.disconnect(ws, device_id)


ws_manager = ConnectionManager()
