package com.linguaoptima.api.service.ai;

public interface AIProvider {
    String complete(String prompt) throws Exception;
    String getProviderName();
}
