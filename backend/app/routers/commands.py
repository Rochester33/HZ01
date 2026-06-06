from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import or_
from typing import List
from datetime import datetime, timedelta
from app.database import get_db
from app.models.command import DeviceCommand
from app.schemas.command import CommandCreate, CommandResponse

router = APIRouter(prefix="/commands", tags=["commands"])

# A command that was handed to the device but not acknowledged within this
# window is re-delivered on the next poll. The device dedupes by id, so a
# re-delivery is harmless if the first one actually landed.
REDELIVER_AFTER = timedelta(seconds=5)


@router.post("/", response_model=CommandResponse, status_code=201)
def send_command(payload: CommandCreate, db: Session = Depends(get_db)):
    cmd = DeviceCommand(**payload.model_dump())
    db.add(cmd)
    db.commit()
    db.refresh(cmd)
    return cmd


@router.get("/", response_model=List[CommandResponse])
def list_commands(device_id: str = None, db: Session = Depends(get_db)):
    q = db.query(DeviceCommand).order_by(DeviceCommand.created_at.desc())
    if device_id:
        q = q.filter(DeviceCommand.device_id == device_id)
    return q.limit(100).all()


@router.get("/pending/{device_id}", response_model=List[CommandResponse])
def get_pending_commands(device_id: str, db: Session = Depends(get_db)):
    """Device polls this to fetch commands it still needs to run.

    Returns every command that has not yet been acknowledged:
      - never-sent commands (status == "pending"), and
      - commands sent earlier but not acknowledged within REDELIVER_AFTER.

    This makes delivery at-least-once: a command lost to a WiFi blip or a
    dropped HTTP response is re-delivered on a later poll instead of being
    silently stuck in "sent" forever. The firmware dedupes by command id, so
    re-delivery never double-fires an action.
    """
    now = datetime.utcnow()
    cutoff = now - REDELIVER_AFTER

    cmds = (
        db.query(DeviceCommand)
        .filter(DeviceCommand.device_id == device_id)
        .filter(
            or_(
                DeviceCommand.status == "pending",
                (DeviceCommand.status == "sent")
                & (
                    DeviceCommand.delivered_at.is_(None)
                    | (DeviceCommand.delivered_at <= cutoff)
                ),
            )
        )
        .order_by(DeviceCommand.created_at.asc())
        .all()
    )

    for cmd in cmds:
        cmd.status = "sent"
        cmd.delivered_at = now
    db.commit()
    # Re-read so serialized objects reflect the committed state.
    for cmd in cmds:
        db.refresh(cmd)
    return cmds


@router.patch("/{command_id}/acknowledge", response_model=CommandResponse)
def acknowledge_command(command_id: int, db: Session = Depends(get_db)):
    cmd = db.query(DeviceCommand).filter(DeviceCommand.id == command_id).first()
    if not cmd:
        raise HTTPException(status_code=404, detail="Command not found")
    cmd.status = "acknowledged"
    cmd.executed_at = datetime.utcnow()
    db.commit()
    db.refresh(cmd)
    return cmd
