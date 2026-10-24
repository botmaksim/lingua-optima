#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="lingua-deployer.service"

echo "=========================================================="
echo "🚀 Setting up Lingua Optima Web Deployer Service"
echo "=========================================================="

chmod +x "${SCRIPT_DIR}/server.py"

echo "📋 Installing systemd unit..."
sudo cp "${SCRIPT_DIR}/${SERVICE_NAME}" /etc/systemd/system/${SERVICE_NAME}
sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_NAME}
sudo systemctl restart ${SERVICE_NAME}

echo "🔍 Checking service status..."
sleep 2
sudo systemctl status ${SERVICE_NAME} --no-pager

echo "=========================================================="
echo "✅ Deployer service is active on http://0.0.0.0:7777"
echo "=========================================================="
