"""
buzzer_off_local.py — 直接在ESP32上运行,立即关闭IO16蜂鸣器

使用方法:
1. 通过串口连接ESP32 (如 Thonny, mpremote, 或 ampy)
2. 运行这个脚本: mpremote run buzzer_off_local.py
   或在REPL中: exec(open('buzzer_off_local.py').read())
"""

import machine

# 配置IO16为输出引脚并设为低电平(关闭)
buzzer = machine.Pin(16, machine.Pin.OUT)
buzzer.value(0)

print("蜂鸣器已关闭 (IO16 = LOW)")
