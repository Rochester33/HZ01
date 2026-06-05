# 阈值同步功能说明

## 功能概述

实现了前端→后端→硬件的完整阈值同步机制，确保用户在前端修改阈值后，硬件设备能自动接收并应用新的阈值配置。

---

## 工作流程

### 1. 设备启动时（固件）

```
ESP32 启动 → 连接 WiFi → 调用 fetch_thresholds()
  ↓
GET https://hz01.online/api/v1/alerts/thresholds/device/Device_001
  ↓
加载阈值到内存中的 thresholds 字典
  ↓
使用动态阈值进行告警判断
```

**实现位置**：`firmware/main.py:247-283`

### 2. 用户更新阈值（前端）

```
用户在 Thresholds 页面修改阈值 → 点击保存
  ↓
检查是否有设备在线（无设备则显示警告）
  ↓
PUT https://hz01.online/api/v1/alerts/thresholds
  ↓
显示成功通知（3秒后自动消失）
```

**实现位置**：`frontend/src/main/java/com/hz01/frontend/views/alert/ThresholdView.java:152-195`

### 3. 后端处理（自动推送）

```
接收前端请求 → 更新数据库中的阈值
  ↓
查询所有在线设备（status = "online"）
  ↓
为每个在线设备创建 update_threshold 命令
  ↓
命令内容：{"command_type": "update_threshold", "action": "{...阈值JSON...}"}
  ↓
命令状态设为 "pending"
```

**实现位置**：`backend/app/routers/alerts.py:46-108`

### 4. 设备轮询命令（固件）

```
固件主循环每 5 秒轮询一次
  ↓
GET https://hz01.online/api/v1/commands/pending/Device_001
  ↓
接收到 update_threshold 命令
  ↓
解析 action 字段中的 JSON 阈值数据
  ↓
更新全局 thresholds 字典
  ↓
后续告警判断使用新阈值
  ↓
确认命令执行完成
```

**实现位置**：
- 轮询：`firmware/main.py:247-260`
- 命令处理：`firmware/main.py:154-167`
- 阈值应用：`firmware/main.py:306-333`

---

## 数据格式

### 固件中的阈值格式

```python
thresholds = {
    "temperature": {
        "warning_max": 40,
        "critical_max": 45
    },
    "humidity": {
        "warning_max": 80,
        "critical_max": 95
    },
    "co_level": {
        "warning_max": 2000,
        "critical_max": 3000
    },
    "methane_level": {
        "warning_max": 2000,
        "critical_max": 3000
    }
}
```

### 后端命令格式

```json
{
    "id": 123,
    "device_id": "Device_001",
    "command_type": "update_threshold",
    "action": "{\"temperature\": {\"warning_max\": 40, \"critical_max\": 45}, ...}",
    "status": "pending",
    "created_at": "2026-06-04T12:00:00Z"
}
```

### 后端阈值查询接口响应

```json
[
    {
        "id": 1,
        "device_id": null,
        "sensor_type": "temperature",
        "warning_min": null,
        "warning_max": 40.0,
        "critical_min": null,
        "critical_max": 45.0,
        "unit": "°C"
    },
    ...
]
```

---

## API 接口

### 1. 获取设备阈值
```
GET /api/v1/alerts/thresholds/device/{device_id}
```
返回该设备的所有阈值配置（设备特定 + 全局回退）

### 2. 更新阈值
```
PUT /api/v1/alerts/thresholds
Body: {
    "sensor_type": "temperature",
    "warning_max": 40,
    "critical_max": 45
}
```
更新阈值并自动推送到所有在线设备

### 3. 轮询命令
```
GET /api/v1/commands/pending/{device_id}
```
设备轮询待执行的命令（包括阈值更新命令）

---

## 关键改进点

### 🔄 从硬编码到动态配置

**之前**：
```python
TEMP_THRESHOLD = 40  # 硬编码，无法修改
if temp >= TEMP_THRESHOLD:
    buzzer.value(1)
```

