"""
oled.py — SSD1306 OLED display helper for ESP32 (MicroPython)

Wiring: GND→GND, VCC→3.3V, SCL→GPIO22, SDA→GPIO21
"""

import machine
from ssd1306 import SSD1306_I2C

_i2c   = machine.I2C(0, scl=machine.Pin(22), sda=machine.Pin(21), freq=400000)
_oled  = SSD1306_I2C(128, 64, _i2c)


def update(wifi_ok: bool, ble_ok: bool, upload_ok: bool, recv_ok: bool):
    """Refresh the OLED with current connection and data status."""
    _oled.fill(0)

    # Row 0: Wi-Fi status
    wifi_str = "WiFi:  OK" if wifi_ok else "WiFi:  --"
    _oled.text(wifi_str, 0, 0)

    # Row 1: Bluetooth status
    ble_str = "BLE:   OK" if ble_ok else "BLE:   --"
    _oled.text(ble_str, 0, 12)

    # Divider
    _oled.hline(0, 26, 128, 1)

    # Row 2: Upload status (refreshed every second)
    up_str = "Upload: OK" if upload_ok else "Upload: FAIL"
    _oled.text(up_str, 0, 30)

    # Row 3: Receive status (refreshed every second)
    rx_str = "Recv:   OK" if recv_ok else "Recv:   --"
    _oled.text(rx_str, 0, 44)

    _oled.show()
