from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from typing import List, Optional
from app.database import get_db
from app.models.alert import AlertThreshold, AlertEvent
from app.models.device import Device
from app.models.command import DeviceCommand
from app.schemas.alert import ThresholdUpdate, ThresholdResponse, AlertEventResponse
import json

router = APIRouter(prefix="/alerts", tags=["alerts"])


@router.get("/thresholds", response_model=List[ThresholdResponse])
def list_thresholds(db: Session = Depends(get_db)):
    return db.query(AlertThreshold).all()


@router.get("/thresholds/device/{device_id}", response_model=List[ThresholdResponse])
def get_device_thresholds(device_id: str, db: Session = Depends(get_db)):
    """Get thresholds for a specific device (device-specific + global fallback)."""
    thresholds = []
    sensor_types = ["temperature", "humidity", "oxygen", "co_level", "methane_level", "battery_level"]

    for sensor_type in sensor_types:
        # Try device-specific first
        threshold = (
            db.query(AlertThreshold)
            .filter(AlertThreshold.sensor_type == sensor_type)
            .filter(AlertThreshold.device_id == device_id)
            .first()
        )
        # Fallback to global
        if not threshold:
            threshold = (
                db.query(AlertThreshold)
                .filter(AlertThreshold.sensor_type == sensor_type)
                .filter(AlertThreshold.device_id.is_(None))
                .first()
            )
        if threshold:
            thresholds.append(threshold)

    return thresholds


@router.put("/thresholds", response_model=ThresholdResponse)
def upsert_threshold(payload: ThresholdUpdate, db: Session = Depends(get_db)):
    from datetime import datetime

    # If updating a specific device's threshold, check if it's online
    if payload.device_id:
        target_device = db.query(Device).filter(Device.device_id == payload.device_id).first()
        if not target_device:
            raise HTTPException(status_code=404, detail="Device not found")
        if target_device.status != "online":
            raise HTTPException(status_code=400, detail="Device is offline, cannot update threshold")

    row = (
        db.query(AlertThreshold)
        .filter(AlertThreshold.sensor_type == payload.sensor_type)
        .filter(
            AlertThreshold.device_id == payload.device_id
            if payload.device_id
            else AlertThreshold.device_id.is_(None)
        )
        .first()
    )
    if row:
        for field, value in payload.model_dump(exclude_none=True).items():
            setattr(row, field, value)
    else:
        row = AlertThreshold(**payload.model_dump())
        db.add(row)
    db.commit()
    db.refresh(row)

    # Push threshold updates to affected online devices
    # For device-specific updates: only that device
    # For global updates: all online devices
    if payload.device_id:
        online_devices = [target_device]  # Already verified online above
    else:
        online_devices = db.query(Device).filter(Device.status == "online").all()

    for device in online_devices:
        # Fetch all thresholds for this device
        sensor_types = ["temperature", "humidity", "oxygen", "co_level", "methane_level", "battery_level"]
        device_thresholds = {}

        for sensor_type in sensor_types:
            threshold = (
                db.query(AlertThreshold)
                .filter(AlertThreshold.sensor_type == sensor_type)
                .filter(AlertThreshold.device_id == device.device_id)
                .first()
            )
            if not threshold:
                threshold = (
                    db.query(AlertThreshold)
                    .filter(AlertThreshold.sensor_type == sensor_type)
                    .filter(AlertThreshold.device_id.is_(None))
                    .first()
                )
            if threshold:
                device_thresholds[sensor_type] = {
                    "warning_min": threshold.warning_min,
                    "warning_max": threshold.warning_max,
                    "critical_min": threshold.critical_min,
                    "critical_max": threshold.critical_max,
                }

        # Create command to push thresholds
        cmd = DeviceCommand(
            device_id=device.device_id,
            command_type="update_threshold",
            action=json.dumps(device_thresholds),  # Store as JSON string
            status="pending",
            created_at=datetime.utcnow(),
        )
        db.add(cmd)

    db.commit()
    return row


@router.get("/events", response_model=List[AlertEventResponse])
def list_events(
    device_id: Optional[str] = Query(None),
    acknowledged: Optional[bool] = Query(None),
    limit: int = Query(50, le=500),
    db: Session = Depends(get_db),
):
    q = db.query(AlertEvent).order_by(AlertEvent.triggered_at.desc())
    if device_id:
        q = q.filter(AlertEvent.device_id == device_id)
    if acknowledged is not None:
        q = q.filter(AlertEvent.acknowledged == acknowledged)
    return q.limit(limit).all()


@router.patch("/events/{event_id}/acknowledge", response_model=AlertEventResponse)
def acknowledge_event(event_id: int, db: Session = Depends(get_db)):
    from datetime import datetime
    event = db.query(AlertEvent).filter(AlertEvent.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="Alert event not found")
    event.acknowledged = True
    event.acknowledged_at = datetime.utcnow()
    db.commit()
    db.refresh(event)
    return event
