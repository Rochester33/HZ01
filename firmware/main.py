import dht
import machine
import time
import json
import urequests

# ── Connection mode: uncomment ONE of the two lines below ─────────────────────
from wifi import connect_wifi, BACKEND_URL   # Wi-Fi (default)
# from bluetooth import connect_bluetooth, send_data, receive_command  # BLE alternative

# ── Pin configuration ────────────────────────────────────────────────────────
dht_pin  = machine.Pin(4)
sensor   = dht.DHT11(dht_pin)

buzzer   = machine.Pin(16, machine.Pin.OUT)
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

# ── Alert thresholds ──────────────────────────────────────────────────────────
TEMP_THRESHOLD     = 40
HUMIDITY_THRESHOLD = 80
HUMIDITY_WARNING   = 95
MQ4_THRESHOLD      = 2000
MQ7_THRESHOLD      = 2000

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


# ── Command polling ───────────────────────────────────────────────────────────
def acknowledge_command(cmd_id):
    try:
        urequests.patch(
            "{}/api/v1/commands/{}/acknowledge".format(BACKEND_URL, cmd_id)
        ).close()
    except Exception:
        pass


def poll_commands():
    """Fetch pending commands and execute each one."""
    global buzzer_manual_on, led_manual_on
    try:
        url  = "{}/api/v1/commands/pending/{}".format(BACKEND_URL, DEVICE_ID)
        resp = urequests.get(url)
        cmds = resp.json()
        resp.close()
    except Exception:
        return

    for cmd in cmds:
        cmd_type = cmd.get("command_type")
        action   = cmd.get("action")
        cmd_id   = cmd.get("id")

        if action == "sos":
            # SOS overrides both channels; play the pattern once
            play_sos()

        elif cmd_type == "buzzer":
            if action == "on":
                buzzer_manual_on = True
                buzzer.value(1)
            elif action == "off":
                buzzer_manual_on = False
                buzzer.value(0)
            elif action == "blink":
                # Simple blink: 5 × 500 ms
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

        acknowledge_command(cmd_id)


# ── Main loop ─────────────────────────────────────────────────────────────────
connect_wifi()

poll_tick = 0   # poll commands every ~5 s (5 × 1 s sleep iterations)

while True:
    temp, humidity = read_dht()
    mq4_value      = read_mq4()
    mq7_value      = read_mq7()

    if temp is None:
        print(json.dumps({"error": "DHT11 read failed"}))
    else:
        gas_alert = (
            (isinstance(mq4_value, int) and mq4_value >= MQ4_THRESHOLD) or
            (isinstance(mq7_value, int) and mq7_value >= MQ7_THRESHOLD)
        )

        # Auto buzzer: only when NOT manually controlled
        if not buzzer_manual_on:
            auto_on = (
                temp >= TEMP_THRESHOLD or
                humidity >= HUMIDITY_THRESHOLD or
                gas_alert
            )
            buzzer.value(1 if auto_on else 0)

        if humidity >= HUMIDITY_WARNING or gas_alert:
            status = "Warning"
        else:
            status = "Online"

        print(json.dumps({
            "device_id":   DEVICE_ID,
            "temperature": temp,
            "humidity":    humidity,
            "methane":     mq4_value,
            "co":          mq7_value,
            "status":      status
        }))

    # Poll for remote commands every 5 iterations (~5 s)
    poll_tick += 1
    if poll_tick >= 5:
        poll_commands()
        poll_tick = 0

    time.sleep(1)
