package com.linguaoptima.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.service.ai.AIBrokerService;
import com.linguaoptima.api.util.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @file ScoringService.java
 * @brief Service responsible for parsing, grading, and rubric extraction of student submissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final AIBrokerService aiBrokerService;
    private final ObjectMapper objectMapper;

    /**
     * @brief Scores a grammar exercise submission against an expected answer key using AI.
     * @param studentText Student's submitted text.
     * @param answerKey Expected answer key JSON or plain text.
     * @param user User initiating the evaluation.
     * @return Map containing numerical score, item feedback, and explanations.
     */
    public Map<String, Object> scoreGrammarTask(String studentText, String answerKey, User user) {
        String prompt = PromptTemplates.buildGrammarCheckingPrompt(studentText, answerKey);
        String aiResponse = aiBrokerService.checkGrammar(prompt, user);

        try {
            return objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse AI grammar response as JSON, fallback parsing: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("score", 75.0);
            fallback.put("feedback", aiResponse);
            return fallback;
        }
    }

    /**
     * @brief Scores an open-ended essay using the 4 CEFR criteria (Task Achievement, Coherence, Lexical Resource, Grammar).
     * @param essayText The student's submitted essay content.
     * @param cefrLevel Target CEFR level benchmark string.
     * @param user User submitting the essay.
     * @return Map containing rubric scores across criteria and qualitative feedback.
     */
    public Map<String, Object> scoreEssay(String essayText, String cefrLevel, User user) {
        String prompt = PromptTemplates.buildEssayScoringPrompt(cefrLevel, essayText);
        String aiResponse = aiBrokerService.scoreEssay(prompt, user);

        try {
            return objectMapper.readValue(aiResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse AI essay score as JSON: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("taskAchievement", 7.0);
            fallback.put("coherence", 7.0);
            fallback.put("lexicalResource", 7.0);
            fallback.put("grammarRange", 7.0);
            fallback.put("overallScore", 7.0);
            fallback.put("feedback", aiResponse);
            return fallback;
        }
    }

    /**
     * @brief Extracts a unified normalized numerical score (0.0 - 100.0) from a rubric evaluation map.
     * @param scoringResult Rubric output map from grammar or essay evaluation.
     * @return Extracted or normalized score as Double.
     */
    public Double extractScore(Map<String, Object> scoringResult) {
        if (scoringResult == null) return 0.0;
        Object scoreObj = scoringResult.get("score");
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }
        Object overallScoreObj = scoringResult.get("overallScore");
        if (overallScoreObj instanceof Number) {
            return ((Number) overallScoreObj).doubleValue() * 10.0;
        }
        return 70.0;
    }
}
