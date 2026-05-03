from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from app.database import get_db
from app.models.command import DeviceCommand
from app.schemas.command import CommandCreate, CommandResponse

router = APIRouter(prefix="/commands", tags=["commands"])


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
    """Device polls this endpoint to fetch pending commands."""
    cmds = (
        db.query(DeviceCommand)
        .filter(DeviceCommand.device_id == device_id)
        .filter(DeviceCommand.status == "pending")
        .order_by(DeviceCommand.created_at.asc())
        .all()
    )
    # mark as sent
    for cmd in cmds:
        cmd.status = "sent"
    db.commit()
    return cmds


@router.patch("/{command_id}/acknowledge", response_model=CommandResponse)
def acknowledge_command(command_id: int, db: Session = Depends(get_db)):
    from datetime import datetime
    cmd = db.query(DeviceCommand).filter(DeviceCommand.id == command_id).first()
    if not cmd:
        raise HTTPException(status_code=404, detail="Command not found")
    cmd.status = "acknowledged"
    cmd.executed_at = datetime.utcnow()
    db.commit()
    db.refresh(cmd)
    return cmd
