# Lingua Optima — Web Deployer Dashboard & Logs Console

Ультралегковесный веб-дашборд деплоя и просмотра логов для сервера `tvbox`.

## Возможности
1. **🚀 1-Click Redeploy**: Кнопка запуска пересборки (`git fetch`, `git reset --hard origin/main`, `docker compose build`, `docker compose up -d`).
2. **📡 Real-Time SSE Terminal**: Построчный стриминг логов сборки в реальном времени с поддержкой ANSI-цветов, автоскролла и таймера.
3. **🔒 Защита от параллельных билдов**: Mutex-блокировка предотвращает одновременный запуск нескольких деплоев.
4. **📋 Docker Logs Viewer**: Просмотр логов любого сервиса (`backend`, `frontend`, `db`, `redis`, `adminer`) за 100/300/1000 строк с кнопкой перезагрузки отдельного контейнера.
5. **📊 Статус системы**: Отображение текущего коммита, ветки, количества отстающих коммитов от `origin/main`, использования RAM и диска, аптайма контейнеров.
6. **🔐 Безопасность**:
   - Авторизация по ключу доступа (`DEPLOY_SECRET`, по умолчанию `lingua2026`).
   - Автоматическая поддержка Cloudflare Access (заголовок `Cf-Access-Authenticated-User-Email`).

---

## Архитектура
- Написан на стандартной библиотеке Python 3 (`http.server.ThreadingHTTPServer`, `subprocess`, `threading`, `queue`).
- **0 внешних зависимостей** (не требует `pip install`).
- Потребляет всего **~11 МБ ОЗУ** (не нагружает систему TV-Box).
- Работает как фоновый сервис systemd пользователя `home` (`lingua-deployer.service`), автоматически стартует при загрузке системы (`loginctl enable-linger home`).

---

## Подключение домена `deploy.mybsu.online` в Cloudflare

Туннель Cloudflare (`angry_banach`) уже запущен на сервере в режиме `--network host`.

Чтобы привязать домен:
1. Откройте **Cloudflare Zero Trust** (`one.dash.cloudflare.com`).
2. Перейдите в **Networks** $\rightarrow$ **Tunnels**.
3. Выберите ваш туннель (`19fd7865-6872-4363-bd4c-a86e87181326`).
4. Нажмите **Configure** $\rightarrow$ вкладка **Public Hostname** $\rightarrow$ **Add a public hostname**:
   - **Subdomain**: `deploy`
   - **Domain**: `mybsu.online`
   - **Type**: `HTTP`
   - **URL**: `localhost:7777`
5. Нажмите **Save hostname**.

Готово! После этого `https://deploy.mybsu.online` будет сразу открывать панель деплоя.

---

## Управление сервисом на сервере

```bash
# Статус сервиса
systemctl --user status lingua-deployer.service

# Перезапуск сервиса
systemctl --user restart lingua-deployer.service

# Просмотр логов сервиса
journalctl --user -u lingua-deployer.service -f
```

## Смена пароля доступа
В файле `/home/home/lingua-optima/.env` на сервере можно задать:
```env
DEPLOY_SECRET=ваш_секретный_пароль
```
После чего перезапустить сервис: `systemctl --user restart lingua-deployer.service`.
