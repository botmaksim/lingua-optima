# Cloudflare Worker AI Reverse-Proxy for Lingua Optima

Этот воркер решает проблему **геоблокировок AI-провайдеров** (включая Cloudflare GeoIP блок у Groq, блокировку OpenAI, Anthropic и т.д.) для серверов или пользователей, находящихся в РФ / РБ или других регионах.

Cloudflare Worker исполняется на глобальной сети Cloudflare Edge (с исходящими IP-адресами в США/ЕС), безопасно пересылая запросы напрямую к целевым API и очищая заголовки исходного IP (`cf-connecting-ip`, `x-real-ip`, `x-forwarded-for`).

---

## 🎯 Поддерживаемые маршруты

| Маршрут прокси | Целевой оригинальный API | Переменная в `.env` |
| :--- | :--- | :--- |
| `https://<твой-домен>/groq/*` | `https://api.groq.com/*` | `GROQ_BASE_URL=https://<твой-домен>/groq/openai/v1` |
| `https://<твой-домен>/gemini/*` | `https://generativelanguage.googleapis.com/*` | `GEMINI_BASE_URL=https://<твой-домен>/gemini` |
| `https://<твой-домен>/openai/*` | `https://api.openai.com/*` | `OPENAI_BASE_URL=https://<твой-домен>/openai/v1` |
| `https://<твой-домен>/anthropic/*` | `https://api.anthropic.com/*` | `ANTHROPIC_BASE_URL=https://<твой-домен>/anthropic/v1` |
| `https://<твой-домен>/health` | Проверка работоспособности | Возвращает `{"status":"UP"}` |

---

## 🚀 Вариант 1: Развертывание вручную через веб-интерфейс Cloudflare (2 минуты)

Этот способ **не требует** передавать никакие API-токены и настраивается прямо в браузере:

1. Зайди в панель [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. В левом меню выбери: **Compute (Workers) > Workers & Pages**.
3. Нажми синюю кнопку **Create Application** (или **Create Worker**).
4. Задай имя воркера (например, `lingua-optima-ai-proxy`) и нажми **Deploy**.
5. На открывшейся странице нажми **Edit code** (Quick Edit).
6. Сотри дефолтный код и вставь полный код из файла [`worker.js`](worker.js).
7. Нажми кнопку **Save and Deploy** в правом верхнем углу.
8. (Рекомендуется) Привязка к твоему домену `mybsu.online`:
   - Вернись в настройки созданного воркера.
   - Вкладка **Settings** > **Triggers** (или **Custom Domains**).
   - Нажми **Add Custom Domain** и введи, например, `ai-proxy.mybsu.online`.
   - Cloudflare автоматически создаст DNS-запись и выпустит SSL-сертификат.

Готово! Теперь адрес прокси: `https://ai-proxy.mybsu.online` (или стандартный бесплатный `https://lingua-optima-ai-proxy.<твое-имя>.workers.dev`).

---

## 🤖 Вариант 2: Автоматическое развертывание агентом (через Wrangler)

Если ты хочешь, чтобы агент сам развернул воркер через CLI, передай агенту токен Cloudflare:

1. В Cloudflare Dashboard перейди в: **My Profile > API Tokens** ([ссылка](https://dash.cloudflare.com/profile/api-tokens)).
2. Нажми **Create Token** -> выбери шаблон **Edit Cloudflare Workers** (или создай кастомный с правами `Account - Workers Scripts: Edit`).
3. Скопируй полученный токен (он показывается один раз).
4. В панели на главной странице скопируй свой **Account ID** (отображается справа внизу при выборе аккаунта).
5. Передай их агенту или задай переменные окружения:
   ```bash
   export CLOUDFLARE_API_TOKEN="твой_токен"
   export CLOUDFLARE_ACCOUNT_ID="твой_account_id"
   ```
6. Агент выполнит команду:
   ```bash
   npx wrangler deploy
   ```
   Воркер автоматически соберется и опубликуется в твоем Cloudflare аккаунте.

---

## ⚙️ Подключение к Lingua Optima

В корне проекта в файле `.env` укажи URL твоего развернутого воркера:

```env
GROQ_BASE_URL=https://ai-proxy.mybsu.online/groq/openai/v1
# или если используешь бесплатный *.workers.dev домен:
# GROQ_BASE_URL=https://lingua-optima-ai-proxy.<твой-сабдомен>.workers.dev/groq/openai/v1
```

После перезапуска бэкенда (`docker compose up -d backend` или `./gradlew bootRun`) все запросы к Groq и другим провайдерам пойдут через Cloudflare Edge, обходя любые региональные блокировки.
