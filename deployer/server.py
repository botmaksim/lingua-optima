#!/usr/bin/env python3
"""
Lingua Optima — Automated Deployment & Container Log Dashboard
A zero-dependency, ultra-lightweight, single-file server with:
  - Real-time SSE streaming of git pull & docker compose build/redeploy
  - Live container inspection & container log viewer with live tailing
  - Simple token/cookie authentication and Cloudflare Access compatibility
  - Concurrency locks preventing parallel duplicate builds
"""

import os
import sys
import json
import time
import queue
import shutil
import hashlib
import secrets
import threading
import subprocess
from datetime import datetime
from collections import deque
from urllib.parse import urlparse, parse_qs
from http.server import HTTPServer, ThreadingHTTPServer, BaseHTTPRequestHandler

# Configuration Defaults
PORT = int(os.environ.get("DEPLOYER_PORT", os.environ.get("PORT", "7777")))
HOST = os.environ.get("DEPLOYER_HOST", "0.0.0.0")
DEFAULT_PROJECT_DIR = "/home/home/lingua-optima" if os.path.isdir("/home/home/lingua-optima") else os.getcwd()
PROJECT_DIR = os.path.abspath(os.environ.get("PROJECT_DIR", DEFAULT_PROJECT_DIR))
DEPLOY_SECRET = os.environ.get("DEPLOY_SECRET", "lingua2026")
COOKIE_NAME = "deploy_auth"

# Derive cookie verification token from DEPLOY_SECRET
SECRET_TOKEN_HASH = hashlib.sha256(f"lingua_optima_salt_{DEPLOY_SECRET}".encode("utf-8")).hexdigest()

import hmac

# Security Constants & Enforcement
ALLOWED_SERVICES = frozenset({"backend", "frontend", "db", "redis", "adminer"})
FORBIDDEN_HOST_PATHS = frozenset({
    "/", "/etc", "/home", "/root", "/bin", "/sbin", "/usr", "/lib",
    "/dev", "/sys", "/proc", "/boot", "/opt", "/var", "/run", "/var/run",
    "/var/run/docker.sock"
})
DANGEROUS_CAPS = frozenset({
    "ALL", "SYS_ADMIN", "SYS_PTRACE", "SYS_MODULE", "SYS_RAWIO",
    "DAC_OVERRIDE", "DAC_READ_SEARCH", "NET_ADMIN"
})


def validate_security_policy(project_dir):
    """
    Enforces strict Docker container isolation and defense against host escape:
    1. Only whitelisted services (backend, frontend, db, redis, adminer).
    2. No privileged mode.
    3. No host networking or namespace sharing (network_mode, pid, ipc, userns).
    4. No dangerous capabilities (cap_add).
    5. No host root, docker.sock, or sensitive host path bind mounts.
    6. No host device mappings.
    """
    try:
        proc = subprocess.run(
            ["docker", "compose", "config", "--format", "json"],
            cwd=project_dir,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=15
        )
        if proc.returncode != 0:
            return False, f"Docker Compose configuration error: {proc.stderr.strip()}"

        cfg = json.loads(proc.stdout)
        services = cfg.get("services", {})

        if not services:
            return False, "No services defined in docker-compose"

        for svc_name, svc_conf in services.items():
            if svc_name not in ALLOWED_SERVICES:
                return False, f"Unauthorized service '{svc_name}' detected. Only {sorted(ALLOWED_SERVICES)} are permitted."

            if svc_conf.get("privileged") is True:
                return False, f"Security violation: service '{svc_name}' has 'privileged: true'."

            net_mode = svc_conf.get("network_mode", "")
            if net_mode and (net_mode == "host" or net_mode.startswith("container:")):
                return False, f"Security violation: service '{svc_name}' uses dangerous network_mode '{net_mode}'."

            if svc_conf.get("pid") == "host":
                return False, f"Security violation: service '{svc_name}' uses host PID namespace."

            if svc_conf.get("ipc") == "host":
                return False, f"Security violation: service '{svc_name}' uses host IPC namespace."

            if svc_conf.get("userns_mode") == "host":
                return False, f"Security violation: service '{svc_name}' uses host user namespace."

            if svc_conf.get("devices"):
                return False, f"Security violation: service '{svc_name}' attempts to map host devices."

            cap_adds = svc_conf.get("cap_add", []) or []
            for cap in cap_adds:
                cap_norm = str(cap).upper().replace("CAP_", "")
                if cap_norm in DANGEROUS_CAPS:
                    return False, f"Security violation: service '{svc_name}' requests forbidden capability '{cap}'."

            volumes = svc_conf.get("volumes", []) or []
            for vol in volumes:
                if isinstance(vol, dict):
                    v_type = vol.get("type", "")
                    v_source = vol.get("source", "")
                    if v_type == "bind" or (v_source and ("/" in v_source or "\\" in v_source)):
                        norm_source = os.path.normpath(v_source)
                        if norm_source in FORBIDDEN_HOST_PATHS or norm_source.startswith(("/var/run", "/etc", "/home", "/root", "/proc", "/sys", "/dev")):
                            return False, f"Security violation: service '{svc_name}' mounts forbidden host path '{v_source}'."
                        if os.path.isabs(norm_source):
                            if not norm_source.startswith(project_dir + os.sep):
                                return False, f"Security violation: service '{svc_name}' mounts external host path '{v_source}'."
                            rel = os.path.relpath(norm_source, project_dir)
                            if rel in (".", ".git", ".env", "deployer"):
                                return False, f"Security violation: service '{svc_name}' attempts to mount '{rel}' into container."

        return True, "Security verification passed: all services strictly isolated within Docker."

    except Exception as e:
        return False, f"Security validator error: {str(e)}"


