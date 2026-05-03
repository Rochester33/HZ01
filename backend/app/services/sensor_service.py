from datetime import datetime
from sqlalchemy.orm import Session
from app.models.sensor import SensorReading
from app.models.device import Device
from app.schemas.sensor import SensorReadingCreate
from app.services.alert_service import evaluate_reading
from app.services.websocket_manager import ws_manager


async def ingest_reading(db: Session, payload: SensorReadingCreate) -> SensorReading:
    recorded_at = payload.recorded_at or datetime.utcnow()
    reading = SensorReading(
        device_id=payload.device_id,
        temperature=payload.temperature,
        humidity=payload.humidity,
        oxygen=payload.oxygen,
        co_level=payload.co_level,
        battery_level=payload.battery_level,
        recorded_at=recorded_at,
    )
    db.add(reading)

    # upsert device last_seen
    device = db.query(Device).filter(Device.device_id == payload.device_id).first()
    if device:
        device.last_seen = recorded_at
        device.status = "online"
    else:
        db.add(Device(device_id=payload.device_id, last_seen=recorded_at, status="online"))

    db.commit()
    db.refresh(reading)

    # push live reading to websocket subscribers
    await ws_manager.broadcast(
        {
            "type": "reading",
            "device_id": reading.device_id,
            "temperature": reading.temperature,
            "humidity": reading.humidity,
            "oxygen": reading.oxygen,
            "co_level": reading.co_level,
            "battery_level": reading.battery_level,
            "recorded_at": str(reading.recorded_at),
        },
        device_id=reading.device_id,
    )

    # evaluate thresholds and fire alerts
    await evaluate_reading(db, reading)

    return reading
