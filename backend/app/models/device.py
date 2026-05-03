from sqlalchemy import Column, Integer, String, DateTime, Enum
from sqlalchemy.sql import func
from app.database import Base


class Device(Base):
    __tablename__ = "devices"

    id = Column(Integer, primary_key=True, autoincrement=True)
    device_id = Column(String(50), unique=True, nullable=False)
    name = Column(String(100))
    location = Column(String(200))
    status = Column(Enum("online", "offline", "warning", "critical"), default="offline")
    last_seen = Column(DateTime)
    created_at = Column(DateTime, server_default=func.now())
