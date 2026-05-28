"""
wifi.py — Wi-Fi connection helper for ESP32 (MicroPython)

Usage:
    from wifi import connect_wifi, BACKEND_URL
    connected = connect_wifi()
"""

import network
import time

# ── Wi-Fi credentials ─────────────────────────────────────────────────────────
WIFI_SSID   = "YOUR_SSID"
WIFI_PASS   = "YOUR_PASSWORD"

# Backend REST API base URL (reachable over Wi-Fi)
BACKEND_URL = "http://192.168.1.100:8000"   # update to your server IP


def connect_wifi(timeout_s: float = 10.0) -> bool:
    """Connect to the configured Wi-Fi network.

    Args:
        timeout_s: Maximum seconds to wait for connection (default 10 s).

    Returns:
        True if connected successfully, False otherwise.
    """
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)

    if wlan.isconnected():
        return True

    wlan.connect(WIFI_SSID, WIFI_PASS)

    steps = int(timeout_s / 0.5)
    for _ in range(steps):
        if wlan.isconnected():
            print("Wi-Fi connected:", wlan.ifconfig())
            return True
        time.sleep(0.5)

    print("Wi-Fi connection failed")
    return False
