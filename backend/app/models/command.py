from sqlalchemy import Column, BigInteger, String, Integer, DateTime, Enum
from sqlalchemy.sql import func
from app.database import Base


class DeviceCommand(Base):
    __tablename__ = "device_commands"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    device_id = Column(String(50), nullable=False, index=True)
    command_type = Column(Enum("buzzer", "led"), nullable=False)
    action = Column(Enum("on", "off", "blink"), nullable=False)
    duration = Column(Integer, default=0)   # seconds, 0 = indefinite
    status = Column(Enum("pending", "sent", "acknowledged", "failed"), default="pending")
    created_at = Column(DateTime, server_default=func.now())
    executed_at = Column(DateTime)