# Global State
class DeployManager:
    def __init__(self):
        self.lock = threading.Lock()
        self.is_running = False
        self.started_at = None
        self.last_status = "idle" # idle, running, success, failed
        self.last_exit_code = None
        self.last_duration = None
        self.log_history = deque(maxlen=6000)
        self.listeners = set()
        self.listeners_lock = threading.Lock()

    def add_listener(self):
        q = queue.Queue(maxsize=1000)
        with self.listeners_lock:
            self.listeners.add(q)
        return q

    def remove_listener(self, q):
        with self.listeners_lock:
            self.listeners.discard(q)

    def broadcast(self, line_obj):
        self.log_history.append(line_obj)
        with self.listeners_lock:
            stale = []
            for q in self.listeners:
                try:
                    q.put_nowait(line_obj)
                except queue.Full:
                    stale.append(q)
            for q in stale:
                self.listeners.discard(q)

    def start_deploy(self):
        with self.lock:
            if self.is_running:
                return False, "Deployment already in progress"
            self.is_running = True
            self.started_at = time.time()
            self.last_status = "running"
            self.last_exit_code = None
            self.last_duration = None
            self.log_history.clear()

        thread = threading.Thread(target=self._run_deploy_thread, daemon=True)
        thread.start()
        return True, "Deployment initiated"

    def _run_deploy_thread(self):
        start_t = time.time()
        self.broadcast({
            "type": "system",
            "time": datetime.now().strftime("%H:%M:%S"),
            "text": f"Starting deployment workflow in {PROJECT_DIR}..."
        })

        workflow = [
            ("[1/5] Pulling latest commits from GitHub origin/main...", ["git", "fetch", "origin", "main"]),
            ("[1/5] Resetting working tree to origin/main...", ["git", "reset", "--hard", "origin/main"]),
            ("[2/5] Running container isolation security compliance check...", None),
            ("[3/5] Building backend Docker image...", ["docker", "compose", "build", "backend"]),
            ("[4/5] Building frontend Docker image...", ["docker", "compose", "build", "frontend"]),
            ("[5/5] Starting and recreating containers in isolated sandbox...", ["docker", "compose", "up", "-d", "--force-recreate"]),
            ("Verifying active containers...", ["docker", "compose", "ps"])
        ]

        exit_code = 0
        for desc, cmd_args in workflow:
            self.broadcast({
                "type": "stage",
                "time": datetime.now().strftime("%H:%M:%S"),
                "text": f"\n─── {desc} ───"
            })

            if cmd_args is None:
                # Security policy verification
                ok, sec_msg = validate_security_policy(PROJECT_DIR)
                if not ok:
                    exit_code = 1
                    self.broadcast({
                        "type": "error",
                        "time": datetime.now().strftime("%H:%M:%S"),
                        "text": f"[SECURITY REJECTION] {sec_msg}\nAborting deployment to protect host server."
                    })
                    break
                else:
                    self.broadcast({
                        "type": "stdout",
                        "time": datetime.now().strftime("%H:%M:%S"),
                        "text": f"[SECURITY OK] {sec_msg}"
                    })
                continue

            self.broadcast({
                "type": "cmd",
                "time": datetime.now().strftime("%H:%M:%S"),
                "text": f"$ {' '.join(cmd_args)}"
            })

            try:
                proc = subprocess.Popen(
                    cmd_args,
                    shell=False,
                    cwd=PROJECT_DIR,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    bufsize=1
                )

                for line in iter(proc.stdout.readline, ""):
                    clean = line.rstrip("\r\n")
                    if clean:
                        self.broadcast({
                            "type": "stdout",
                            "time": datetime.now().strftime("%H:%M:%S"),
                            "text": clean
                        })

                proc.stdout.close()
                rc = proc.wait()
                if rc != 0:
                    exit_code = rc
                    self.broadcast({
                        "type": "error",
                        "time": datetime.now().strftime("%H:%M:%S"),
                        "text": f"Command failed with exit code {rc}"
                    })
                    break
            except Exception as e:
                exit_code = 1
                self.broadcast({
                    "type": "error",
                    "time": datetime.now().strftime("%H:%M:%S"),
                    "text": f"Execution error: {str(e)}"
                })
                break

        duration = round(time.time() - start_t, 1)
        with self.lock:
            self.is_running = False
            self.last_duration = duration
            self.last_exit_code = exit_code
            self.last_status = "success" if exit_code == 0 else "failed"

        final_msg = f"Deployment completed successfully in {duration}s" if exit_code == 0 else f"Deployment failed after {duration}s (exit code {exit_code})"
        self.broadcast({
            "type": "done",
            "time": datetime.now().strftime("%H:%M:%S"),
            "text": final_msg,
            "exit_code": exit_code,
            "duration": duration,
            "status": self.last_status
        })

deploy_manager = DeployManager()


def get_git_info():
    try:
        cmd = ["git", "log", "-1", "--format=%h||%an||%ar||%s"]
        res = subprocess.check_output(cmd, shell=False, cwd=PROJECT_DIR, text=True, stderr=subprocess.DEVNULL).strip()
        parts = res.split("||")
        branch = subprocess.check_output(["git", "branch", "--show-current"], shell=False, cwd=PROJECT_DIR, text=True, stderr=subprocess.DEVNULL).strip()

        # Check pending commits against remote if available
        pending_count = 0
        try:
            subprocess.run(["git", "fetch", "origin", "main", "--quiet"], shell=False, cwd=PROJECT_DIR, timeout=5, stderr=subprocess.DEVNULL)
            behind = subprocess.check_output(["git", "rev-list", "HEAD..origin/main", "--count"], shell=False, cwd=PROJECT_DIR, text=True, stderr=subprocess.DEVNULL).strip()
            pending_count = int(behind) if behind.isdigit() else 0
        except Exception:
            pass

        return {
            "hash": parts[0] if len(parts) > 0 else "unknown",
            "author": parts[1] if len(parts) > 1 else "unknown",
            "date": parts[2] if len(parts) > 2 else "unknown",
            "message": parts[3] if len(parts) > 3 else "unknown",
            "branch": branch or "main",
            "pending_commits": pending_count
        }
    except Exception as e:
        return {"error": str(e), "hash": "unknown", "branch": "unknown", "pending_commits": 0}


