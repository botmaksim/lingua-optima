# Cloudflare Worker AI Reverse-Proxy for Lingua Optima

This Cloudflare Worker resolves **AI provider regional restrictions** (including Cloudflare GeoIP blocking on Groq, geographic access restrictions on OpenAI, Anthropic, etc.) for servers and client environments located in restricted regions.

The worker executes globally on the Cloudflare Edge network (with outbound egress IP addresses in the US/EU), securely forwarding requests directly to target AI provider endpoints and sanitizing source IP headers (`cf-connecting-ip`, `x-real-ip`, `x-forwarded-for`).

---

## 🎯 Supported Endpoints & Routes

| Proxy Route | Upstream Target API / Function | Configuration Key in `.env` / Usage |
| :--- | :--- | :--- |
| `https://ai-proxy.mybsu.online/models/:provider` | **Live upstream model scraping from official provider docs** (`ai.google.dev`, `console.groq.com`, `docs.anthropic.com`, `api-docs.deepseek.com`, etc.) | Used by frontend (`useProviderModels`) for real-time model catalog synchronization |
| `https://<domain>/groq/*` | `https://api.groq.com/*` | `GROQ_BASE_URL=https://<domain>/groq/openai/v1` |
| `https://<domain>/gemini/*` | `https://generativelanguage.googleapis.com/*` | `GEMINI_BASE_URL=https://<domain>/gemini` |
| `https://<domain>/openai/*` | `https://api.openai.com/*` | `OPENAI_BASE_URL=https://<domain>/openai/v1` |
| `https://<domain>/anthropic/*` | `https://api.anthropic.com/*` | `ANTHROPIC_BASE_URL=https://<domain>/anthropic/v1` |
| `https://<domain>/deepseek/*` | `https://api.deepseek.com/*` | `DEEPSEEK_BASE_URL=https://<domain>/deepseek` |
| `https://<domain>/qwen/*` | `https://dashscope-intl.aliyuncs.com/*` | `QWEN_BASE_URL=https://<domain>/qwen/compatible-mode/v1` |
| `https://<domain>/kimi/*` | `https://api.moonshot.cn/*` | `KIMI_BASE_URL=https://<domain>/kimi/v1` |
| `https://<domain>/health` | Service health probe | Returns `{"status":"UP"}` |

---

## 🚀 Option 1: Manual Deployment via Cloudflare Dashboard (2 Minutes)

This method does **not** require exporting API tokens and is configured directly in the browser:

1. Open the [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. In the sidebar, navigate to: **Compute (Workers) > Workers & Pages**.
3. Click the blue button: **Create Application** (or **Create Worker**).
4. Provide a name for the worker (e.g., `lingua-optima-ai-proxy`) and click **Deploy**.
5. On the confirmation page, click **Edit code** (Quick Edit).
6. Replace the default placeholder script with the full contents of [`worker.js`](worker.js).
7. Click the **Save and Deploy** button in the upper-right corner.
8. *(Recommended)* Bind to custom domain `mybsu.online`:
   - Open the worker's **Settings** tab.
   - Navigate to **Triggers** (or **Custom Domains**).
   - Click **Add Custom Domain** and enter e.g. `ai-proxy.mybsu.online`.
   - Cloudflare will provision DNS routing and issue an SSL/TLS certificate automatically.

Done! The proxy endpoint is accessible at: `https://ai-proxy.mybsu.online` (or the default free domain `https://lingua-optima-ai-proxy.<subdomain>.workers.dev`).

---

## 🤖 Option 2: Automated CLI Deployment via Wrangler

To deploy automatically using the Wrangler CLI:

1. In the Cloudflare Dashboard, navigate to: **My Profile > API Tokens** ([direct link](https://dash.cloudflare.com/profile/api-tokens)).
2. Click **Create Token** -> choose the **Edit Cloudflare Workers** template (or grant `Account - Workers Scripts: Edit` permissions).
3. Copy the generated token.
4. From the main dashboard page, copy your **Account ID** (found in the right-hand overview panel).
5. Set the credentials as environment variables:
   ```bash
   export CLOUDFLARE_API_TOKEN="your_token"
   export CLOUDFLARE_ACCOUNT_ID="your_account_id"
   ```
6. Deploy the worker:
   ```bash
   npx wrangler deploy
   ```
   Wrangler will package and deploy the worker to your Cloudflare account.

---

## ⚙️ Connecting to Lingua Optima

In your root `.env` configuration file, configure the proxy URL for the required upstream providers:

```env
GROQ_BASE_URL=https://ai-proxy.mybsu.online/groq/openai/v1
# Or using the free workers.dev subdomain:
# GROQ_BASE_URL=https://lingua-optima-ai-proxy.<your-subdomain>.workers.dev/groq/openai/v1
```

Once the backend is restarted (`docker compose up -d backend` or `./gradlew bootRun`), requests to Groq, Gemini, and other providers will route transparently through the Cloudflare Edge network, bypassing regional access restrictions.
