"""
wifi.py — Wi-Fi connection helper for ESP32 (MicroPython)

Usage:
    from wifi import connect_wifi, BACKEND_URL, test_backend_connection
    connected = connect_wifi()
    if connected:
        test_backend_connection()
"""

import network
import time
import socket

# ── Wi-Fi credentials ─────────────────────────────────────────────────────────
WIFI_SSID   = "Karasu"
WIFI_PASS   = "Cz10110703!"

# Backend REST API base URL (reachable over Wi-Fi)
BACKEND_URL = "https://hz01.online"


def _status_name(code: int) -> str:
    """Translate WiFi status code to human-readable string."""
    status_map = {
        1000: "IDLE",
        1001: "CONNECTING",
        1010: "GOT_IP",
        201:  "WRONG_PASSWORD/AUTH_FAIL",
        202:  "NO_AP_FOUND",
        203:  "CONNECT_FAIL",
        204:  "TIMEOUT",
        205:  "HANDSHAKE_TIMEOUT",
    }
    return status_map.get(code, "UNKNOWN({})".format(code))


def connect_wifi(timeout_s: float = 15.0, retry_count: int = 3) -> bool:
    """
    Connect to WiFi with retry mechanism.

    Args:
        timeout_s: Timeout per attempt in seconds
        retry_count: Number of connection attempts

    Returns:
        True if connected, False otherwise
    """
    wlan = network.WLAN(network.STA_IF)

    # Scan for available networks
    print("Scanning WiFi networks...")
    wlan.active(True)
    try:
        networks = wlan.scan()
        target_found = False
        for net in networks:
            ssid = net[0].decode('utf-8')
            if ssid == WIFI_SSID:
                target_found = True
                print("  Found '{}': RSSI={}, Channel={}, Auth={}".format(
                    ssid, net[3], net[2], net[4]
                ))
                break
        if not target_found:
            print("  WARNING: SSID '{}' not found in scan!".format(WIFI_SSID))
    except Exception as e:
        print("  Scan failed:", e)

    for attempt in range(retry_count):
        # Reset the interface to clear any invalid internal state
        wlan.active(False)
        time.sleep(0.5)
        wlan.active(True)

        if wlan.isconnected():
            print("Wi-Fi already connected:", wlan.ifconfig())
            return True

        print("Wi-Fi connecting... (attempt {}/{})".format(attempt + 1, retry_count))
        print("  SSID: '{}', Pass length: {}".format(WIFI_SSID, len(WIFI_PASS)))
        wlan.connect(WIFI_SSID, WIFI_PASS)

        steps = int(timeout_s / 0.5)
        for _ in range(steps):
            if wlan.isconnected():
                print("Wi-Fi connected:", wlan.ifconfig())
                return True
            # Print connection status during wait
            status = wlan.status()
            if _ % 4 == 0:  # Print every 2 seconds
                print("  Status: {}".format(_status_name(status)))
            time.sleep(0.5)

        status = wlan.status()
        print("Wi-Fi attempt {} failed".format(attempt + 1))
        print("  Final status: {}".format(_status_name(status)))

        if status == 201:
            print("  → Check: Router auth mode (use WPA2-PSK, not WPA3)")
            print("  → Check: Password is correct")
            print("  → Check: MAC filtering disabled")
        elif status == 202:
            print("  → Check: SSID is correct and router is powered on")
            print("  → Check: ESP32 is in range")

        if attempt < retry_count - 1:
            time.sleep(2)  # Wait before retry

    print("Wi-Fi connection failed after {} attempts".format(retry_count))
    return False


def test_backend_connection():
    """Test connectivity to backend server with detailed diagnostics."""
    print("\n=== Backend Connectivity Test ===")

    # Test 1: DNS resolution
    print("1. DNS Resolution Test:")
    try:
        host = BACKEND_URL.replace("https://", "").replace("http://", "").split("/")[0]
        print("   Resolving: {}".format(host))
        addr_info = socket.getaddrinfo(host, 443)
        ip = addr_info[0][-1][0]
        print("   ✓ Resolved to: {}".format(ip))
    except Exception as e:
        print("   ✗ DNS failed:", e)
        return False

    # Test 2: TCP connection
    print("2. TCP Connection Test:")
    try:
        s = socket.socket()
        s.settimeout(10)
        s.connect((ip, 443))
        print("   ✓ TCP connection to {}:443 successful".format(ip))
        s.close()
    except Exception as e:
        print("   ✗ TCP connection failed:", e)
        return False

    # Test 3: HTTP request
    print("3. HTTPS Request Test:")
    try:
        import urequests
        url = "{}/api/v1/sensors/readings".format(BACKEND_URL)
        print("   Testing: {}".format(url))
        resp = urequests.get(BACKEND_URL, timeout=10)
        print("   ✓ HTTP status: {}".format(resp.status_code))
        resp.close()
    except Exception as e:
        print("   ✗ HTTPS request failed:", e)
        return False

    print("=== All tests passed ===\n")
    return True
