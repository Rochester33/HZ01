from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from enum import Enum


class DeviceStatus(str, Enum):
    online = "online"
    offline = "offline"
    warning = "warning"
    critical = "critical"


class DeviceCreate(BaseModel):
    device_id: str
    name: Optional[str] = None
    location: Optional[str] = None


class DeviceUpdate(BaseModel):
    name: Optional[str] = None
    location: Optional[str] = None


class DeviceResponse(BaseModel):
    id: int
    device_id: str
    name: Optional[str]
    location: Optional[str]
    status: DeviceStatus
    last_seen: Optional[datetime]
    created_at: datetime

    class Config:
        from_attributes = True
