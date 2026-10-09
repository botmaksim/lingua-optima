/**
 * @file worker.js
 * @brief Cloudflare Worker reverse-proxy and live model catalog scraper for AI providers.
 *
 * Enables backend servers deployed in geo-restricted regions (e.g. RU/BY) to securely route
 * AI inference requests through Cloudflare's global edge network (US/EU egress IPs) via
 * https://ai-proxy.mybsu.online and https://lingua-optima-ai-proxy.maksimmon2008.workers.dev.
 * Also provides live model discovery from official vendor documentation pages via /models/:provider.
 *
 * Supported Routes:
 *   - /models/:provider -> Live scraped + curated October 2026 model list from official vendor docs
 *   - /groq/...         -> https://api.groq.com/...
 *   - /gemini/...       -> https://generativelanguage.googleapis.com/...
 *   - /openai/...       -> https://api.openai.com/...
 *   - /anthropic/...    -> https://api.anthropic.com/...
 *   - /deepseek/...     -> https://api.deepseek.com/...
 *   - /qwen/...         -> https://dashscope-intl.aliyuncs.com/...
 *   - /kimi/...         -> https://api.moonshot.cn/...
 *   - /health           -> {"status":"UP","service":"Lingua Optima AI Proxy"}
 */

const BASE_CATALOG = {
  DEEPSEEK: {
    defaultModel: "deepseek-flash",
    docsUrl: "https://api-docs.deepseek.com/quick_start/pricing",
    models: [
      { id: "deepseek-flash", label: "DeepSeek V4.1 Flash (1M)", badge: "Latest Flagship" },
      { id: "deepseek-v4-pro", label: "DeepSeek V4 Pro (1M)", badge: "Deep Reasoning" },
      { id: "deepseek-chat", label: "DeepSeek Chat (Auto Alias)", badge: "Compatible Alias" },
      { id: "deepseek-reasoner", label: "DeepSeek Reasoner (CoT Alias)", badge: "Reasoning Alias" },
    ],
  },
  QWEN: {
    defaultModel: "qwen3.8-max",
    docsUrl: "https://www.alibabacloud.com/help/en/model-studio/getting-started/models",
    models: [
      { id: "qwen3.8-max", label: "Qwen 3.8 Max", badge: "Latest 3.8 Flagship" },
      { id: "qwen3.8-flash", label: "Qwen 3.8 Flash", badge: "Ultra-Fast 3.8" },
      { id: "qwen3.8-omni-flash", label: "Qwen 3.8 Omni Flash", badge: "Multimodal 3.8" },
      { id: "qwen3.8-27b", label: "Qwen 3.8 27B", badge: "Open Weights 3.8" },
      { id: "qwen3.7-max", label: "Qwen 3.7 Max", badge: "High Precision" },
      { id: "qwen3.7-plus", label: "Qwen 3.7 Plus", badge: "Balanced" },
      { id: "qwen3.6-plus", label: "Qwen 3.6 Plus", badge: "Stable" },
      { id: "qwen-max", label: "Qwen Max (Auto-Latest)", badge: "Alias" },
    ],
  },
  KIMI: {
    defaultModel: "kimi-k3",
    docsUrl: "https://platform.moonshot.ai/docs/pricing/chat",
    models: [
      { id: "kimi-k3", label: "Kimi K3 (2.8T Flagship, 1M)", badge: "Latest K3 Flagship" },
      { id: "kimi-k2.7-code", label: "Kimi K2.7 Code (256K)", badge: "Deep Reasoning" },
      { id: "kimi-k2.7-code-highspeed", label: "Kimi K2.7 Highspeed (180 tok/s)", badge: "Ultra-Fast" },
      { id: "kimi-k2.6", label: "Kimi K2.6 (Multimodal Thinking)", badge: "General Purpose" },
    ],
  },
  GEMINI: {
    defaultModel: "gemini-3.8-flash",
    docsUrl: "https://ai.google.dev/gemini-api/docs/models",
    models: [
      { id: "gemini-3.8-flash", label: "Gemini 3.8 Flash", badge: "Latest 3.8 Flagship" },
      { id: "gemini-3.8-live-extended-thinking", label: "Gemini 3.8 Extended Thinking", badge: "Deep Reasoning" },
      { id: "gemini-3.8-live", label: "Gemini 3.8 Live", badge: "Low-Latency" },
      { id: "gemini-3.7-flash", label: "Gemini 3.7 Flash", badge: "Fast 3.7" },
      { id: "gemini-3.6-flash", label: "Gemini 3.6 Flash (Stable)", badge: "Production Stable" },
      { id: "gemini-3.5-flash", label: "Gemini 3.5 Flash", badge: "Balanced 3.5" },
      { id: "gemini-3.1-pro-preview", label: "Gemini 3.1 Pro Preview", badge: "Pro Reasoning" },
      { id: "gemini-2.5-pro", label: "Gemini 2.5 Pro", badge: "Legacy Pro" },
      { id: "gemini-2.5-flash", label: "Gemini 2.5 Flash", badge: "Legacy Flash" },
    ],
  },
  GROQ: {
    defaultModel: "qwen/qwen3.8-27b",
    docsUrl: "https://console.groq.com/docs/models",
    models: [
      { id: "qwen/qwen3.8-27b", label: "Qwen 3.8 27B (Groq LPU)", badge: "Latest 3.8 on LPU" },
      { id: "openai/gpt-oss-120b", label: "OpenAI GPT-OSS 120B (Groq LPU)", badge: "Flagship LPU" },
      { id: "openai/gpt-oss-20b", label: "OpenAI GPT-OSS 20B (Groq LPU)", badge: "Ultra-Fast LPU" },
      { id: "qwen/qwen3.6-27b", label: "Qwen 3.6 27B (Groq LPU)", badge: "Fast Reasoning" },
      { id: "meta-llama/llama-4-maverick-17b-128e-instruct", label: "Llama 4 Maverick 17B-128E", badge: "Llama 4 MoE" },
      { id: "meta-llama/llama-4-scout-17b-16e-instruct", label: "Llama 4 Scout 17B-16E", badge: "Ultra-Low Latency" },
      { id: "moonshotai/kimi-k2-instruct", label: "Kimi K2 Instruct (Groq LPU)", badge: "1T MoE on LPU" },
      { id: "llama-3.3-70b-versatile", label: "Llama 3.3 70B Versatile", badge: "Classic Versatile" },
      { id: "llama-3.1-8b-instant", label: "Llama 3.1 8B Instant", badge: "Instant" },
    ],
  },
  OPENAI: {
    defaultModel: "gpt-6.1-sol",
    docsUrl: "https://platform.openai.com/docs/models",
    models: [
      { id: "gpt-6-astra", label: "GPT-6 Astra (1M Flagship)", badge: "Frontier Reasoning" },
      { id: "gpt-6.1-sol", label: "GPT-6.1 Sol", badge: "Default Balanced" },
      { id: "gpt-6-luna", label: "GPT-6 Luna", badge: "Fast & Cost-Efficient" },
      { id: "gpt-5.6-sol", label: "GPT-5.6 Sol", badge: "High Precision" },
      { id: "gpt-5.6-luna", label: "GPT-5.6 Luna", badge: "Low Latency" },
      { id: "o4-mini", label: "o4-mini", badge: "Fast Reasoning" },
      { id: "o3", label: "o3", badge: "Deep Reasoning" },
    ],
  },
  ANTHROPIC: {
    defaultModel: "claude-sonnet-5-5",
    docsUrl: "https://docs.anthropic.com/en/docs/about-claude/models/overview",
    models: [
      { id: "claude-fable-5-1", label: "Claude Fable 5.1", badge: "Frontier Agentic" },
      { id: "claude-opus-5-5", label: "Claude Opus 5.5", badge: "Recommended Flagship" },
      { id: "claude-sonnet-5-5", label: "Claude Sonnet 5.5", badge: "Default Balanced" },
      { id: "claude-haiku-5-5", label: "Claude Haiku 5.5", badge: "Fast & Efficient" },
      { id: "claude-opus-5", label: "Claude Opus 5", badge: "Previous Opus" },
      { id: "claude-sonnet-5", label: "Claude Sonnet 5", badge: "Previous Sonnet" },
      { id: "claude-sonnet-4-6", label: "Claude Sonnet 4.6", badge: "Legacy 4.6" },
    ],
  },
};

