from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import datetime
from app.database import get_db
from app.models.sensor import SensorReading
from app.schemas.sensor import SensorReadingCreate, SensorReadingResponse
from app.services.sensor_service import ingest_reading

router = APIRouter(prefix="/sensors", tags=["sensors"])


@router.post("/readings", response_model=SensorReadingResponse, status_code=201)
async def post_reading(payload: SensorReadingCreate, db: Session = Depends(get_db)):
    return await ingest_reading(db, payload)


@router.get("/readings", response_model=List[SensorReadingResponse])
def get_readings(
    device_id: Optional[str] = Query(None),
    limit: int = Query(100, le=1000),
    offset: int = Query(0),
    db: Session = Depends(get_db),
):
    q = db.query(SensorReading)
    if device_id:
        q = q.filter(SensorReading.device_id == device_id)
    return q.order_by(SensorReading.recorded_at.desc()).offset(offset).limit(limit).all()


@router.get("/readings/latest")
def get_latest_readings(db: Session = Depends(get_db)):
    """Return the most recent reading per device."""
    from sqlalchemy import func
    subq = (
        db.query(
            SensorReading.device_id,
            func.max(SensorReading.recorded_at).label("max_ts"),
        )
        .group_by(SensorReading.device_id)
        .subquery()
    )
    rows = (
        db.query(SensorReading)
        .join(
            subq,
            (SensorReading.device_id == subq.c.device_id)
            & (SensorReading.recorded_at == subq.c.max_ts),
        )
        .all()
    )
    return rows
