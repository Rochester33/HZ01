"""
Background task to monitor device heartbeats and mark offline devices.
"""
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from app.models.device import Device
from app.database import SessionLocal
import asyncio
import logging

logger = logging.getLogger(__name__)

# If a device hasn't sent data in this time, mark it offline
OFFLINE_THRESHOLD = timedelta(minutes=5)


async def check_device_heartbeats():
    """Check all devices and mark those without recent heartbeat as offline."""
    db = SessionLocal()
    try:
        now = datetime.utcnow()
        cutoff = now - OFFLINE_THRESHOLD

        # Find devices that are marked online but haven't been seen recently
        stale_devices = (
            db.query(Device)
            .filter(Device.status == "online")
            .filter(Device.last_seen < cutoff)
            .all()
        )

        if stale_devices:
            device_ids = [d.device_id for d in stale_devices]
            logger.info(f"Marking {len(stale_devices)} devices as offline: {device_ids}")

            for device in stale_devices:
                device.status = "offline"

            db.commit()
    except Exception as e:
        logger.error(f"Error checking device heartbeats: {e}")
        db.rollback()
    finally:
        db.close()


async def device_monitor_loop():
    """Background loop that periodically checks device heartbeats."""
    logger.info("Device monitor started")
    while True:
        try:
            await check_device_heartbeats()
        except Exception as e:
            logger.error(f"Device monitor error: {e}")

        # Check every 60 seconds
        await asyncio.sleep(60)
