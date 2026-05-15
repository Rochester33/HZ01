#!/bin/bash
# HZ01 停止脚本

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[HZ01]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
PID_DIR="$ROOT_DIR/.pids"

stop_service() {
    local name=$1
    local pid_file="$PID_DIR/${name}.pid"

    if [ -f "$pid_file" ]; then
        PID=$(cat "$pid_file")
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID"
            log "$name (PID $PID) 已停止"
        else
            warn "$name PID $PID 已不存在"
        fi
        rm -f "$pid_file"
    else
        # 按端口兜底查找
        case $name in
            backend)  PORT=8000 ;;
            frontend) PORT=1011 ;;
        esac
        PID=$(lsof -ti ":$PORT" 2>/dev/null || true)
        if [ -n "$PID" ]; then
            kill "$PID"
            log "$name (port $PORT, PID $PID) 已停止"
        else
            warn "未找到运行中的 $name 进程"
        fi
    fi
}

log "=== 停止 HZ01 服务 ==="
stop_service frontend
stop_service backend
log "所有服务已停止"
