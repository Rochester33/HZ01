"""
Simulation router — generates fake sensor readings for UI testing.
Only active when ENABLE_SIMULATE=true in .env.
"""
import random
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.sensor import SensorReadingCreate
from app.services.sensor_service import ingest_reading
from app.config import settings

router = APIRouter(prefix="/simulate", tags=["simulate"])


def _fake_reading(device_id: str) -> SensorReadingCreate:
    return SensorReadingCreate(
        device_id=device_id,
        temperature=round(random.uniform(18, 50), 1),
        humidity=round(random.uniform(30, 95), 1),
        oxygen=round(random.uniform(17, 23), 2),
        co_level=round(random.uniform(0, 80), 1),
        battery_level=round(random.uniform(5, 100), 1),
        recorded_at=datetime.utcnow(),
    )


@router.post("/{device_id}")
async def simulate_reading(device_id: str, db: Session = Depends(get_db)):
    reading = await ingest_reading(db, _fake_reading(device_id))
    return reading
