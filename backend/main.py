from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.database import create_tables
from app.services.alert_service import seed_default_thresholds
from app.database import SessionLocal
from app.routers import devices, sensors, alerts, commands, simulate, websocket
from app.services.device_monitor import device_monitor_loop
import asyncio


@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    db = SessionLocal()
    try:
        seed_default_thresholds(db)
    finally:
        db.close()

    # Start device heartbeat monitor in background
    monitor_task = asyncio.create_task(device_monitor_loop())

    yield

    # Cancel monitor on shutdown
    monitor_task.cancel()
    try:
        await monitor_task
    except asyncio.CancelledError:
        pass


app = FastAPI(
    title="HZ-01 Smart Helmet Backend",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(devices.router, prefix="/api/v1")
app.include_router(sensors.router, prefix="/api/v1")
app.include_router(alerts.router, prefix="/api/v1")
app.include_router(commands.router, prefix="/api/v1")
app.include_router(simulate.router, prefix="/api/v1")
app.include_router(websocket.router)


@app.get("/health")
def health():
    return {"status": "ok"}
