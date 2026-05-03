from pydantic import BaseModel
from datetime import datetime
from typing import Optional
from enum import Enum


class CommandType(str, Enum):
    buzzer = "buzzer"
    led = "led"


class CommandAction(str, Enum):
    on = "on"
    off = "off"
    blink = "blink"


class CommandCreate(BaseModel):
    device_id: str
    command_type: CommandType
    action: CommandAction
    duration: int = 0


class CommandResponse(BaseModel):
    id: int
    device_id: str
    command_type: CommandType
    action: CommandAction
    duration: int
    status: str
    created_at: datetime
    executed_at: Optional[datetime]

    class Config:
        from_attributes = True
