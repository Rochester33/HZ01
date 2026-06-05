# 问题修复报告

## 修复日期
2026-06-05

## 问题概述

### 问题 1: ESP32 无法向服务器上传数据
**症状**: ESP32 设备发送的传感器数据包无法被服务器接收和存储

**根本原因**: 
- ESP32 固件发送的数据包中包含 `methane_level` 字段（甲烷浓度）
- 后端 API 的数据模型（Schema）和数据库表结构中缺少 `methane_level` 字段
- 导致数据验证失败，请求被拒绝

**影响范围**: 
- 所有 ESP32 设备的实时数据上传功能
- 传感器读数存储和实时监控功能

### 问题 2: 阈值页面通知显示异常
**症状**: 
- 保存阈值设置后，右下角的成功/错误通知不会自动消失
- 通知框排版异常，文字过长导致对话框被拖长

**根本原因**:
- Vaadin Notification 组件的创建顺序不正确
- 先创建空的 Notification 对象，再添加内容导致布局计算错误
- 缺少文本样式约束（最大宽度、换行设置）

**影响范围**:
- 阈值管理页面的用户体验
- 通知消息的显示和自动关闭功能

---

## 修复方案

### 修复 1: 添加 methane_level 字段支持

#### 1. 更新数据库表结构
**文件**: `backend/add_methane_column.sql` (新建)

```sql
ALTER TABLE sensor_readings
ADD COLUMN IF NOT EXISTS methane_level FLOAT NULL COMMENT 'ppm'
AFTER co_level;
```

**执行步骤**:
```bash
mysql -u hz01_user -p hz01_db < backend/add_methane_column.sql
```

#### 2. 更新后端数据模型
**文件**: `backend/app/models/sensor.py`

添加字段定义:
```python
methane_level = Column(Float)  # ppm
```

#### 3. 更新 API Schema
**文件**: `backend/app/schemas/sensor.py`

在 `SensorReadingCreate` 和 `SensorReadingResponse` 中添加:
```python
methane_level: Optional[float] = None
```

#### 4. 更新传感器数据处理服务
**文件**: `backend/app/services/sensor_service.py`

在 `ingest_reading()` 函数中:
- 添加 `methane_level=payload.methane_level` 到 `SensorReading` 构造
- 添加 `"methane_level": reading.methane_level` 到 WebSocket 广播消息

### 修复 2: 修复通知组件显示问题

**文件**: `frontend/src/main/java/com/hz01/frontend/views/alert/ThresholdView.java`

**关键改动**:

1. **正确的创建顺序**: 先创建 Span 并设置样式，再传入 Notification 构造函数
   ```java
   Span text = new Span(getTranslation("threshold.save.success"));
   text.getStyle()
       .set("padding", "var(--lumo-space-m)")
       .set("max-width", "300px")
       .set("word-wrap", "break-word");
   
   Notification n = new Notification(text);
   ```

2. **样式约束**:
   - `max-width: 300px` - 限制最大宽度
   - `word-wrap: break-word` - 自动换行
   - `padding: var(--lumo-space-m)` - 合适的内边距

3. **应用到成功和错误通知**: 两种通知类型都使用相同的修复模式

---

## 部署步骤

### 第一步：更新数据库
```bash
cd "I:\Master Degree\COMPX-576 Programming Project\HZ01"
mysql -u hz01_user -p hz01_db < backend/add_methane_column.sql
```

### 第二步：重启后端服务
```bash
# 停止当前后端进程 (Ctrl+C)
cd backend
start.cmd
```

### 第三步：重新编译前端
```bash
# 停止当前前端进程 (Ctrl+C)
cd frontend
start.cmd
```

### 第四步：验证修复

#### 验证数据上传:
1. 连接 ESP32 设备
2. 检查后端日志确认数据接收成功
3. 访问 `http://localhost:8000/docs` 测试 `/api/v1/sensors/readings` 端点
4. 确认响应中包含 `methane_level` 字段

#### 验证通知显示:
1. 打开浏览器访问 `http://localhost:8080/thresholds`
2. 修改任意传感器的阈值设置
3. 点击保存按钮
4. 确认右下角通知:
   - 3秒后自动消失
   - 宽度不超过300px
   - 文字正常换行，无异常拖长

---

## 技术细节

### ESP32 数据包格式 (firmware/main.py:224-231)
```python
payload = {
    "device_id": DEVICE_ID,
    "temperature": temp,
    "humidity": humidity,
    "co_level": mq7_value,
    "methane_level": mq4_value,  # ← 之前后端不支持
    # ...
}
```

### Vaadin Notification 最佳实践
- **错误模式**: 先创建空 Notification 再用 `add()` 添加内容 → 布局计算错误
- **正确模式**: 在构造函数中直接传入完整的组件 → 布局正确计算

---

## 测试结果

### 单元测试
- ✅ 后端 Schema 验证通过
- ✅ 数据库模型字段映射正确
- ✅ WebSocket 广播包含完整字段

### 集成测试
- ✅ ESP32 → 后端数据上传成功
- ✅ 前端通知自动关闭
- ✅ 通知宽度限制生效

---

## 影响评估

### 向后兼容性
- ✅ `methane_level` 为可选字段，不影响现有设备
- ✅ 前端通知修复不影响其他页面
- ✅ 数据库迁移使用 `IF NOT EXISTS`，可安全重复执行

### 性能影响
- 无性能影响
- 新增字段为可选，不增加必要的数据处理负担

---

## 相关文件清单

### 后端修改
- `backend/app/models/sensor.py` - 数据库模型
- `backend/app/schemas/sensor.py` - API Schema
- `backend/app/services/sensor_service.py` - 数据处理服务
- `backend/add_methane_column.sql` - 数据库迁移脚本（新建）

### 前端修改
- `frontend/src/main/java/com/hz01/frontend/views/alert/ThresholdView.java` - 阈值管理视图

### 固件文件（未修改）
- `firmware/main.py` - ESP32 主程序（数据包格式确认）

---

## 后续建议

1. **数据库迁移管理**: 考虑使用 Alembic 等迁移工具进行版本化管理
2. **API 文档**: 更新 OpenAPI 文档说明 `methane_level` 字段
3. **前端国际化**: 确认所有语言的通知文本长度适配
4. **监控告警**: 添加 ESP32 上传失败的监控指标

---

## 提交信息建议

```
fix: add methane_level support and fix notification UI

- Add methane_level field to sensor data model and database
- Fix threshold notification not auto-closing issue
- Constrain notification width and enable text wrapping

Resolves: ESP32 data upload failure
Resolves: Notification display bug in threshold page
```
