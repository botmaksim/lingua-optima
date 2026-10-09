package com.linguaoptima.api.service.ai;

/**
 * @file AIProvider.java
 * @brief Common interface for large language model providers.
 */
public interface AIProvider {

    /**
     * @brief Dispatches a prompt to the model and returns the generated textual response.
     * @param prompt Plaintext input instructions or questionnaire.
     * @return Generated model output string.
     * @throws Exception if network communication or API authentication fails.
     */
    String complete(String prompt) throws Exception;

    /**
     * @brief Returns the unique identifier name of the AI provider.
     * @return Provider name string (e.g. "GROQ", "GEMINI", "OPENAI", "ANTHROPIC").
     */
    String getProviderName();
}
