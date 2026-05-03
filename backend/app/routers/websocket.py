from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from app.services.websocket_manager import ws_manager

router = APIRouter(tags=["websocket"])


@router.websocket("/ws")
async def ws_global(websocket: WebSocket):
    """Subscribe to all device events."""
    await ws_manager.connect_global(websocket)
    try:
        while True:
            await websocket.receive_text()  # keep connection alive
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket)


@router.websocket("/ws/{device_id}")
async def ws_device(websocket: WebSocket, device_id: str):
    """Subscribe to events for a specific device."""
    await ws_manager.connect_device(websocket, device_id)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        ws_manager.disconnect(websocket, device_id)
