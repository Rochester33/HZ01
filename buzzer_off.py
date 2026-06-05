"""
buzzer_off.py — Send a buzzer OFF command to Device_001 via the hz01.online API.

Usage:
  python buzzer_off.py
  python buzzer_off.py --device Device_002
  python buzzer_off.py --url http://localhost:8000
"""

import argparse
import requests

API_BASE  = "https://hz01.online/api/v1"
DEVICE_ID = "Device_001"
TIMEOUT   = 5


def buzzer_off(device_id: str, api_base: str) -> None:
    url = f"{api_base}/commands/"
    payload = {
        "device_id":    device_id,
        "command_type": "buzzer",
        "action":       "off",
        "duration":     0,
    }
    try:
        resp = requests.post(url, json=payload, timeout=TIMEOUT)
        if resp.status_code == 201:
            print(f"[OK] Buzzer OFF command sent to {device_id} (id={resp.json().get('id')})")
        else:
            print(f"[ERROR] HTTP {resp.status_code}: {resp.text[:200]}")
    except requests.exceptions.ConnectionError:
        print(f"[ERROR] Cannot connect to {api_base}")
    except requests.exceptions.Timeout:
        print("[ERROR] Request timed out.")


def main():
    parser = argparse.ArgumentParser(description="Send buzzer OFF command to a device")
    parser.add_argument("--device", default=DEVICE_ID, help=f"Device ID (default: {DEVICE_ID})")
    parser.add_argument("--url",    default=API_BASE,  help=f"Backend API base URL (default: {API_BASE})")
    args = parser.parse_args()
    buzzer_off(args.device, args.url)


if __name__ == "__main__":
    main()