def get_docker_containers():
    containers = []
    try:
        cmd = ["docker", "compose", "ps", "--format", "json"]
        output = subprocess.check_output(cmd, shell=False, cwd=PROJECT_DIR, text=True, stderr=subprocess.DEVNULL).strip()
        for line in output.splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                data = json.loads(line)
                containers.append({
                    "name": data.get("Name") or data.get("Names") or "unknown",
                    "service": data.get("Service") or "unknown",
                    "state": data.get("State") or "unknown",
                    "status": data.get("Status") or "unknown",
                    "health": data.get("Health") or "",
                    "ports": data.get("Ports") or ""
                })
            except Exception:
                pass
    except Exception:
        pass
    return containers


def get_system_stats():
    ram = {"used": "0 MB", "total": "0 MB", "percent": 0}
    disk = {"used": "0 GB", "total": "0 GB", "percent": 0}
    load = [0.0, 0.0, 0.0]

    try:
        load = list(os.getloadavg())
    except Exception:
        pass

    try:
        total, used, free = shutil.disk_usage(PROJECT_DIR)
        disk = {
            "used": f"{round(used / (1024**3), 1)} GB",
            "total": f"{round(total / (1024**3), 1)} GB",
            "percent": int((used / total) * 100) if total else 0
        }
    except Exception:
        pass

    try:
        if os.path.exists("/proc/meminfo"):
            mem = {}
            with open("/proc/meminfo", "r") as f:
                for line in f:
                    parts = line.split(":")
                    if len(parts) == 2:
                        key = parts[0].strip()
                        val = parts[1].strip().split()[0]
                        if val.isdigit():
                            mem[key] = int(val)
            total_kb = mem.get("MemTotal", 1)
            avail_kb = mem.get("MemAvailable", mem.get("MemFree", 0))
            used_kb = total_kb - avail_kb
            ram = {
                "used": f"{round(used_kb / 1024)} MB",
                "total": f"{round(total_kb / 1024)} MB",
                "percent": int((used_kb / total_kb) * 100)
            }
    except Exception:
        pass

    return {"ram": ram, "disk": disk, "load": load}


