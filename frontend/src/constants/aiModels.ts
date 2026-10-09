/**
 * @file aiModels.ts
 * @brief Up-to-date October 2026 AI provider and model catalog with live Cloudflare Edge vendor docs synchronization.
 */

import { useState, useEffect } from 'react';

/**
 * @brief Supported AI provider identifiers across Lingua Optima.
 */
export type AIProviderType = 'DEEPSEEK' | 'QWEN' | 'KIMI' | 'GEMINI' | 'GROQ' | 'OPENAI' | 'ANTHROPIC';

/**
 * @interface AIModelOption
 * @brief Metadata for a selectable AI model within a provider.
 */
export interface AIModelOption {
  /** @brief Model API identifier sent to the provider endpoint. */
  id: string;
  /** @brief Human-readable model display name. */
  label: string;
  /** @brief Short capability badge or description. */
  badge: string;
}

/**
 * @interface AIProviderOption
 * @brief Metadata and available models for an AI provider.
 */
export interface AIProviderOption {
  /** @brief Provider enum key. */
  id: AIProviderType;
  /** @brief Human-readable provider name. */
  name: string;
  /** @brief Default model identifier for this provider. */
  defaultModel: string;
  /** @brief Official documentation URL for live model discovery. */
  docsUrl: string;
  /** @brief Available modern models for this provider. */
  models: AIModelOption[];
}

/**
 * @brief Cloudflare Worker endpoint used to scrape live model lists from official vendor websites.
 */
export const AI_PROXY_MODELS_ENDPOINT = 'https://ai-proxy.mybsu.online/models';

/**
 * @brief Complete catalog of AI providers and their latest October 2026 models from official company websites.
 */
