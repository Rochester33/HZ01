import machine
import time

buzzer = machine.Pin(16, machine.Pin.OUT)

print("Buzzer ON")
buzzer.value(1)
time.sleep(3)

buzzer.value(0)
print("Buzzer OFF")
