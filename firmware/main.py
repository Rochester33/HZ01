import dht
import machine
import time
import json

dht_pin = machine.Pin(4)
sensor = dht.DHT11(dht_pin)

buzzer = machine.Pin(16, machine.Pin.OUT)
buzzer.value(0)

DEVICE_ID = "Device_001"
TEMP_THRESHOLD = 40
HUMIDITY_THRESHOLD = 80
HUMIDITY_WARNING = 95


def read_dht():
    try:
        sensor.measure()
        return sensor.temperature(), sensor.humidity()
    except OSError:
        return None, None


while True:
    temp, humidity = read_dht()

    if temp is None:
        print(json.dumps({"error": "DHT11 read failed"}))
    else:
        if temp >= TEMP_THRESHOLD or humidity >= HUMIDITY_THRESHOLD:
            buzzer.value(1)
        else:
            buzzer.value(0)

        if humidity >= HUMIDITY_WARNING:
            status = "warning"
        else:
            status = "online"

        print(json.dumps({
            "device_id": DEVICE_ID,
            "temperature": temp,
            "humidity": humidity,
            "status": status
        }))

    time.sleep(2)