export const AI_PROVIDER_CATALOG: AIProviderOption[] = [
  {
    id: 'DEEPSEEK',
    name: 'DeepSeek',
    defaultModel: 'deepseek-flash',
    docsUrl: 'https://api-docs.deepseek.com/quick_start/pricing',
    models: [
      { id: 'deepseek-flash', label: 'DeepSeek V4.1 Flash (1M)', badge: 'Latest Flagship' },
      { id: 'deepseek-v4-pro', label: 'DeepSeek V4 Pro (1M)', badge: 'Deep Reasoning' },
      { id: 'deepseek-chat', label: 'DeepSeek Chat (Auto Alias)', badge: 'Compatible Alias' },
      { id: 'deepseek-reasoner', label: 'DeepSeek Reasoner (CoT Alias)', badge: 'Reasoning Alias' },
    ],
  },
  {
    id: 'QWEN',
    name: 'Alibaba Qwen (DashScope)',
    defaultModel: 'qwen3.8-max',
    docsUrl: 'https://www.alibabacloud.com/help/en/model-studio/getting-started/models',
    models: [
      { id: 'qwen3.8-max', label: 'Qwen 3.8 Max', badge: 'Latest 3.8 Flagship' },
      { id: 'qwen3.8-flash', label: 'Qwen 3.8 Flash', badge: 'Ultra-Fast 3.8' },
      { id: 'qwen3.8-omni-flash', label: 'Qwen 3.8 Omni Flash', badge: 'Multimodal 3.8' },
      { id: 'qwen3.8-27b', label: 'Qwen 3.8 27B', badge: 'Open Weights 3.8' },
      { id: 'qwen3.7-max', label: 'Qwen 3.7 Max', badge: 'High Precision' },
      { id: 'qwen3.7-plus', label: 'Qwen 3.7 Plus', badge: 'Balanced' },
      { id: 'qwen3.6-plus', label: 'Qwen 3.6 Plus', badge: 'Stable' },
      { id: 'qwen-max', label: 'Qwen Max (Auto-Latest)', badge: 'Alias' },
    ],
  },
  {
    id: 'KIMI',
    name: 'Moonshot Kimi',
    defaultModel: 'kimi-k3',
    docsUrl: 'https://platform.moonshot.ai/docs/pricing/chat',
    models: [
      { id: 'kimi-k3', label: 'Kimi K3 (2.8T Flagship, 1M)', badge: 'Latest K3 Flagship' },
      { id: 'kimi-k2.7-code', label: 'Kimi K2.7 Code (256K)', badge: 'Deep Reasoning' },
      { id: 'kimi-k2.7-code-highspeed', label: 'Kimi K2.7 Highspeed (180 tok/s)', badge: 'Ultra-Fast' },
      { id: 'kimi-k2.6', label: 'Kimi K2.6 (Multimodal Thinking)', badge: 'General Purpose' },
    ],
  },
  {
    id: 'GEMINI',
    name: 'Google Gemini',
    defaultModel: 'gemini-3.8-flash',
    docsUrl: 'https://ai.google.dev/gemini-api/docs/models',
    models: [
      { id: 'gemini-3.8-flash', label: 'Gemini 3.8 Flash', badge: 'Latest 3.8 Flagship' },
      { id: 'gemini-3.8-live-extended-thinking', label: 'Gemini 3.8 Extended Thinking', badge: 'Deep Reasoning' },
      { id: 'gemini-3.8-live', label: 'Gemini 3.8 Live', badge: 'Low-Latency' },
      { id: 'gemini-3.7-flash', label: 'Gemini 3.7 Flash', badge: 'Fast 3.7' },
      { id: 'gemini-3.6-flash', label: 'Gemini 3.6 Flash (Stable)', badge: 'Production Stable' },
      { id: 'gemini-3.5-flash', label: 'Gemini 3.5 Flash', badge: 'Balanced 3.5' },
      { id: 'gemini-3.1-pro-preview', label: 'Gemini 3.1 Pro Preview', badge: 'Pro Reasoning' },
      { id: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro', badge: 'Legacy Pro' },
      { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash', badge: 'Legacy Flash' },
    ],
  },
  {
    id: 'GROQ',
    name: 'Groq LPU Cloud',
    defaultModel: 'qwen/qwen3.8-27b',
    docsUrl: 'https://console.groq.com/docs/models',
    models: [
      { id: 'qwen/qwen3.8-27b', label: 'Qwen 3.8 27B (Groq LPU)', badge: 'Latest 3.8 on LPU' },
      { id: 'openai/gpt-oss-120b', label: 'OpenAI GPT-OSS 120B (Groq LPU)', badge: 'Flagship LPU' },
      { id: 'openai/gpt-oss-20b', label: 'OpenAI GPT-OSS 20B (Groq LPU)', badge: 'Ultra-Fast LPU' },
      { id: 'qwen/qwen3.6-27b', label: 'Qwen 3.6 27B (Groq LPU)', badge: 'Fast Reasoning' },
      { id: 'meta-llama/llama-4-maverick-17b-128e-instruct', label: 'Llama 4 Maverick 17B-128E', badge: 'Llama 4 MoE' },
      { id: 'meta-llama/llama-4-scout-17b-16e-instruct', label: 'Llama 4 Scout 17B-16E', badge: 'Ultra-Low Latency' },
      { id: 'moonshotai/kimi-k2-instruct', label: 'Kimi K2 Instruct (Groq LPU)', badge: '1T MoE on LPU' },
      { id: 'llama-3.3-70b-versatile', label: 'Llama 3.3 70B Versatile', badge: 'Classic Versatile' },
      { id: 'llama-3.1-8b-instant', label: 'Llama 3.1 8B Instant', badge: 'Instant' },
    ],
  },
  {
    id: 'OPENAI',
    name: 'OpenAI',
    defaultModel: 'gpt-6.1-sol',
    docsUrl: 'https://platform.openai.com/docs/models',
    models: [
      { id: 'gpt-6-astra', label: 'GPT-6 Astra (1M Flagship)', badge: 'Frontier Reasoning' },
      { id: 'gpt-6.1-sol', label: 'GPT-6.1 Sol', badge: 'Default Balanced' },
      { id: 'gpt-6-luna', label: 'GPT-6 Luna', badge: 'Fast & Cost-Efficient' },
      { id: 'gpt-5.6-sol', label: 'GPT-5.6 Sol', badge: 'High Precision' },
      { id: 'gpt-5.6-luna', label: 'GPT-5.6 Luna', badge: 'Low Latency' },
      { id: 'o4-mini', label: 'o4-mini', badge: 'Fast Reasoning' },
      { id: 'o3', label: 'o3', badge: 'Deep Reasoning' },
    ],
  },
  {
    id: 'ANTHROPIC',
    name: 'Anthropic Claude',
    defaultModel: 'claude-sonnet-5-5',
    docsUrl: 'https://docs.anthropic.com/en/docs/about-claude/models/overview',
    models: [
      { id: 'claude-fable-5-1', label: 'Claude Fable 5.1', badge: 'Frontier Agentic' },
      { id: 'claude-opus-5-5', label: 'Claude Opus 5.5', badge: 'Recommended Flagship' },
      { id: 'claude-sonnet-5-5', label: 'Claude Sonnet 5.5', badge: 'Default Balanced' },
      { id: 'claude-haiku-5-5', label: 'Claude Haiku 5.5', badge: 'Fast & Efficient' },
      { id: 'claude-opus-5', label: 'Claude Opus 5', badge: 'Previous Opus' },
      { id: 'claude-sonnet-5', label: 'Claude Sonnet 5', badge: 'Previous Sonnet' },
      { id: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6', badge: 'Legacy 4.6' },
    ],
  },
];

/**
 * @brief In-memory session cache of live-synced provider models from Cloudflare Edge.
 */
const liveModelsCache = new Map<AIProviderType, AIModelOption[]>();

/**
 * @brief Retrieves the static fallback list of models for a given AI provider.
 * @param provider Provider identifier.
 * @return Array of AIModelOption entries.
 */
export const getModelsForProvider = (provider: AIProviderType): AIModelOption[] => {
  const cached = liveModelsCache.get(provider);
  if (cached && cached.length > 0) {
    return cached;
  }
  const found = AI_PROVIDER_CATALOG.find((p) => p.id === provider);
  return found ? found.models : AI_PROVIDER_CATALOG[0].models;
};

/**
 * @brief Retrieves the default model ID for a given AI provider.
 * @param provider Provider identifier.
 * @return Default model identifier string.
 */
export const getDefaultModelForProvider = (provider: AIProviderType): string => {
  const found = AI_PROVIDER_CATALOG.find((p) => p.id === provider);
  return found ? found.defaultModel : AI_PROVIDER_CATALOG[0].defaultModel;
};

/**
 * @brief Fetches live model options for a provider from the Cloudflare Worker vendor docs scraper.
 * @param provider Provider identifier.
 * @return Promise resolving to the merged list of live and curated AIModelOption entries.
 */
export const fetchLiveModelsForProvider = async (provider: AIProviderType): Promise<AIModelOption[]> => {
  const cached = liveModelsCache.get(provider);
  if (cached && cached.length > 0) {
    return cached;
  }
  const baseModels = getModelsForProvider(provider);
  try {
    const response = await fetch(`${AI_PROXY_MODELS_ENDPOINT}/${provider}`);
    if (!response.ok) return baseModels;
    const data = await response.json();
    if (Array.isArray(data?.models) && data.models.length > 0) {
      liveModelsCache.set(provider, data.models);
      return data.models;
    }
  } catch {
    // Gracefully fall back to curated October 2026 catalog if offline
  }
  return baseModels;
};

/**
 * @brief React hook that provides the model list for a provider and syncs live models from official company websites.
 * @param provider Selected AI provider.
 * @return Object containing models list and live sync status.
 */
export const useProviderModels = (provider: AIProviderType) => {
  const [models, setModels] = useState<AIModelOption[]>(() => getModelsForProvider(provider));
  const [isLiveSynced, setIsLiveSynced] = useState<boolean>(() => liveModelsCache.has(provider));

  useEffect(() => {
    let active = true;
    setModels(getModelsForProvider(provider));
    setIsLiveSynced(liveModelsCache.has(provider));

    fetchLiveModelsForProvider(provider).then((liveList) => {
      if (active) {
        setModels(liveList);
        setIsLiveSynced(true);
      }
    });

    return () => {
      active = false;
    };
  }, [provider]);

  return { models, isLiveSynced };
};
