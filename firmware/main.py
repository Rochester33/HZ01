import dht
import machine
import time
import json
import sys

# ── Connection mode ───────────────────────────────────────────────────────────
SERIAL_MODE = False

if not SERIAL_MODE:
    from wifi import connect_wifi, test_backend_connection, BACKEND_URL
    import urequests
    import network

# ── OLED display ──────────────────────────────────────────────────────────────
try:
    from oled import update as oled_update
    _oled_ok = True
except Exception:
    _oled_ok = False

def oled(wifi_ok, ble_ok, upload_ok, recv_ok):
    if _oled_ok:
        try:
            oled_update(wifi_ok, ble_ok, upload_ok, recv_ok)
        except Exception:
            pass

# ── Pin configuration ────────────────────────────────────────────────────────
dht_pin  = machine.Pin(4)
sensor   = dht.DHT11(dht_pin)

buzzer   = machine.Pin(16, machine.Pin.OUT, value=0)
buzzer.value(0)

# Power-indicator LED (active-low on most ESP32 boards; adjust if needed)
led      = machine.Pin(2, machine.Pin.OUT)
led.value(0)

mq4_pin  = machine.ADC(machine.Pin(13))
mq4_pin.atten(machine.ADC.ATTN_11DB)

mq7_pin  = machine.ADC(machine.Pin(14))
mq7_pin.atten(machine.ADC.ATTN_11DB)

# ── Device config ─────────────────────────────────────────────────────────────
DEVICE_ID   = "Device_001"

# ── Alert thresholds (mutable - can be updated via commands) ─────────────────
thresholds = {
    "temperature": {"warning_max": 40, "critical_max": 45},
    "humidity": {"warning_max": 80, "critical_max": 95},
    "co_level": {"warning_max": 2000, "critical_max": 3000},
    "methane_level": {"warning_max": 2000, "critical_max": 3000},
}

# ── State ─────────────────────────────────────────────────────────────────────
buzzer_manual_on = False   # True = manually forced ON; None = auto mode
led_manual_on    = False


# ── Sensor helpers ────────────────────────────────────────────────────────────
def read_dht():
    try:
        sensor.measure()
        return sensor.temperature(), sensor.humidity()
    except OSError:
        return None, None


def read_mq4():
    try:
        return mq4_pin.read()
    except Exception:
        return "Not Detected"


def read_mq7():
    try:
        return mq7_pin.read()
    except Exception:
        return "Not Detected"


# ── SOS pattern: 3 short · 3 long · 3 short ──────────────────────────────────
SHORT_MS = 200
LONG_MS  = 600
GAP_MS   = 200
WORD_GAP = 800

def _pulse(duration_ms):
    """Fire both LED and buzzer for duration_ms, then a GAP_MS pause."""
    led.value(1)
    buzzer.value(1)
    time.sleep_ms(duration_ms)
    led.value(0)
    buzzer.value(0)
    time.sleep_ms(GAP_MS)

def play_sos():
    """Execute one full SOS sequence (·· · — — — ··· ) synchronously."""
    # 3 short
    for _ in range(3):
        _pulse(SHORT_MS)
    time.sleep_ms(WORD_GAP - GAP_MS)   # gap between groups
    # 3 long
    for _ in range(3):
        _pulse(LONG_MS)
    time.sleep_ms(WORD_GAP - GAP_MS)
    # 3 short
    for _ in range(3):
        _pulse(SHORT_MS)


