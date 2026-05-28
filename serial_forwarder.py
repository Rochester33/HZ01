"""
Serial Forwarder - Reads JSON data from USB serial port and forwards it to hz01.online
Also polls the server for pending commands and writes them back to the ESP32 via serial.

Usage:
  python serial_forwarder.py --port COM3 --baud 115200
  python serial_forwarder.py --list
  python serial_forwarder.py --port /dev/ttyUSB0 --baud 9600 --retry 10

Examples:
  # List all available serial ports
  python serial_forwarder.py --list

  # Forward from COM3 at default baud rate (115200)
  python serial_forwarder.py --port COM3

  # Forward from a Linux USB serial device at 9600 baud, retry every 10 s
  python serial_forwarder.py --port /dev/ttyUSB0 --baud 9600 --retry 10
"""

import argparse
import json
import logging
import time
import threading
from datetime import datetime, timezone

import requests
import serial
import serial.tools.list_ports

API_BASE   = "https://hz01.online/api/v1"
API_URL    = f"{API_BASE}/sensors/readings"
TIMEOUT    = 5   # HTTP request timeout in seconds
DEVICE_ID  = "Device_001"
CMD_POLL_INTERVAL = 1  # seconds between command polls

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


def list_ports():
    ports = serial.tools.list_ports.comports()
    if not ports:
        print("No serial devices detected.")
        return
    print("Available serial ports:")
    for p in ports:
        print(f"  {p.device} - {p.description}")


def parse_line(line: str) -> dict | None:
    """Parse one line of serial data and return an API-compatible dict, or None on failure."""
    line = line.strip()
    if not line:
        return None
    try:
        data = json.loads(line)
    except json.JSONDecodeError:
        log.warning("Non-JSON data, skipping: %s", line)
        return None

    if "device_id" not in data:
        log.warning("Missing 'device_id' field, skipping: %s", line)
        return None

    # If the device did not supply a timestamp, add the current UTC time
    if "recorded_at" not in data:
        data["recorded_at"] = datetime.now(timezone.utc).isoformat()

    return data


def forward(payload: dict) -> bool:
    """POST sensor data to the cloud API. Returns True on success."""
    try:
        resp = requests.post(API_URL, json=payload, timeout=TIMEOUT)
        if resp.status_code == 201:
            log.info("Upload OK  device=%s", payload.get("device_id"))
            return True
        else:
            log.error("Upload failed  HTTP %d: %s", resp.status_code, resp.text[:200])
            return False
    except requests.exceptions.ConnectionError:
        log.error("Cannot connect to server: %s", API_URL)
        return False
    except requests.exceptions.Timeout:
        log.error("Request timed out.")
        return False


def fetch_pending_commands(device_id: str) -> list:
    """Fetch pending commands from the server for the given device."""
    url = f"{API_BASE}/commands/pending/{device_id}"
    try:
        resp = requests.get(url, timeout=TIMEOUT)
        if resp.status_code == 200:
            return resp.json()
        else:
            log.error("Command fetch failed  HTTP %d", resp.status_code)
            return []
    except requests.exceptions.ConnectionError:
        log.error("Cannot connect to server to fetch commands.")
        return []
    except requests.exceptions.Timeout:
        log.error("Command fetch request timed out.")
        return []


def acknowledge_command(cmd_id: int) -> None:
    """Acknowledge a command as executed."""
    url = f"{API_BASE}/commands/{cmd_id}/acknowledge"
    try:
        requests.patch(url, timeout=TIMEOUT)
    except Exception:
        pass


def send_commands_to_device(ser: serial.Serial, commands: list) -> None:
    """Write each command as a JSON line to the serial port."""
    for cmd in commands:
        cmd_id   = cmd.get("id")
        cmd_type = cmd.get("command_type")
        action   = cmd.get("action")

        payload = json.dumps({"command_type": cmd_type, "action": action, "id": cmd_id})
        try:
            ser.write((payload + "\n").encode("utf-8"))
            log.info("Sent command to device: type=%s action=%s id=%s", cmd_type, action, cmd_id)
        except serial.SerialException as e:
            log.error("Failed to write command to serial: %s", e)


def command_poller(ser_ref: list, device_id: str, stop_event: threading.Event) -> None:
    """Background thread: poll for commands every CMD_POLL_INTERVAL seconds."""
    while not stop_event.is_set():
        time.sleep(CMD_POLL_INTERVAL)
        if stop_event.is_set():
            break
        commands = fetch_pending_commands(device_id)
        if commands:
            ser = ser_ref[0]
            if ser and ser.is_open:
                send_commands_to_device(ser, commands)
                # Acknowledge each command after sending
                for cmd in commands:
                    acknowledge_command(cmd.get("id"))


def run(port: str, baud: int, retry_interval: int):
    log.info("Opening serial port %s @ %d baud", port, baud)
    stop_event = threading.Event()
    # Use a list so the poller thread always has a reference to the current Serial object
    ser_ref = [None]

    poller_thread = threading.Thread(
        target=command_poller,
        args=(ser_ref, DEVICE_ID, stop_event),
        daemon=True,
    )
    poller_thread.start()
    log.info("Command poller started (polling every %d s for device %s)", CMD_POLL_INTERVAL, DEVICE_ID)

    while True:
        try:
            with serial.Serial(port, baud, timeout=1) as ser:
                ser_ref[0] = ser
                log.info("Serial port open, listening...")
                while True:
                    raw = ser.readline()
                    if not raw:
                        continue
                    try:
                        line = raw.decode("utf-8", errors="replace")
                    except Exception:
                        continue
                    payload = parse_line(line)
                    if payload:
                        forward(payload)
        except serial.SerialException as e:
            ser_ref[0] = None
            log.error("Serial error: %s — retrying in %d s...", e, retry_interval)
            time.sleep(retry_interval)
        except KeyboardInterrupt:
            log.info("Stopped.")
            stop_event.set()
            break


def main():
    parser = argparse.ArgumentParser(
        description="Serial ↔ hz01.online forwarder (sensor upload + command relay)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
examples:
  python serial_forwarder.py --list
  python serial_forwarder.py --port COM3
  python serial_forwarder.py --port COM3 --baud 9600
  python serial_forwarder.py --port /dev/ttyUSB0 --baud 115200 --retry 10
        """,
    )
    parser.add_argument("--port",   help="Serial port, e.g. COM3 or /dev/ttyUSB0")
    parser.add_argument("--baud",   type=int, default=115200, help="Baud rate (default: 115200)")
    parser.add_argument("--list",   action="store_true", help="List available serial ports and exit")
    parser.add_argument("--retry",  type=int, default=5,   help="Reconnect interval in seconds (default: 5)")
    parser.add_argument("--device", default=DEVICE_ID,    help=f"Device ID to poll commands for (default: {DEVICE_ID})")
    args = parser.parse_args()

    if args.list:
        list_ports()
        return

    if not args.port:
        parser.error("Please specify --port, or use --list to see available ports.")

    # Allow overriding DEVICE_ID from CLI
    global DEVICE_ID
    DEVICE_ID = args.device

    run(args.port, args.baud, args.retry)


if __name__ == "__main__":
    main()
