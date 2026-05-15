# HZ-01 Smart Helmet System

A three-tier IoT monitoring system for the HZ-01 smart helmet, featuring real-time sensor data, alert management, and device control.

## Architecture

```
Browser → Nginx (80/443) → Frontend (Spring Boot + Vaadin, :8080)
                         → Backend API (FastAPI, :8000)
                                      ↓
                               MySQL Database
```

## Requirements

| Component | Version |
|-----------|---------|
| Python    | 3.10+   |
| Java      | 17+     |
| MySQL     | 8.0+    |
| Node.js   | 20+     |

---

## Linux Deployment

### 1. Install System Dependencies

```bash
sudo apt update
sudo apt install -y python3 python3-pip python3-venv openjdk-17-jdk mysql-server nodejs npm
```

Verify Node.js version (must be 20+):

```bash
node -v
```

If version is below 20, upgrade:

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

### 2. Clone the Repository

```bash
git clone https://github.com/Rochester33/HZ01.git
cd HZ01
chmod +x scripts/*.sh frontend/mvnw
```

### 3. Configure MySQL

Run `mysql_secure_installation` and set password policy to **MEDIUM** (option `1`).

Password must meet: length ≥ 8, uppercase, lowercase, number, special character.

Then create the database and user:

```bash
sudo mysql -u root
```

```sql
CREATE DATABASE IF NOT EXISTS hz01_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hz01_user'@'localhost' IDENTIFIED BY 'YourPassword#2024';
GRANT ALL PRIVILEGES ON hz01_db.* TO 'hz01_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 4. Configure Environment

```bash
cp backend/.env.example backend/.env
nano backend/.env
```

Fill in your values:

```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=hz01_user
DB_PASSWORD=YourPassword#2024
DB_NAME=hz01_db
SECRET_KEY=your-random-secret-key
CORS_ORIGINS=http://localhost:8080,https://yourdomain.com
```

### 5. Install Python Dependencies

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
deactivate
cd ..
```

### 6. Start / Stop Services

```bash
# Start both backend and frontend
./scripts/start.sh

# Stop all services
./scripts/stop.sh
```

Logs are written to `logs/backend.log` and `logs/frontend.log`.

### 7. Nginx Reverse Proxy (Optional)

```bash
sudo apt install -y nginx
sudo tee /etc/nginx/sites-available/hz01 > /dev/null <<'EOF'
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/ {
        proxy_pass http://localhost:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://localhost:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/hz01 /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

### 8. HTTPS with Let's Encrypt (Optional)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

---

## Windows Deployment

### 1. Install Requirements

- [Python 3.10+](https://www.python.org/downloads/)
- [Java 17 JDK](https://adoptium.net/)
- [MySQL 8.0](https://dev.mysql.com/downloads/installer/)
- [Node.js 20+](https://nodejs.org/)

### 2. Clone the Repository

```cmd
git clone https://github.com/Rochester33/HZ01.git
cd HZ01
```

### 3. Configure MySQL

During MySQL installation, set a root password. Then open MySQL Workbench or Command Prompt:

```sql
CREATE DATABASE IF NOT EXISTS hz01_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'hz01_user'@'localhost' IDENTIFIED BY 'YourPassword#2024';
GRANT ALL PRIVILEGES ON hz01_db.* TO 'hz01_user'@'localhost';
FLUSH PRIVILEGES;
```

### 4. Configure Environment

Copy and edit the environment file:

```cmd
copy backend\.env.example backend\.env
notepad backend\.env
```

Fill in your database credentials and secret key.

### 5. Start Backend

```cmd
cd backend
start.cmd
```

This script will:
- Create a Python virtual environment
- Install dependencies from `requirements.txt`
- Start the FastAPI server on port 8000

### 6. Start Frontend

Open a new terminal:

```cmd
cd frontend
start.cmd
```

This script will start the Spring Boot application on port 8080.

---

## Access

| Service      | URL                          |
|--------------|------------------------------|
| Frontend UI  | http://localhost:8080        |
| Backend API  | http://localhost:8000        |
| API Docs     | http://localhost:8000/docs   |

---

## Project Structure

```
HZ01/
├── backend/          # Python FastAPI backend
│   ├── app/          # Application modules
│   ├── main.py       # Entry point
│   ├── requirements.txt
│   ├── .env.example
│   └── start.cmd     # Windows startup script
├── frontend/         # Java Spring Boot + Vaadin UI
│   ├── src/
│   ├── pom.xml
│   └── start.cmd     # Windows startup script
├── firmware/         # MicroPython for embedded device
│   └── main.py
└── scripts/          # Linux deployment scripts
    ├── setup.sh
    ├── start.sh
    └── stop.sh
```
