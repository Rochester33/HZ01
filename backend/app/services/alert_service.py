from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Session
from app.models.alert import AlertThreshold, AlertEvent
from app.models.sensor import SensorReading
from app.services.websocket_manager import ws_manager

# Default thresholds (global fallback)
DEFAULT_THRESHOLDS = {
    "temperature": {"warning_min": -10, "warning_max": 35, "critical_min": -20, "critical_max": 45, "unit": "°C"},
    "humidity":    {"warning_min": 20,  "warning_max": 80, "critical_min": 10,  "critical_max": 95, "unit": "%"},
    "oxygen":      {"warning_min": 19.5,"warning_max": 23, "critical_min": 18,  "critical_max": 25, "unit": "%"},
    "co_level":    {"warning_min": None,"warning_max": 25, "critical_min": None,"critical_max": 50, "unit": "ppm"},
    "battery_level":{"warning_min": 20, "warning_max": None,"critical_min": 10, "critical_max": None,"unit": "%"},
}


def _get_threshold(db: Session, device_id: str, sensor_type: str) -> Optional[AlertThreshold]:
    row = (
        db.query(AlertThreshold)
        .filter(AlertThreshold.sensor_type == sensor_type)
        .filter(AlertThreshold.device_id == device_id)
        .first()
    )
    if row:
        return row
    return (
        db.query(AlertThreshold)
        .filter(AlertThreshold.sensor_type == sensor_type)
        .filter(AlertThreshold.device_id.is_(None))
        .first()
    )


def _check_value(value: float, t: AlertThreshold) -> Optional[str]:
    if t.critical_min is not None and value < t.critical_min:
        return "critical"
    if t.critical_max is not None and value > t.critical_max:
        return "critical"
    if t.warning_min is not None and value < t.warning_min:
        return "warning"
    if t.warning_max is not None and value > t.warning_max:
        return "warning"
    return None


async def evaluate_reading(db: Session, reading: SensorReading):
    sensor_fields = {
        "temperature": reading.temperature,
        "humidity": reading.humidity,
        "oxygen": reading.oxygen,
        "co_level": reading.co_level,
        "battery_level": reading.battery_level,
    }
    events = []
    for sensor_type, value in sensor_fields.items():
        if value is None:
            continue
        threshold = _get_threshold(db, reading.device_id, sensor_type)
        if threshold is None:
            continue
        level = _check_value(value, threshold)
        if level is None:
            continue
        breach_val = (
            threshold.critical_max if (threshold.critical_max and value > threshold.critical_max)
            else threshold.critical_min if (threshold.critical_min and value < threshold.critical_min)
            else threshold.warning_max if (threshold.warning_max and value > threshold.warning_max)
            else threshold.warning_min
        )
        event = AlertEvent(
            device_id=reading.device_id,
            sensor_type=sensor_type,
            level=level,
            value=value,
            threshold=breach_val,
            message=f"{sensor_type} {level}: {value} (threshold: {breach_val})",
            triggered_at=reading.recorded_at or datetime.utcnow(),
        )
        db.add(event)
        events.append(event)

    if events:
        db.commit()
        for event in events:
            db.refresh(event)
            await ws_manager.broadcast(
                {
                    "type": "alert",
                    "device_id": event.device_id,
                    "sensor_type": event.sensor_type,
                    "level": event.level,
                    "value": event.value,
                    "threshold": event.threshold,
                    "message": event.message,
                    "triggered_at": str(event.triggered_at),
                },
                device_id=event.device_id,
            )


def seed_default_thresholds(db: Session):
    for sensor_type, vals in DEFAULT_THRESHOLDS.items():
        exists = (
            db.query(AlertThreshold)
            .filter(AlertThreshold.sensor_type == sensor_type)
            .filter(AlertThreshold.device_id.is_(None))
            .first()
        )
        if not exists:
            db.add(AlertThreshold(sensor_type=sensor_type, **vals))
    db.commit()
