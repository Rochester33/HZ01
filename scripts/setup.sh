#!/bin/bash
# HZ01 一键初始化脚本 - 首次部署时运行
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

log "=== HZ01 环境初始化 ==="

# 检查系统依赖
log "检查系统依赖..."
command -v python3 >/dev/null 2>&1 || err "未找到 python3，请先安装: sudo apt install python3"
command -v java   >/dev/null 2>&1 || err "未找到 java，请先安装: sudo apt install openjdk-17-jdk"
command -v mysql  >/dev/null 2>&1 || err "未找到 mysql，请先安装: sudo apt install mysql-server"

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
[ "$JAVA_VER" -lt 17 ] 2>/dev/null && err "需要 Java 17+，当前版本: $JAVA_VER"

log "Python: $(python3 --version)"
log "Java:   $(java -version 2>&1 | head -1)"

# 配置 .env
ENV_FILE="$ROOT_DIR/backend/.env"
if [ ! -f "$ENV_FILE" ]; then
    log "创建 backend/.env ..."
    cp "$ROOT_DIR/backend/.env.example" "$ENV_FILE"

    read -rp "请输入数据库密码 (hz01_user): " DB_PASS
    read -rp "请输入 SECRET_KEY (直接回车自动生成): " SECRET_KEY
    [ -z "$SECRET_KEY" ] && SECRET_KEY=$(python3 -c "import secrets; print(secrets.token_hex(32))")

    sed -i "s/your_password_here/$DB_PASS/" "$ENV_FILE"
    sed -i "s/change-this-to-a-random-secret/$SECRET_KEY/" "$ENV_FILE"
    log ".env 已创建"
else
    warn "backend/.env 已存在，跳过创建"
fi

# 初始化数据库
log "初始化 MySQL 数据库..."
DB_PASS=$(grep DB_PASSWORD "$ENV_FILE" | cut -d'=' -f2)

sudo mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS hz01_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hz01_user'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON hz01_db.* TO 'hz01_user'@'localhost';
FLUSH PRIVILEGES;
SQL
log "数据库初始化完成"

# 创建 Python 虚拟环境
log "创建 Python 虚拟环境..."
cd "$ROOT_DIR/backend"
python3 -m venv .venv
source .venv/bin/activate
pip install --quiet --upgrade pip
pip install --quiet -r requirements.txt
deactivate
log "Python 依赖安装完成"

# 赋予 mvnw 执行权限
chmod +x "$ROOT_DIR/frontend/mvnw"
log "mvnw 权限已设置"

log "=== 初始化完成，运行 ./scripts/start.sh 启动服务 ==="
