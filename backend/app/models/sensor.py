from sqlalchemy import Column, BigInteger, String, Float, DateTime
from sqlalchemy.sql import func
from app.database import Base


class SensorReading(Base):
    __tablename__ = "sensor_readings"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    device_id = Column(String(50), nullable=False, index=True)
    temperature = Column(Float)    # °C
    humidity = Column(Float)       # %
    oxygen = Column(Float)         # %
    co_level = Column(Float)       # ppm
    methane_level = Column(Float)  # ppm
    battery_level = Column(Float)  # %
    recorded_at = Column(DateTime, nullable=False, index=True)
    created_at = Column(DateTime, server_default=func.now())
