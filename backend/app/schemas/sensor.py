from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class SensorReadingCreate(BaseModel):
    device_id: str
    temperature: Optional[float] = None
    humidity: Optional[float] = None
    oxygen: Optional[float] = None
    co_level: Optional[float] = None
    battery_level: Optional[float] = None
    recorded_at: Optional[datetime] = None


class SensorReadingResponse(BaseModel):
    id: int
    device_id: str
    temperature: Optional[float]
    humidity: Optional[float]
    oxygen: Optional[float]
    co_level: Optional[float]
    battery_level: Optional[float]
    recorded_at: datetime
    created_at: datetime

    class Config:
        from_attributes = True