async function scrapeLiveModelsFromDocs(providerKey) {
  const entry = BASE_CATALOG[providerKey];
  if (!entry) return null;

  const knownMap = new Map();
  for (const m of entry.models) {
    knownMap.set(m.id, m);
  }

  let liveSource = "curated-2026-catalog";
  try {
    const resp = await fetch(entry.docsUrl, {
      headers: { "User-Agent": "Mozilla/5.0 (compatible; LinguaOptimaModelSync/1.0)" },
      cf: { cacheTtl: 3600, cacheEverything: true },
    });
    if (resp.ok) {
      const html = await resp.text();
      liveSource = "live-official-docs";
      if (providerKey === "GEMINI") {
        const matches = html.match(/gemini-[345]\.[0-9]+-(?:flash|pro|live(?:-[a-z-]+)?)/g) || [];
        for (const id of matches) {
          if (
            !id.includes("tts") &&
            !id.includes("image") &&
            !id.includes("translate") &&
            !id.includes("transcribe") &&
            !knownMap.has(id)
          ) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      } else if (providerKey === "GROQ") {
        const matches =
          html.match(/(?:qwen\/qwen[0-9a-z.-]+|openai\/gpt-oss-[0-9a-z.-]+|meta-llama\/llama-4-[a-z0-9.-]+)/g) || [];
        for (const id of matches) {
          if (
            !id.endsWith("-limits") &&
            !id.endsWith("-price") &&
            !id.includes("safeguard") &&
            !id.includes("guard") &&
            !knownMap.has(id)
          ) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      } else if (providerKey === "ANTHROPIC") {
        const matches = html.match(/claude-(?:fable|opus|sonnet|haiku)-[56](?:-[0-9]+)?/g) || [];
        for (const id of matches) {
          if (!knownMap.has(id)) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      } else if (providerKey === "DEEPSEEK") {
        const matches = html.match(/deepseek-(?:flash|v[45](?:-[a-z0-9.]+)?|chat|reasoner)/g) || [];
        for (const id of matches) {
          if (!knownMap.has(id)) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      } else if (providerKey === "QWEN") {
        const matches = html.match(/qwen[34]\.[0-9]+-(?:max|plus|flash|omni-flash|[0-9]+b)/g) || [];
        for (const id of matches) {
          if (!knownMap.has(id)) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      } else if (providerKey === "KIMI") {
        const matches = html.match(/kimi-k[34](?:\.[0-9]+)?(?:-[a-z0-9-]+)?/g) || [];
        for (const id of matches) {
          if (!knownMap.has(id)) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      } else if (providerKey === "OPENAI") {
        const matches = html.match(/gpt-[56](?:\.[0-9]+)?-(?:astra|sol|luna|mini|pro)/g) || [];
        for (const id of matches) {
          if (!knownMap.has(id)) {
            knownMap.set(id, { id, label: id, badge: "Live Docs" });
          }
        }
      }
    }
  } catch (_) {
    // Fallback gracefully to BASE_CATALOG if upstream docs timeout
  }

  return {
    provider: providerKey,
    defaultModel: entry.defaultModel,
    docsUrl: entry.docsUrl,
    source: liveSource,
    updatedAt: new Date().toISOString(),
    models: Array.from(knownMap.values()),
  };
}

export default {
  async fetch(request) {
    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
          "Access-Control-Allow-Headers": "*",
          "Access-Control-Max-Age": "86400",
        },
      });
    }

    const url = new URL(request.url);
    const pathname = url.pathname;

    if (pathname === "/models" || pathname.startsWith("/models/")) {
      const parts = pathname.split("/").filter(Boolean);
      if (parts.length === 2) {
        const providerKey = parts[1].toUpperCase();
        const data = await scrapeLiveModelsFromDocs(providerKey);
        if (!data) {
          return new Response(JSON.stringify({ error: "Unknown provider" }), {
            status: 404,
            headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
          });
        }
        return new Response(JSON.stringify(data), {
          status: 200,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }
      return new Response(JSON.stringify(BASE_CATALOG), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }

    let targetHost = "";
    let targetPath = pathname;

    if (pathname.startsWith("/groq/")) {
      targetHost = "api.groq.com";
      targetPath = pathname.replace(/^\/groq/, "");
    } else if (pathname.startsWith("/gemini/")) {
      targetHost = "generativelanguage.googleapis.com";
      targetPath = pathname.replace(/^\/gemini/, "");
    } else if (pathname.startsWith("/openai/")) {
      targetHost = "api.openai.com";
      targetPath = pathname.replace(/^\/openai/, "");
    } else if (pathname.startsWith("/anthropic/")) {
      targetHost = "api.anthropic.com";
      targetPath = pathname.replace(/^\/anthropic/, "");
    } else if (pathname.startsWith("/deepseek/")) {
      targetHost = "api.deepseek.com";
      targetPath = pathname.replace(/^\/deepseek/, "");
    } else if (pathname.startsWith("/qwen/")) {
      targetHost = "dashscope-intl.aliyuncs.com";
      targetPath = pathname.replace(/^\/qwen/, "");
    } else if (pathname.startsWith("/kimi/")) {
      targetHost = "api.moonshot.cn";
      targetPath = pathname.replace(/^\/kimi/, "");
    } else if (pathname === "/" || pathname === "/health") {
      return new Response(JSON.stringify({ status: "UP", service: "Lingua Optima AI Proxy" }), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    } else {
      return new Response(
        JSON.stringify({
          error:
            "Unknown route. Use /models/:provider, /groq/*, /gemini/*, /openai/*, /anthropic/*, /deepseek/*, /qwen/*, or /kimi/*",
        }),
        { status: 404, headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" } }
      );
    }

    const targetUrl = `https://${targetHost}${targetPath}${url.search}`;

    const headers = new Headers(request.headers);
    headers.set("Host", targetHost);
    headers.delete("cf-connecting-ip");
    headers.delete("x-real-ip");
    headers.delete("x-forwarded-for");

    const modifiedRequest = new Request(targetUrl, {
      method: request.method,
      headers: headers,
      body: request.body,
      redirect: "follow",
    });

    const response = await fetch(modifiedRequest);

    const responseHeaders = new Headers(response.headers);
    responseHeaders.set("Access-Control-Allow-Origin", "*");

    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: responseHeaders,
    });
  },
};
