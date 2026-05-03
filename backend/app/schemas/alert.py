from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from enum import Enum


class AlertLevel(str, Enum):
    warning = "warning"
    critical = "critical"


class ThresholdUpdate(BaseModel):
    device_id: Optional[str] = None
    sensor_type: str
    warning_min: Optional[float] = None
    warning_max: Optional[float] = None
    critical_min: Optional[float] = None
    critical_max: Optional[float] = None
    unit: Optional[str] = None


class ThresholdResponse(BaseModel):
    id: int
    device_id: Optional[str]
    sensor_type: str
    warning_min: Optional[float]
    warning_max: Optional[float]
    critical_min: Optional[float]
    critical_max: Optional[float]
    unit: Optional[str]
    updated_at: datetime

    class Config:
        from_attributes = True


class AlertEventResponse(BaseModel):
    id: int
    device_id: str
    sensor_type: str
    level: AlertLevel
    value: float
    threshold: float
    message: Optional[str]
    acknowledged: bool
    triggered_at: datetime
    acknowledged_at: Optional[datetime]

    class Config:
        from_attributes = True