**现在**：
```python
thresholds = {"temperature": {"warning_max": 40, ...}}  # 可动态更新

temp_warn = thresholds.get("temperature", {}).get("warning_max", 40)
if temp >= temp_warn:
    buzzer.value(1)
```

### ✅ 启动时同步

设备启动后立即从服务器拉取最新阈值，避免使用过时配置。

### 📡 实时推送

用户修改阈值后，后端立即为所有在线设备创建更新命令，设备在下次轮询时（最多 5 秒延迟）就能收到。

### 🛡️ 前端验证

前端保存前检查设备是否在线，避免用户误以为阈值已生效。

---

## 测试步骤

### 1. 测试启动时加载阈值

```bash
# 上传固件到 ESP32
# 观察串口输出应显示：
Wi-Fi connected: (...)
Thresholds loaded from server: {'temperature': {...}, ...}
```

### 2. 测试前端更新阈值

1. 访问 https://hz01.online/thresholds
2. 修改温度阈值从 40 改为 35
3. 点击保存
4. 观察右下角通知："阈值已更新"（3秒后消失）

### 3. 测试阈值推送到硬件

1. 在前端修改阈值后，观察 ESP32 串口输出
2. 应该在 5 秒内看到：
   ```
   Thresholds updated: {'temperature': {'warning_max': 35, ...}, ...}
   ```

### 4. 测试阈值生效

1. 将温度阈值改为 25°C（低于室温）
2. 观察 ESP32 的蜂鸣器是否立即响起
3. 将阈值改回 40°C
4. 蜂鸣器应该停止

### 5. 测试无设备警告

1. 确保没有设备在线
2. 在 Thresholds 页面修改并保存
3. 应显示警告："没有连接的设备，阈值设置不会生效"

---

## 故障排查

### 问题：设备启动后阈值未加载

**检查**：
1. ESP32 是否连接 WiFi？
2. 串口输出是否显示 "Failed to fetch thresholds"？
3. 后端 API 是否正常运行？

**解决**：
```bash
# 手动测试 API
curl https://hz01.online/api/v1/alerts/thresholds/device/Device_001
```

### 问题：前端更新后硬件未收到

**检查**：
1. 设备是否在线？（数据库中 status = "online"）
2. 查看数据库命令表是否创建了 update_threshold 命令
3. ESP32 是否正常轮询命令？

**解决**：
```sql
-- 查看设备状态
SELECT device_id, status FROM devices;

-- 查看待执行命令
SELECT * FROM device_commands WHERE command_type = 'update_threshold' AND status = 'pending';
```

### 问题：前端通知不消失

**检查**：
- 浏览器控制台是否有 JavaScript 错误？
- Vaadin 版本是否支持自动关闭？

**解决**：
- 已使用明确的 `setDuration(3000)` 和 `open()` 方法
- 如果仍不消失，可能是浏览器缓存问题，清除缓存后重试

---

## 文件清单

### 修改的文件

1. **firmware/main.py**
   - 阈值从常量改为字典
   - 添加 `fetch_thresholds()` 启动时拉取
   - 添加 `update_threshold` 命令处理
   - 告警判断使用动态阈值

2. **backend/app/routers/alerts.py**
   - 添加 `GET /thresholds/device/{device_id}` 接口
   - `PUT /thresholds` 自动推送到在线设备

3. **frontend/.../ThresholdView.java**
   - 添加设备存在性检查
   - 修复通知自动关闭
   - 注入 `DeviceApiClient`

4. **frontend/.../messages_*.properties**
   - 添加 `threshold.no.devices` 翻译
   - 添加 `threshold.select.sensor` 翻译

---

## 未来改进建议

1. **WebSocket 实时推送**：替代轮询机制，阈值更新后立即推送到设备
2. **设备特定阈值**：支持为不同设备设置不同的阈值
3. **阈值历史记录**：记录阈值变更历史，便于审计
4. **批量更新**：支持一次更新多个传感器的阈值

---

生成时间：2026-06-04
版本：v1.0
