from pydantic import BaseModel
from datetime import datetime
from typing import Optional


class CommandCreate(BaseModel):
    device_id: str
    command_type: str           # buzzer | led | update_threshold
    action: str                 # on | off | blink | auto | sos | <json payload>
    duration: int = 0


class CommandResponse(BaseModel):
    id: int
    device_id: str
    command_type: str
    action: str
    duration: int
    status: str
    created_at: datetime
    executed_at: Optional[datetime]

    class Config:
        from_attributes = True