# ── Command execution (shared by both Wi-Fi and Serial modes) ─────────────────
def execute_command(cmd):
    """Execute a single command dict {command_type, action, id?, thresholds?}."""
    global buzzer_manual_on, led_manual_on, thresholds

    cmd_type = cmd.get("command_type")
    action   = cmd.get("action")

    if action == "sos":
        play_sos()

    elif cmd_type == "buzzer":
        if action == "on":
            buzzer_manual_on = True
            buzzer.value(1)
        elif action == "off":
            buzzer_manual_on = False
            buzzer.value(0)
        elif action == "blink":
            for _ in range(5):
                buzzer.value(1)
                time.sleep_ms(500)
                buzzer.value(0)
                time.sleep_ms(500)

    elif cmd_type == "led":
        if action == "on":
            led_manual_on = True
            led.value(1)
        elif action == "off":
            led_manual_on = False
            led.value(0)
        elif action == "blink":
            for _ in range(5):
                led.value(1)
                time.sleep_ms(500)
                led.value(0)
                time.sleep_ms(500)

    elif cmd_type == "update_threshold":
        # Update thresholds from server
        # Backend sends: {"command_type": "update_threshold", "action": "{\"temperature\": {...}, ...}"}
        action_data = cmd.get("action")
        if action_data:
            try:
                # Parse JSON string from action field
                if isinstance(action_data, str):
                    new_thresholds = json.loads(action_data)
                else:
                    new_thresholds = action_data

                if new_thresholds and isinstance(new_thresholds, dict):
                    thresholds.update(new_thresholds)
                    print("Thresholds updated:", thresholds)
            except Exception as e:
                print("Failed to parse thresholds:", e)


# ── Serial-mode: read inbound commands from stdin (REPL port) ────────────────
_serial_buf = bytearray()
_serial_buf_max = 1024  # Maximum buffer size to prevent memory exhaustion

# Try to set up a non-blocking poll on stdin; falls back to None (skip reads)
try:
    import uselect as _uselect
    _stdin_poll = _uselect.poll()
    _stdin_poll.register(sys.stdin, _uselect.POLLIN)
except Exception:
    _stdin_poll = None


def read_serial_commands():
    """
    Non-blocking read of complete JSON lines from stdin.
    serial_forwarder.py writes one JSON command per line, e.g.:
      {"command_type": "buzzer", "action": "on", "id": 42}
    """
    global _serial_buf
    if _stdin_poll is None:
        return
    try:
        while _stdin_poll.poll(0):   # 0 ms = non-blocking; returns [] when no data
            ch = sys.stdin.read(1)
            if not ch:
                break
            if ch == '\n':
                line = _serial_buf.decode('utf-8', 'ignore').strip()
                _serial_buf = bytearray()
                if line:
                    try:
                        cmd = json.loads(line)
                        execute_command(cmd)
                    except ValueError:
                        pass   # not JSON — ignore
            else:
                # Prevent buffer overflow
                if len(_serial_buf) < _serial_buf_max:
                    _serial_buf += ch.encode('utf-8')
                else:
                    # Buffer full, discard and reset
                    print("Serial buffer overflow, resetting")
                    _serial_buf = bytearray()
    except Exception:
        pass


# ── Wi-Fi mode: HTTP helpers ──────────────────────────────────────────────────

def upload_reading(temp, humidity, mq4_value, mq7_value):
    """POST sensor data directly to backend. Returns True on success."""
    payload = {
        "device_id": DEVICE_ID,
        "temperature": temp,
        "humidity": humidity,
        "co_level": mq7_value if isinstance(mq7_value, int) else None,
        "methane_level": mq4_value if isinstance(mq4_value, int) else None,
    }

    # Try with extended timeout and handle common responses
    for attempt in range(2):
        try:
            url = "{}/api/v1/sensors/readings".format(BACKEND_URL)
            resp = urequests.post(
                url,
                json=payload,
                headers={"Content-Type": "application/json"},
                timeout=15,  # Increased from 5s to 15s
            )
            # Accept both 200 and 201 as success
            ok = resp.status_code in (200, 201)
            if not ok:
                print("Upload status: {} (expected 200/201)".format(resp.status_code))
            resp.close()
            return ok
        except OSError as e:
            if attempt == 0:
                print("Upload timeout, retrying...")
                time.sleep(1)
            else:
                print("Upload network error:", e)
            return False
        except Exception as e:
            print("Upload failed:", e)
            return False

def acknowledge_command(cmd_id):
    try:
        resp = urequests.patch(
            "{}/api/v1/commands/{}/acknowledge".format(BACKEND_URL, cmd_id),
            timeout=5,
        )
        resp.close()
    except Exception:
        pass


