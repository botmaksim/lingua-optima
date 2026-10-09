/**
 * @file AIProvider.java
 * @brief Supported AI and LLM inference providers in the fallback chain.
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Supported AI and LLM inference providers in the fallback chain.
 */
public enum AIProvider {
    /** @brief Constant or enum value representing groq in AIProvider. */
    GROQ,
    /** @brief Constant or enum value representing gemini in AIProvider. */
    GEMINI,
    /** @brief Constant or enum value representing openai in AIProvider. */
    OPENAI,
    /** @brief Constant or enum value representing anthropic in AIProvider. */
    ANTHROPIC,
    /** @brief Constant or enum value representing deepseek in AIProvider. */
    DEEPSEEK,
    /** @brief Constant or enum value representing qwen in AIProvider. */
    QWEN,
    /** @brief Constant or enum value representing kimi in AIProvider. */
    KIMI
}