HTML_PAGE = """<!DOCTYPE html>
<html lang="ru" class="dark">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lingua Optima | Deploy Control</title>
  <link rel="icon" type="image/svg+xml" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%236366f1'><path d='M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5'/></svg>">
  <script src="https://cdn.tailwindcss.com"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <script>
    tailwind.config = {
      darkMode: 'class',
      theme: {
        extend: {
          fontFamily: {
            sans: ['"Plus Jakarta Sans"', 'sans-serif'],
            mono: ['"JetBrains Mono"', 'monospace'],
          },
          colors: {
            brand: {
              50: '#eef2ff',
              100: '#e0e7ff',
              500: '#6366f1',
              600: '#4f46e5',
              700: '#4338ca',
            }
          }
        }
      }
    }
  </script>
  <style>
    /* Custom scrollbar */
    ::-webkit-scrollbar { width: 8px; height: 8px; }
    ::-webkit-scrollbar-track { background: #0b0f19; }
    ::-webkit-scrollbar-thumb { background: #1e293b; border-radius: 4px; }
    ::-webkit-scrollbar-thumb:hover { background: #334155; }
    .term-line { white-space: pre-wrap; word-break: break-all; }
  </style>
</head>
<body class="bg-[#0b0f19] text-slate-100 font-sans min-h-screen flex flex-col selection:bg-brand-500 selection:text-white">

  <!-- Header -->
  <header class="border-b border-slate-800/80 bg-[#0f172a]/70 backdrop-blur-md sticky top-0 z-40">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
      <div class="flex items-center space-x-3">
        <div class="w-9 h-9 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-200">
          <svg class="w-5 h-5 text-slate-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
        </div>
        <div>
          <div class="flex items-center space-x-2">
            <h1 class="text-base font-bold tracking-tight text-white">Lingua Optima</h1>
            <span class="text-xs px-2 py-0.5 rounded font-mono font-semibold bg-slate-800 text-slate-300 border border-slate-700">DEPLOYER</span>
          </div>
          <p class="text-xs text-slate-400 font-mono">tvbox · armbian</p>
        </div>
      </div>

      <div class="flex items-center space-x-3">
        <div id="deploy-indicator" class="hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-lg border border-slate-800 bg-slate-900/60 text-xs font-mono">
          <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span class="text-slate-300" id="indicator-text">Idle</span>
        </div>
        <button onclick="refreshAll()" class="p-2 text-slate-400 hover:text-white bg-slate-800/50 hover:bg-slate-800 rounded-lg border border-slate-700/50 transition" title="Обновить">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
        </button>
        <button onclick="logout()" class="px-3 py-1.5 text-xs text-slate-400 hover:text-rose-400 bg-slate-800/50 hover:bg-rose-500/10 rounded-lg border border-slate-700/50 hover:border-rose-500/30 transition">
          Выйти
        </button>
      </div>
    </div>
  </header>

  <!-- Main Content -->
  <main class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full space-y-6">

    <!-- Top Grid: Git Version & System Info -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      
      <!-- Git Card -->
      <div class="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur flex flex-col justify-between">
        <div class="flex items-start justify-between">
          <div>
            <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Версия Git</div>
            <div class="mt-2 flex items-center space-x-2">
              <span id="git-hash" class="font-mono text-base font-bold text-indigo-400 bg-indigo-950/60 px-2 py-0.5 rounded border border-indigo-500/30">------</span>
              <span id="git-branch" class="text-xs font-mono text-slate-400 bg-slate-800 px-2 py-0.5 rounded">main</span>
            </div>
          </div>
          <div id="pending-badge" class="hidden text-xs font-mono px-2 py-1 rounded bg-amber-500/10 text-amber-400 border border-amber-500/30">
            Есть новые коммиты
          </div>
        </div>
        <div class="mt-3">
          <div id="git-message" class="text-sm font-medium text-slate-200 line-clamp-2">Загрузка информации о коммите...</div>
          <div id="git-author" class="text-xs text-slate-400 font-mono mt-1">...</div>
        </div>
      </div>

      <!-- Resource Card -->
      <div class="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur flex flex-col justify-between">
        <div class="text-xs font-semibold text-slate-400 uppercase tracking-wider">Ресурсы Сервера</div>
        <div class="space-y-3 mt-2">
          <div>
            <div class="flex justify-between text-xs font-mono mb-1">
              <span class="text-slate-400">ОЗУ</span>
              <span id="ram-text" class="text-slate-300">-- / --</span>
            </div>
            <div class="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
              <div id="ram-bar" class="bg-indigo-500 h-1.5 rounded-full transition-all duration-500" style="width: 0%"></div>
            </div>
          </div>
          <div>
            <div class="flex justify-between text-xs font-mono mb-1">
              <span class="text-slate-400">Диск</span>
              <span id="disk-text" class="text-slate-300">-- / --</span>
            </div>
            <div class="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
              <div id="disk-bar" class="bg-emerald-500 h-1.5 rounded-full transition-all duration-500" style="width: 0%"></div>
            </div>
          </div>
        </div>
        <div id="load-text" class="text-xs text-slate-400 font-mono mt-2">Load avg: - - -</div>
      </div>

      <!-- Deploy Trigger Card -->
      <div class="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur flex flex-col justify-between">
        <div>
          <div class="text-xs font-semibold text-slate-300 uppercase tracking-wider">Управление деплоем</div>
          <p class="text-xs text-slate-400 mt-1">Стянуть свежий код из origin/main и безопасно пересобрать Docker-контейнеры.</p>
        </div>
        <div class="pt-4">
          <button id="btn-deploy" onclick="triggerDeploy()" class="w-full py-3 px-4 rounded-xl font-bold text-sm text-white bg-indigo-600 hover:bg-indigo-500 active:scale-[0.98] transition shadow-xs flex items-center justify-center space-x-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
            <span id="btn-deploy-text">Запустить Redeploy</span>
          </button>
        </div>
      </div>

    </div>

    <!-- Container Health Grid -->
    <div class="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center space-x-2">
          <span>Статус контейнеров</span>
          <span id="containers-count" class="px-2 py-0.5 rounded-full bg-slate-800 text-[10px] text-slate-300 font-mono">0</span>
        </h2>
        <span class="text-xs text-slate-400 font-mono">docker-compose.yml</span>
      </div>
      <div id="containers-grid" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <!-- Containers injected via JS -->
      </div>
    </div>

    <!-- Tabs: Deployment Terminal & Container Logs -->
    <div class="bg-[#0f172a] border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col">
      
      <!-- Tab Header -->
      <div class="border-b border-slate-800 bg-slate-900/80 px-4 py-3 flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center space-x-2">
          <div class="flex space-x-1.5 mr-2">
            <span class="w-3 h-3 rounded-full bg-rose-500/80 inline-block"></span>
            <span class="w-3 h-3 rounded-full bg-amber-500/80 inline-block"></span>
            <span class="w-3 h-3 rounded-full bg-emerald-500/80 inline-block"></span>
          </div>
          <button id="tab-btn-build" onclick="switchTab('build')" class="px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 text-white transition flex items-center space-x-2">
            <span>Логи сборки</span>
            <span id="build-status-dot" class="w-2 h-2 rounded-full bg-slate-500"></span>
          </button>
          <button id="tab-btn-docker" onclick="switchTab('docker')" class="px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800/50 transition flex items-center space-x-2">
            <span>Логи сервисов</span>
          </button>
        </div>

        <!-- Terminal Controls -->
        <div class="flex items-center space-x-3 text-xs">
          <!-- Docker service selector (visible in docker tab) -->
          <div id="docker-controls" class="hidden flex items-center space-x-2">
            <select id="select-service" onchange="changeDockerService()" class="bg-slate-800 text-slate-200 border border-slate-700 rounded-lg px-2.5 py-1 text-xs font-mono focus:outline-none focus:ring-1 focus:ring-brand-500">
              <option value="backend">backend</option>
              <option value="frontend">frontend</option>
              <option value="db">db</option>
              <option value="redis">redis</option>
              <option value="adminer">adminer</option>
            </select>
            <select id="select-tail" onchange="fetchDockerLogs()" class="bg-slate-800 text-slate-200 border border-slate-700 rounded-lg px-2 py-1 text-xs font-mono focus:outline-none">
              <option value="100">100 строк</option>
              <option value="300" selected>300 строк</option>
              <option value="1000">1000 строк</option>
            </select>
            <button onclick="restartSelectedService()" class="px-2.5 py-1 bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 border border-amber-500/30 rounded-lg text-xs transition font-mono flex items-center space-x-1" title="Перезагрузить контейнер">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
              <span>Restart</span>
            </button>
          </div>

          <label class="flex items-center space-x-1.5 text-slate-400 cursor-pointer">
            <input type="checkbox" id="chk-autoscroll" checked class="rounded bg-slate-800 border-slate-700 text-brand-600 focus:ring-0">
            <span>Автопрокрутка</span>
          </label>
          <button onclick="copyTerminalText()" class="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition" title="Скопировать логи">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
          </button>
          <button onclick="clearTerminal()" class="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded transition" title="Очистить экран">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </button>
        </div>
      </div>

      <!-- Terminal Body -->
      <div id="terminal-container" class="p-4 font-mono text-xs leading-relaxed overflow-y-auto h-[480px] bg-[#070b14] text-slate-300">
        <div id="terminal-content">
          <div class="text-slate-500">// Терминал готов к работе. Нажмите «Запустить Redeploy» или выберите логи сервиса.</div>
        </div>
      </div>

      <!-- Terminal Footer -->
      <div class="border-t border-slate-800/80 px-4 py-2 bg-slate-900/60 flex items-center justify-between text-[11px] font-mono text-slate-400">
        <div id="term-status-left">Готов к деплою</div>
        <div id="term-status-right">lingua-optima production</div>
      </div>

    </div>

  </main>

  <script>
    let activeTab = 'build';
    let isDeploying = false;
    let sseSource = null;
    let dockerSseSource = null;

    // Switch between Build logs and Docker logs
    function switchTab(tab) {
      activeTab = tab;
      const btnBuild = document.getElementById('tab-btn-build');
      const btnDocker = document.getElementById('tab-btn-docker');
      const dockerControls = document.getElementById('docker-controls');

      if (tab === 'build') {
        btnBuild.className = 'px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 text-white transition flex items-center space-x-2';
        btnDocker.className = 'px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800/50 transition flex items-center space-x-2';
        dockerControls.classList.add('hidden');
        if (dockerSseSource) { dockerSseSource.close(); dockerSseSource = null; }
        connectBuildSse();
      } else {
        btnBuild.className = 'px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800/50 transition flex items-center space-x-2';
        btnDocker.className = 'px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 text-white transition flex items-center space-x-2';
        dockerControls.classList.remove('hidden');
        fetchDockerLogs();
      }
    }

    // ANSI escape code parser
    function ansiToHtml(text) {
      if (!text) return '';
      // Escape HTML
      let escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      // Standard ANSI colors
      escaped = escaped
        .replace(/\\033\\[0m|\\x1b\\[0m/g, '</span>')
        .replace(/\\033\\[1m|\\x1b\\[1m/g, '<span class="font-bold text-white">')
        .replace(/\\033\\[31m|\\x1b\\[31m/g, '<span class="text-rose-400">')
        .replace(/\\033\\[32m|\\x1b\\[32m/g, '<span class="text-emerald-400">')
        .replace(/\\033\\[33m|\\x1b\\[33m/g, '<span class="text-amber-400">')
        .replace(/\\033\\[34m|\\x1b\\[34m/g, '<span class="text-sky-400">')
        .replace(/\\033\\[35m|\\x1b\\[35m/g, '<span class="text-purple-400">')
        .replace(/\\033\\[36m|\\x1b\\[36m/g, '<span class="text-cyan-400">')
        .replace(/\\033\\[37m|\\x1b\\[37m/g, '<span class="text-slate-300">')
        .replace(/\\033\\[90m|\\x1b\\[90m/g, '<span class="text-slate-500">')
        .replace(/\\033\\[[0-9;]*m|\\x1b\\[[0-9;]*m/g, ''); // strip any others
      return escaped;
    }

    function appendTerminal(lineObj) {
      const box = document.getElementById('terminal-content');
      const div = document.createElement('div');
      div.className = 'term-line py-0.5 leading-snug';

      let prefix = '';
      if (lineObj.time) {
        prefix = `<span class="text-slate-600 select-none mr-2">${lineObj.time}</span>`;
      }

      if (lineObj.type === 'stage') {
        div.className += ' text-indigo-400 font-bold mt-2 pb-1 border-b border-indigo-950';
        div.innerHTML = `${prefix}${ansiToHtml(lineObj.text)}`;
      } else if (lineObj.type === 'cmd') {
        div.className += ' text-cyan-300 font-semibold';
        div.innerHTML = `${prefix}${ansiToHtml(lineObj.text)}`;
      } else if (lineObj.type === 'error') {
        div.className += ' text-rose-400 font-semibold';
        div.innerHTML = `${prefix}${ansiToHtml(lineObj.text)}`;
      } else if (lineObj.type === 'done') {
        div.className += lineObj.exit_code === 0 ? ' text-emerald-400 font-bold mt-2' : ' text-rose-400 font-bold mt-2';
        div.innerHTML = `${prefix}${ansiToHtml(lineObj.text)}`;
      } else {
        div.innerHTML = `${prefix}${ansiToHtml(lineObj.text)}`;
      }

      box.appendChild(div);

      if (document.getElementById('chk-autoscroll').checked) {
        const container = document.getElementById('terminal-container');
        container.scrollTop = container.scrollHeight;
      }
    }

    function clearTerminal() {
      document.getElementById('terminal-content').innerHTML = '';
    }

    function copyTerminalText() {
      const text = document.getElementById('terminal-content').innerText;
      navigator.clipboard.writeText(text).then(() => {
        alert('Логи скопированы в буфер обмена!');
      });
    }

    // Refresh general system & git status
    async function refreshAll() {
      try {
        const res = await fetch('/api/status');
        if (res.status === 401) {
          window.location.reload();
          return;
        }
        const data = await res.json();

        // Git
        document.getElementById('git-hash').innerText = data.git.hash;
        document.getElementById('git-branch').innerText = data.git.branch;
        document.getElementById('git-message').innerText = data.git.message;
        document.getElementById('git-author').innerText = `${data.git.author} · ${data.git.date}`;

        const pendingBadge = document.getElementById('pending-badge');
        if (data.git.pending_commits > 0) {
          pendingBadge.classList.remove('hidden');
          pendingBadge.innerText = `+${data.git.pending_commits} новых коммитов в main`;
        } else {
          pendingBadge.classList.add('hidden');
        }

        // Resources
        document.getElementById('ram-text').innerText = `${data.system.ram.used} / ${data.system.ram.total} (${data.system.ram.percent}%)`;
        document.getElementById('ram-bar').style.width = `${data.system.ram.percent}%`;
        document.getElementById('disk-text').innerText = `${data.system.disk.used} / ${data.system.disk.total} (${data.system.disk.percent}%)`;
        document.getElementById('disk-bar').style.width = `${data.system.disk.percent}%`;
        document.getElementById('load-text').innerText = `Load avg: ${data.system.load.map(n => n.toFixed(2)).join(' ')}`;

        // Containers
        document.getElementById('containers-count').innerText = data.containers.length;
        const grid = document.getElementById('containers-grid');
        grid.innerHTML = '';
        data.containers.forEach(c => {
          const isUp = c.state === 'running';
          const card = document.createElement('div');
          card.className = `p-3 rounded-xl border ${isUp ? 'border-slate-800 bg-slate-900/40' : 'border-rose-900/40 bg-rose-950/20'} flex flex-col justify-between`;
          card.innerHTML = `
            <div class="flex items-center justify-between">
              <span class="text-xs font-mono font-bold text-slate-200 truncate">${c.service}</span>
              <span class="w-2 h-2 rounded-full ${isUp ? 'bg-emerald-400' : 'bg-rose-400'}"></span>
            </div>
            <div class="text-[11px] font-mono text-slate-400 mt-1 truncate">${c.status}</div>
            <div class="text-[10px] font-mono text-slate-500 mt-1 truncate">${c.ports || 'no ports'}</div>
          `;
          grid.appendChild(card);
        });

        // Deploy State
        setDeployState(data.deployment.is_running, data.deployment);

      } catch (err) {
        console.error('Failed to fetch status', err);
      }
    }

    function setDeployState(running, deployInfo) {
      isDeploying = running;
      const btn = document.getElementById('btn-deploy');
      const btnText = document.getElementById('btn-deploy-text');
      const indicator = document.getElementById('indicator-text');
      const dot = document.getElementById('build-status-dot');

      if (running) {
        btn.disabled = true;
        btnText.innerText = 'Идет деплой...';
        indicator.innerText = 'Building...';
        indicator.className = 'text-amber-400';
        dot.className = 'w-2 h-2 rounded-full bg-amber-400 animate-pulse';
        document.getElementById('term-status-left').innerText = 'Выполняется сборка и деплой...';
      } else {
        btn.disabled = false;
        btnText.innerText = 'Запустить Redeploy';
        indicator.innerText = 'Idle';
        indicator.className = 'text-slate-300';
        dot.className = deployInfo && deployInfo.last_status === 'success' ? 'w-2 h-2 rounded-full bg-emerald-400' : (deployInfo && deployInfo.last_status === 'failed' ? 'w-2 h-2 rounded-full bg-rose-400' : 'w-2 h-2 rounded-full bg-slate-500');
        if (deployInfo && deployInfo.last_duration) {
          document.getElementById('term-status-left').innerText = `Последний деплой: ${deployInfo.last_status.toUpperCase()} (${deployInfo.last_duration}s)`;
        }
      }
    }

    // Connect to Build SSE
    function connectBuildSse() {
      if (sseSource) sseSource.close();
      sseSource = new EventSource('/api/deploy/stream');

      sseSource.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data);
          appendTerminal(data);
          if (data.type === 'done') {
            setDeployState(false, { last_status: data.status, last_duration: data.duration });
            refreshAll();
          }
        } catch (err) {
          console.error(err);
        }
      };

      sseSource.onerror = () => {
        // Retry connection automatically
      };
    }

    // Trigger deployment
    async function triggerDeploy() {
      if (isDeploying) return;
      if (!confirm('Вы уверены, что хотите запустить пересборку и деплой Lingua Optima?')) return;

      switchTab('build');
      clearTerminal();
      setDeployState(true);

      try {
        const res = await fetch('/api/deploy', { method: 'POST' });
        const data = await res.json();
        if (!res.ok) {
          alert('Ошибка запуска: ' + (data.error || 'Unknown'));
          setDeployState(false);
        }
      } catch (err) {
        alert('Сетевая ошибка: ' + err.message);
        setDeployState(false);
      }
    }

    // Docker Logs
    async function fetchDockerLogs() {
      if (activeTab !== 'docker') return;
      const service = document.getElementById('select-service').value;
      const tail = document.getElementById('select-tail').value;
      clearTerminal();
      document.getElementById('term-status-left').innerText = `Просмотр логов: ${service} · ${tail} строк...`;

      try {
        const res = await fetch(`/api/logs?service=${encodeURIComponent(service)}&tail=${tail}`);
        const data = await res.json();
        if (data.logs) {
          data.logs.forEach(line => appendTerminal({ text: line }));
        }
      } catch (err) {
        appendTerminal({ type: 'error', text: 'Ошибка получения логов: ' + err.message });
      }
    }

    function changeDockerService() {
      fetchDockerLogs();
    }

    async function restartSelectedService() {
      const service = document.getElementById('select-service').value;
      if (!confirm(`Перезагрузить контейнер ${service}?`)) return;

      appendTerminal({ type: 'cmd', text: `$ docker compose restart ${service}` });
      try {
        const res = await fetch('/api/restart', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ service })
        });
        const data = await res.json();
        if (res.ok) {
          appendTerminal({ type: 'done', text: 'Контейнер ' + service + ' успешно перезагружен' });
          setTimeout(fetchDockerLogs, 1500);
          refreshAll();
        } else {
          appendTerminal({ type: 'error', text: 'Ошибка перезагрузки: ' + data.error });
        }
      } catch (err) {
        appendTerminal({ type: 'error', text: 'Ошибка: ' + err.message });
      }
    }

    function logout() {
      document.cookie = 'deploy_auth=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
      window.location.reload();
    }

    // Init
    refreshAll();
    connectBuildSse();
    setInterval(refreshAll, 10000); // 10s auto-refresh
  </script>
</body>
</html>
"""

