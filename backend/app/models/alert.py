from sqlalchemy import Column, BigInteger, Integer, String, Float, DateTime, Enum, Boolean, Text
from sqlalchemy.sql import func
from app.database import Base


class AlertThreshold(Base):
    __tablename__ = "alert_thresholds"

    id = Column(Integer, primary_key=True, autoincrement=True)
    device_id = Column(String(50), nullable=True)   # NULL = global default
    sensor_type = Column(String(50), nullable=False)
    warning_min = Column(Float)
    warning_max = Column(Float)
    critical_min = Column(Float)
    critical_max = Column(Float)
    unit = Column(String(20))
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())


class AlertEvent(Base):
    __tablename__ = "alert_events"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    device_id = Column(String(50), nullable=False, index=True)
    sensor_type = Column(String(50), nullable=False)
    level = Column(Enum("warning", "critical"), nullable=False)
    value = Column(Float, nullable=False)
    threshold = Column(Float, nullable=False)
    message = Column(Text)
    acknowledged = Column(Boolean, default=False)
    triggered_at = Column(DateTime, nullable=False)
    acknowledged_at = Column(DateTime)
