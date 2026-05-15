#!/bin/bash
# HZ01 启动脚本 - 同时启动 backend 和 frontend
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[HZ01]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PID_DIR="$ROOT_DIR/.pids"
LOG_DIR="$ROOT_DIR/logs"

mkdir -p "$PID_DIR" "$LOG_DIR"

# 检查 .env
[ ! -f "$ROOT_DIR/backend/.env" ] && err "未找到 backend/.env，请先运行 ./scripts/setup.sh"

# 检查端口占用
check_port() {
    if lsof -Pi ":$1" -sTCP:LISTEN -t >/dev/null 2>&1; then
        warn "端口 $1 已被占用，尝试停止旧进程..."
        "$SCRIPT_DIR/stop.sh" 2>/dev/null || true
        sleep 2
    fi
}

check_port 8000
check_port 1011

# 启动 MySQL
log "确认 MySQL 运行中..."
sudo systemctl start mysql 2>/dev/null || sudo service mysql start 2>/dev/null || warn "MySQL 启动命令失败，请手动确认"

# 启动 Backend
log "启动 Backend (port 8000)..."
cd "$ROOT_DIR/backend"
source .venv/bin/activate
nohup uvicorn main:app \
    --host 0.0.0.0 \
    --port 8000 \
    --workers 1 \
    > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > "$PID_DIR/backend.pid"
deactivate
log "Backend PID: $BACKEND_PID"

# 等待 backend 就绪
log "等待 Backend 就绪..."
for i in $(seq 1 20); do
    if curl -sf http://localhost:8000/health >/dev/null 2>&1; then
        log "Backend 已就绪"
        break
    fi
    [ "$i" -eq 20 ] && err "Backend 启动超时，查看日志: tail -f $LOG_DIR/backend.log"
    sleep 1
done

# 启动 Frontend
log "启动 Frontend (port 1011)..."
cd "$ROOT_DIR/frontend"
nohup ./mvnw spring-boot:run \
    > "$LOG_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > "$PID_DIR/frontend.pid"
log "Frontend PID: $FRONTEND_PID (首次启动需要编译，约 2-3 分钟)"

log ""
log "=== 服务已启动 ==="
log "Frontend UI : http://localhost:1011"
log "Backend API : http://localhost:8000"
log "API 文档    : http://localhost:8000/docs"
log ""
log "查看日志: tail -f $LOG_DIR/backend.log"
log "         tail -f $LOG_DIR/frontend.log"
log "停止服务: ./scripts/stop.sh"