LOGIN_PAGE = """<!DOCTYPE html>
<html lang="ru" class="dark">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Lingua Optima | Deploy Access</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500&display=swap" rel="stylesheet">
  <style>body { font-family: 'Plus Jakarta Sans', sans-serif; }</style>
</head>
<body class="bg-[#0b0f19] text-slate-100 min-h-screen flex items-center justify-center p-4">
  <div class="max-w-md w-full bg-slate-900/80 border border-slate-800 rounded-3xl p-8 shadow-2xl backdrop-blur relative overflow-hidden">
    <div class="text-center space-y-2">
      <div class="w-10 h-10 rounded-xl bg-slate-800 border border-slate-700 mx-auto flex items-center justify-center text-slate-200">
        <svg class="w-5 h-5 text-slate-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
        </svg>
      </div>
      <h1 class="text-xl font-bold text-white tracking-tight">Lingua Optima Deploy Control</h1>
      <p class="text-xs text-slate-400">Введите пароль доступа для управления сервером tvbox</p>
    </div>

    <form method="POST" action="/api/login" class="mt-6 space-y-4">
      <div>
        <label class="block text-xs font-mono text-slate-400 mb-1.5">Ключ доступа</label>
        <input type="password" name="secret" required autofocus placeholder="••••••••••••" class="w-full bg-[#070b14] border border-slate-700/80 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition font-mono">
      </div>
      <button type="submit" class="w-full py-2.5 rounded-xl text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-500 active:scale-[0.98] transition shadow-xs">
        Войти в панель
      </button>
    </form>
    <div class="mt-6 text-center text-[11px] text-slate-400 font-mono">
      Protected by Cloudflare & SSL
    </div>
  </div>
</body>
</html>
"""


class DeployRequestHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Concise logging
        sys.stderr.write(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {self.address_string()} {format % args}\n")

    def _is_authenticated(self):
        # 1. Cloudflare Access header bypass if configured
        cf_email = self.headers.get("Cf-Access-Authenticated-User-Email")
        if cf_email:
            return True

        # 2. Check X-Deploy-Secret header
        secret_hdr = self.headers.get("X-Deploy-Secret", "")
        if secret_hdr and hmac.compare_digest(secret_hdr, DEPLOY_SECRET):
            return True

        # 3. Check Authorization Header: Bearer <key>
        auth_header = self.headers.get("Authorization", "")
        if auth_header.startswith("Bearer "):
            token = auth_header[7:].strip()
            if hmac.compare_digest(token, DEPLOY_SECRET):
                return True

        # 4. Check Cookie
        cookie_header = self.headers.get("Cookie", "")
        if cookie_header:
            cookies = [c.strip() for c in cookie_header.split(";")]
            for c in cookies:
                if "=" in c:
                    k, v = c.split("=", 1)
                    if k.strip() == COOKIE_NAME and hmac.compare_digest(v.strip(), SECRET_TOKEN_HASH):
                        return True

        # 5. Check Query Param ?token=...
        parsed = urlparse(self.path)
        qs = parse_qs(parsed.query)
        if "token" in qs and hmac.compare_digest(qs["token"][0], DEPLOY_SECRET):
            return True

        return False

    def _send_json(self, data, status=200):
        body = json.dumps(data, indent=2 if self._is_pretty() else None).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _send_text(self, text, status=200):
        body = text.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _is_pretty(self):
        parsed = urlparse(self.path)
        qs = parse_qs(parsed.query)
        return "pretty" in qs or qs.get("format", [""])[0] == "pretty"

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        # Unauthenticated health check endpoint for monitoring & agents
        if path == "/api/health":
            self._send_json({
                "ok": True,
                "status": "UP",
                "service": "lingua-deployer",
                "is_deploying": deploy_manager.is_running,
                "timestamp": datetime.utcnow().isoformat() + "Z"
            })
            return

        # Unprotected or Login check
        if not self._is_authenticated():
            if path.startswith("/api/"):
                self._send_json({"ok": False, "error": "Unauthorized", "message": "Authentication required. Use Authorization: Bearer <secret> or X-Deploy-Secret: <secret>"}, 401)
                return
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(LOGIN_PAGE.encode("utf-8"))
            return

        if path == "/":
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(HTML_PAGE.encode("utf-8"))
            return

        elif path == "/api/status":
            git_info = get_git_info()
            containers = get_docker_containers()
            sys_info = get_system_stats()
            self._send_json({
                "ok": True,
                "git": git_info,
                "containers": containers,
                "system": sys_info,
                "deployment": {
                    "is_running": deploy_manager.is_running,
                    "started_at": deploy_manager.started_at,
                    "last_status": deploy_manager.last_status,
                    "last_exit_code": deploy_manager.last_exit_code,
                    "last_duration": deploy_manager.last_duration
                }
            })
            return

        elif path == "/api/containers":
            containers = get_docker_containers()
            self._send_json({
                "ok": True,
                "count": len(containers),
                "containers": containers
            })
            return

        elif path in ("/api/deploy/logs", "/api/deploy/history"):
            qs = parse_qs(parsed.query)
            fmt = qs.get("format", ["json"])[0].lower()
            accept = self.headers.get("Accept", "")
            tail_param = qs.get("tail", ["200"])[0]
            tail = int(tail_param) if tail_param.isdigit() else 200

            history = list(deploy_manager.log_history)
            if tail > 0:
                history = history[-tail:]

            if fmt == "text" or "text/plain" in accept:
                lines = [item.get("text", item.get("line", "")) for item in history]
                self._send_text("\n".join(lines) + ("\n" if lines else ""))
            else:
                self._send_json({
                    "ok": True,
                    "is_running": deploy_manager.is_running,
                    "last_status": deploy_manager.last_status,
                    "last_duration": deploy_manager.last_duration,
                    "count": len(history),
                    "logs": history
                })
            return

        elif path == "/api/deploy/stream":
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.send_header("X-Accel-Buffering", "no") # Disable nginx buffering
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()

            q = deploy_manager.add_listener()
            try:
                # Replay existing buffer
                for item in list(deploy_manager.log_history):
                    msg = f"data: {json.dumps(item)}\n\n"
                    self.wfile.write(msg.encode("utf-8"))
                self.wfile.flush()

                # Stream new lines
                while True:
                    try:
                        item = q.get(timeout=15.0)
                        msg = f"data: {json.dumps(item)}\n\n"
                        self.wfile.write(msg.encode("utf-8"))
                        self.wfile.flush()
                    except queue.Empty:
                        # Heartbeat ping
                        self.wfile.write(b": heartbeat\n\n")
                        self.wfile.flush()
            except (BrokenPipeError, ConnectionResetError):
                pass
            finally:
                deploy_manager.remove_listener(q)
            return

        elif path == "/api/logs":
            qs = parse_qs(parsed.query)
            service = qs.get("service", ["backend"])[0]
            if service not in ALLOWED_SERVICES:
                self._send_json({"ok": False, "error": f"Invalid service '{service}'. Allowed: {sorted(ALLOWED_SERVICES)}"}, status=400)
                return

            tail_param = qs.get("tail", ["200"])[0]
            try:
                tail = max(1, min(int(tail_param), 2000))
            except ValueError:
                tail = 200

            fmt = qs.get("format", ["json"])[0].lower()
            accept = self.headers.get("Accept", "")

            try:
                cmd = ["docker", "compose", "logs", f"--tail={tail}", service]
                output = subprocess.check_output(cmd, shell=False, cwd=PROJECT_DIR, text=True, stderr=subprocess.STDOUT, timeout=15)
                if fmt == "text" or "text/plain" in accept:
                    self._send_text(output)
                else:
                    lines = [line.rstrip("\r\n") for line in output.splitlines()]
                    self._send_json({
                        "ok": True,
                        "service": service,
                        "lines": len(lines),
                        "logs": lines
                    })
            except subprocess.CalledProcessError as e:
                if fmt == "text" or "text/plain" in accept:
                    self._send_text(e.output, status=500)
                else:
                    self._send_json({"ok": False, "service": service, "error": e.output, "logs": []}, status=500)
            except Exception as e:
                self._send_json({"ok": False, "error": str(e), "logs": []}, status=500)
            return

        else:
            self._send_json({"ok": False, "error": "Not Found"}, status=404)

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path

        # Handle Login form
        if path == "/api/login":
            length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(length).decode("utf-8")
            form_data = parse_qs(body)
            secret_entered = form_data.get("secret", [""])[0]

            if hmac.compare_digest(secret_entered, DEPLOY_SECRET):
                self.send_response(303)
                self.send_header("Location", "/")
                self.send_header("Set-Cookie", f"{COOKIE_NAME}={SECRET_TOKEN_HASH}; Path=/; HttpOnly; SameSite=Lax; Max-Age=2592000")
                self.end_headers()
            else:
                self.send_response(303)
                self.send_header("Location", "/?error=invalid_key")
                self.end_headers()
            return

        # Remaining endpoints require auth
        if not self._is_authenticated():
            self._send_json({"ok": False, "error": "Unauthorized"}, status=401)
            return

        if path == "/api/deploy":
            success, msg = deploy_manager.start_deploy()
            status_code = 200 if success else 409
            self._send_json({
                "ok": success,
                "success": success,
                "message": msg,
                "is_running": deploy_manager.is_running
            }, status=status_code)
            return

        elif path == "/api/restart":
            length = int(self.headers.get("Content-Length", 0))
            payload = {}
            if length > 0:
                try:
                    payload = json.loads(self.rfile.read(length).decode("utf-8"))
                except Exception:
                    pass

            service = payload.get("service") or parse_qs(urlparse(self.path).query).get("service", ["backend"])[0]
            if service not in ALLOWED_SERVICES:
                self._send_json({"ok": False, "error": f"Invalid service '{service}'. Allowed: {sorted(ALLOWED_SERVICES)}"}, status=400)
                return

            try:
                cmd = ["docker", "compose", "restart", service]
                subprocess.check_call(cmd, shell=False, cwd=PROJECT_DIR, timeout=30)
                self._send_json({"ok": True, "success": True, "service": service, "message": f"Service {service} restarted successfully"})
            except Exception as e:
                self._send_json({"ok": False, "success": False, "error": str(e)}, status=500)
            return

        else:
            self._send_json({"ok": False, "error": "Not Found"}, status=404)


def main():
    print("=" * 60)
    print("[DEPLOYER] Lingua Optima Deployment Server")
    print("=" * 60)
    print(f"[DIR]  Target Project Dir : {PROJECT_DIR}")
    print(f"[ADDR] Listening on       : http://{HOST}:{PORT}")
    print(f"[AUTH] Secret Key Set     : {'[CUSTOM]' if DEPLOY_SECRET != 'lingua2026' else 'lingua2026 (default)'}")
    print("=" * 60)

    server = ThreadingHTTPServer((HOST, PORT), DeployRequestHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server...")
        server.server_close()


if __name__ == "__main__":
    main()
