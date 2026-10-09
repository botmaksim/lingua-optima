/**
 * @file worker.js
 * @brief Cloudflare Worker reverse-proxy for AI providers (Groq, Gemini, OpenAI, Anthropic).
 *
 * Enables backend servers deployed in geo-restricted regions (e.g. RU/BY) to securely route
 * AI inference requests through Cloudflare's global edge network (US/EU egress IPs).
 *
 * Supported Routes:
 *   - /groq/*       -> https://api.groq.com/*
 *   - /gemini/*     -> https://generativelanguage.googleapis.com/*
 *   - /openai/*     -> https://api.openai.com/*
 *   - /anthropic/*  -> https://api.anthropic.com/*
 */

export default {
  async fetch(request, env) {
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
    } else if (pathname === "/" || pathname === "/health") {
      return new Response(JSON.stringify({ status: "UP", service: "Lingua Optima AI Proxy" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    } else {
      return new Response(
        JSON.stringify({
          error: "Unknown route. Use /groq/*, /gemini/*, /openai/*, or /anthropic/*",
        }),
        { status: 404, headers: { "Content-Type": "application/json" } }
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