def poll_commands():
    """Fetch pending commands from server and execute each one. Returns True if any received."""
    try:
        url  = "{}/api/v1/commands/pending/{}".format(BACKEND_URL, DEVICE_ID)
        resp = urequests.get(url, timeout=5)
        cmds = resp.json()
        resp.close()
    except Exception:
        return False

    for cmd in cmds:
        execute_command(cmd)
        acknowledge_command(cmd.get("id"))
    return len(cmds) > 0


def fetch_thresholds():
    """Fetch current thresholds from server on startup."""
    global thresholds
    try:
        url = "{}/api/v1/alerts/thresholds/device/{}".format(BACKEND_URL, DEVICE_ID)
        resp = urequests.get(url, timeout=5)
        data = resp.json()
        resp.close()

        # Convert server format to firmware format
        # Server returns list: [{"sensor_type": "temperature", "warning_max": 40, ...}, ...]
        if data and isinstance(data, list):
            for item in data:
                sensor_type = item.get("sensor_type")
                if sensor_type:
                    thresholds[sensor_type] = {
                        "warning_min": item.get("warning_min"),
                        "warning_max": item.get("warning_max"),
                        "critical_min": item.get("critical_min"),
                        "critical_max": item.get("critical_max"),
                    }
            print("Thresholds loaded from server:", thresholds)
            return True
    except Exception as e:
        print("Failed to fetch thresholds:", e)
    return False


# ── Startup ───────────────────────────────────────────────────────────────────
wifi_connected = False
if not SERIAL_MODE:
    wifi_connected = connect_wifi()
    if wifi_connected:
        test_backend_connection()  # Run connectivity diagnostics
        fetch_thresholds()  # Load thresholds from server

oled(wifi_connected, False, False, False)

# Allow DHT11 to stabilise after power-on before the first read
time.sleep(2)

poll_tick   = 0
upload_ok   = False
recv_ok     = False
reconnect_delay = 0  # Delay counter for WiFi reconnection

# ── Main loop ─────────────────────────────────────────────────────────────────
while True:
    if SERIAL_MODE:
        read_serial_commands()
    else:
        wlan = network.WLAN(network.STA_IF)
        wifi_connected = wlan.isconnected()
        if not wifi_connected:
            if reconnect_delay <= 0:
                print("Wi-Fi lost, reconnecting...")
                wifi_connected = connect_wifi()
                if not wifi_connected:
                    reconnect_delay = 10  # Wait 10 seconds before next retry
            else:
                reconnect_delay -= 1

    temp, humidity = read_dht()
    mq4_value      = read_mq4()
    mq7_value      = read_mq7()

    # Check thresholds using dynamic values
    temp_warn = thresholds.get("temperature", {}).get("warning_max", 40)
    temp_crit = thresholds.get("temperature", {}).get("critical_max", 45)
    humid_warn = thresholds.get("humidity", {}).get("warning_max", 80)
    humid_crit = thresholds.get("humidity", {}).get("critical_max", 95)
    co_warn = thresholds.get("co_level", {}).get("warning_max", 2000)
    methane_warn = thresholds.get("methane_level", {}).get("warning_max", 2000)

    gas_alert = (
        (isinstance(mq4_value, int) and mq4_value >= methane_warn) or
        (isinstance(mq7_value, int) and mq7_value >= co_warn)
    )

    if temp is not None:
        if not buzzer_manual_on:
            auto_on = (
                temp >= temp_warn or
                humidity >= humid_warn or
                gas_alert
            )
            buzzer.value(1 if auto_on else 0)

        if humidity >= humid_crit or gas_alert:
            status = "Warning"
        else:
            status = "Online"
    else:
        status = "Online"

    # Upload and poll commands
    if not SERIAL_MODE:
        upload_ok = upload_reading(temp, humidity, mq4_value, mq7_value)
        poll_tick += 1
        if poll_tick >= 5:
            recv_ok = poll_commands()
            poll_tick = 0
    else:
        print(json.dumps({
            "device_id":   DEVICE_ID,
            "temperature": temp,
            "humidity":    humidity,
            "co_level":    mq7_value,
            "methane_level": mq4_value,
            "status":      status,
        }))

    oled(wifi_connected, False, upload_ok, recv_ok)

    time.sleep(1)
