"""
bluetooth.py — Bluetooth connection helper for ESP32 (MicroPython)

Usage:
    from bluetooth import connect_bluetooth, receive_command

Note:
    Requires a MicroPython build with bluetooth / aioble support.
    On standard ESP32 firmware, use the 'ubluetooth' module.
"""

import ubluetooth
import time
import json

# ── BLE configuration ─────────────────────────────────────────────────────────
DEVICE_NAME    = "ESP32_HZ01"

# UUIDs — replace with your own if needed
SERVICE_UUID   = ubluetooth.UUID("12345678-1234-5678-1234-56789abcdef0")
CHAR_RX_UUID   = ubluetooth.UUID("12345678-1234-5678-1234-56789abcdef1")  # write from central
CHAR_TX_UUID   = ubluetooth.UUID("12345678-1234-5678-1234-56789abcdef2")  # notify to central

# ── Internal state ────────────────────────────────────────────────────────────
_ble            = None
_connected      = False
_rx_buffer      = []   # incoming raw bytes from central


def _irq_handler(event, data):
    """Central BLE IRQ dispatcher."""
    global _connected

    if event == ubluetooth.IRQ_CENTRAL_CONNECT:
        _connected = True
        print("BLE: central connected")

    elif event == ubluetooth.IRQ_CENTRAL_DISCONNECT:
        _connected = False
        print("BLE: central disconnected — advertising again")
        _start_advertising()

    elif event == ubluetooth.IRQ_GATTS_WRITE:
        # A central wrote to the RX characteristic
        conn_handle, attr_handle = data
        value = _ble.gatts_read(attr_handle)
        if value:
            _rx_buffer.append(bytes(value))


def _start_advertising():
    """Begin BLE advertisements so a central can find this device."""
    # Encode name into advertising payload
    name_bytes  = DEVICE_NAME.encode()
    adv_payload = (
        bytes([2, 0x01, 0x06]) +                          # Flags: LE General Discoverable
        bytes([len(name_bytes) + 1, 0x09]) + name_bytes   # Complete Local Name
    )
    _ble.gap_advertise(100_000, adv_data=adv_payload)     # interval ≈ 100 ms
    print("BLE: advertising as '{}'".format(DEVICE_NAME))


def connect_bluetooth() -> bool:
    """Initialise BLE, register GATT service, and start advertising.

    Returns:
        True once the stack is active and advertising (a central may not be
        connected yet — use is_connected() to check pairing status).
    """
    global _ble

    try:
        _ble = ubluetooth.BLE()
        _ble.active(True)
        _ble.irq(_irq_handler)

        # Register a simple UART-style service
        ((rx_handle, tx_handle),) = _ble.gatts_register_services((
            (SERVICE_UUID, (
                (CHAR_RX_UUID, ubluetooth.FLAG_WRITE),
                (CHAR_TX_UUID, ubluetooth.FLAG_NOTIFY),
            )),
        ))

        # Store handles for later use
        global _rx_handle, _tx_handle
        _rx_handle = rx_handle
        _tx_handle = tx_handle

        _start_advertising()
        return True

    except Exception as exc:
        print("BLE init failed:", exc)
        return False


def is_connected() -> bool:
    """Return True if a central is currently connected."""
    return _connected


def send_data(payload: dict) -> bool:
    """Notify the connected central with a JSON-encoded payload.

    Args:
        payload: Dictionary to serialise and send.

    Returns:
        True if notification was sent, False if no central is connected.
    """
    if not _connected or _ble is None:
        return False
    try:
        _ble.gatts_notify(0, _tx_handle, json.dumps(payload).encode())
        return True
    except Exception as exc:
        print("BLE send error:", exc)
        return False


def receive_command() -> dict | None:
    """Pop and return the oldest command received from the central, or None.

    The central is expected to write a JSON object, e.g.:
        {"command_type": "buzzer", "action": "on", "id": 42}
    """
    if not _rx_buffer:
        return None
    raw = _rx_buffer.pop(0)
    try:
        return json.loads(raw.decode())
    except Exception:
        print("BLE: failed to parse command:", raw)
        return None
