/**
 * @file aiModels.ts
 * @brief Up-to-date 2026 AI provider and model catalog for student, teacher, and BYOK profile configuration.
 */

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
  /** @brief Available modern models for this provider. */
  models: AIModelOption[];
}

/**
 * @brief Complete catalog of AI providers and their latest 2026 models.
 */
export const AI_PROVIDER_CATALOG: AIProviderOption[] = [
  {
    id: 'DEEPSEEK',
    name: 'DeepSeek',
    defaultModel: 'deepseek-chat',
    models: [
      { id: 'deepseek-chat', label: 'DeepSeek V3.2 (Chat)', badge: 'Fast & Direct' },
      { id: 'deepseek-reasoner', label: 'DeepSeek R1 (Reasoner)', badge: 'Deep Reasoning' },
    ],
  },
  {
    id: 'QWEN',
    name: 'Alibaba Qwen (DashScope)',
    defaultModel: 'qwen3-235b-a22b',
    models: [
      { id: 'qwen3-235b-a22b', label: 'Qwen 3 235B A22B', badge: 'Flagship MoE' },
      { id: 'qwen3-32b', label: 'Qwen 3 32B', badge: 'Balanced' },
      { id: 'qwq-plus', label: 'QwQ Plus', badge: 'Reasoning' },
      { id: 'qwen-max-latest', label: 'Qwen Max Latest', badge: 'High Accuracy' },
      { id: 'qwen-plus-latest', label: 'Qwen Plus Latest', badge: 'Low Latency' },
    ],
  },
  {
    id: 'KIMI',
    name: 'Moonshot Kimi',
    defaultModel: 'kimi-k2-0711-preview',
    models: [
      { id: 'kimi-k2-0711-preview', label: 'Kimi K2 (1T MoE)', badge: 'Flagship' },
      { id: 'kimi-latest', label: 'Kimi Latest (128K)', badge: 'Auto-Context' },
      { id: 'kimi-thinking-preview', label: 'Kimi Thinking Preview', badge: 'Reasoning' },
      { id: 'moonshot-v1-128k', label: 'Moonshot v1 128K', badge: 'Long Context' },
    ],
  },
  {
    id: 'GEMINI',
    name: 'Google Gemini',
    defaultModel: 'gemini-2.5-flash',
    models: [
      { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash', badge: 'Default Fast' },
      { id: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro', badge: 'Advanced Reasoning' },
      { id: 'gemini-3-flash-preview', label: 'Gemini 3.0 Flash Preview', badge: 'Next-Gen Speed' },
      { id: 'gemini-3.1-pro-preview', label: 'Gemini 3.1 Pro Preview', badge: 'Next-Gen Flagship' },
    ],
  },
  {
    id: 'GROQ',
    name: 'Groq LPU Cloud',
    defaultModel: 'llama-3.3-70b-versatile',
    models: [
      { id: 'llama-3.3-70b-versatile', label: 'Llama 3.3 70B Versatile', badge: 'Default Ultra-Fast' },
      { id: 'meta-llama/llama-4-maverick-17b-128e-instruct', label: 'Llama 4 Maverick 17B-128E', badge: 'Llama 4 MoE' },
      { id: 'meta-llama/llama-4-scout-17b-16e-instruct', label: 'Llama 4 Scout 17B-16E', badge: 'Ultra-Low Latency' },
      { id: 'deepseek-r1-distill-llama-70b', label: 'DeepSeek R1 Distill Llama 70B', badge: 'Reasoning on LPU' },
      { id: 'qwen-qwq-32b', label: 'Qwen QwQ 32B (Groq)', badge: 'Fast Reasoning' },
    ],
  },
  {
    id: 'OPENAI',
    name: 'OpenAI',
    defaultModel: 'gpt-4.1-mini',
    models: [
      { id: 'gpt-5', label: 'GPT-5', badge: 'Flagship' },
      { id: 'gpt-5-mini', label: 'GPT-5 Mini', badge: 'Fast & Smart' },
      { id: 'gpt-4.1', label: 'GPT-4.1', badge: 'High Precision' },
      { id: 'gpt-4.1-mini', label: 'GPT-4.1 Mini', badge: 'Cost-Efficient' },
      { id: 'o4-mini', label: 'o4-mini', badge: 'Reasoning' },
      { id: 'o3', label: 'o3', badge: 'Deep Reasoning' },
    ],
  },
  {
    id: 'ANTHROPIC',
    name: 'Anthropic Claude',
    defaultModel: 'claude-sonnet-4-6',
    models: [
      { id: 'claude-sonnet-4-6', label: 'Claude Sonnet 4.6', badge: 'Recommended' },
      { id: 'claude-opus-4-6', label: 'Claude Opus 4.6', badge: 'Flagship' },
      { id: 'claude-3-7-sonnet-latest', label: 'Claude 3.7 Sonnet', badge: 'Hybrid Thinking' },
      { id: 'claude-3-5-haiku-latest', label: 'Claude 3.5 Haiku', badge: 'Fast' },
    ],
  },
];

/**
 * @brief Retrieves the list of models for a given AI provider.
 * @param provider Provider identifier.
 * @return Array of AIModelOption entries.
 */
export const getModelsForProvider = (provider: AIProviderType): AIModelOption[] => {
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
