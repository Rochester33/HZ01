from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from typing import List, Optional
from app.database import get_db
from app.models.alert import AlertThreshold, AlertEvent
from app.schemas.alert import ThresholdUpdate, ThresholdResponse, AlertEventResponse

router = APIRouter(prefix="/alerts", tags=["alerts"])


@router.get("/thresholds", response_model=List[ThresholdResponse])
def list_thresholds(db: Session = Depends(get_db)):
    return db.query(AlertThreshold).all()


@router.put("/thresholds", response_model=ThresholdResponse)
def upsert_threshold(payload: ThresholdUpdate, db: Session = Depends(get_db)):
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
