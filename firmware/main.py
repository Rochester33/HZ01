import dht
import machine
import time
import json

dht_pin = machine.Pin(4)
sensor = dht.DHT11(dht_pin)

buzzer = machine.Pin(16, machine.Pin.OUT)
buzzer.value(0)

mq4_pin = machine.ADC(machine.Pin(13))
mq4_pin.atten(machine.ADC.ATTN_11DB)

mq7_pin = machine.ADC(machine.Pin(14))
mq7_pin.atten(machine.ADC.ATTN_11DB)

DEVICE_ID = "Device_001"
TEMP_THRESHOLD = 40
HUMIDITY_THRESHOLD = 80
HUMIDITY_WARNING = 95
MQ4_THRESHOLD = 2000
MQ7_THRESHOLD = 2000


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


while True:
    temp, humidity = read_dht()
    mq4_value = read_mq4()
    mq7_value = read_mq7()

    if temp is None:
        print(json.dumps({"error": "DHT11 read failed"}))
    else:
        gas_alert = (isinstance(mq4_value, int) and mq4_value >= MQ4_THRESHOLD) or \
                    (isinstance(mq7_value, int) and mq7_value >= MQ7_THRESHOLD)

        if temp >= TEMP_THRESHOLD or humidity >= HUMIDITY_THRESHOLD or gas_alert:
            buzzer.value(1)
        else:
            buzzer.value(0)

        if humidity >= HUMIDITY_WARNING or gas_alert:
            status = "Warning"
        else:
            status = "Online"

        print(json.dumps({
            "device_id": DEVICE_ID,
            "temperature": temp,
            "humidity": humidity,
            "methane": mq4_value,
            "co": mq7_value,
            "status": status
        }))

    time.sleep(2)
