from sqlalchemy import Column, BigInteger, String, Integer, DateTime, Enum, Text
from sqlalchemy.sql import func
from app.database import Base


class DeviceCommand(Base):
    __tablename__ = "device_commands"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    device_id = Column(String(50), nullable=False, index=True)
    # Plain strings (not Enum) so control commands (buzzer/led/auto) and
    # config pushes (update_threshold, with a JSON payload in `action`) share
    # one table without DataError truncation.
    command_type = Column(String(32), nullable=False)
    action = Column(Text, nullable=False)
    duration = Column(Integer, default=0)   # seconds, 0 = indefinite
    status = Column(Enum("pending", "sent", "acknowledged", "failed"), default="pending")
    created_at = Column(DateTime, server_default=func.now())
    # Bumped each time the command is handed to the device; lets the poller
    # re-deliver un-acknowledged commands without losing the first-sent time.
    delivered_at = Column(DateTime)
    executed_at = Column(DateTime)
