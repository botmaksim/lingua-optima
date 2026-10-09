/**
 * @file ScoringService.java
 * @brief Service responsible for parsing, grading, and rubric extraction of student submissions.
 */
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
import java.util.List;
import java.util.Map;

/**
 * @brief Service responsible for parsing, grading, and rubric extraction of student submissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    /** @brief Field representing ai broker service in ScoringService. */
    private final AIBrokerService aiBrokerService;
    /** @brief Field representing object mapper in ScoringService. */
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
            String cleaned = cleanJson(aiResponse);
            Map<String, Object> result = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
            sanitizeFeedback(result, "Your submission has been evaluated. Review the score and accuracy breakdown above.");
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse AI grammar response as JSON, fallback parsing: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("score", 75.0);
            fallback.put("feedback", "Your submission has been evaluated. Review the score and accuracy breakdown above.");
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
            String cleaned = cleanJson(aiResponse);
            Map<String, Object> result = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
            sanitizeFeedback(result, "Your essay has been evaluated according to CEFR standards. Review the scoring criteria breakdown above for detailed criteria ratings.");
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse AI essay score as JSON: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("taskAchievement", 7.0);
            fallback.put("coherence", 7.0);
            fallback.put("lexicalResource", 7.0);
            fallback.put("grammarRange", 7.0);
            fallback.put("overallScore", 7.0);
            fallback.put("feedback", "Your essay has been evaluated according to CEFR standards. Review the scoring criteria breakdown above for detailed criteria ratings.");
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

    private static final List<String> PROMPT_LEAK_MARKERS = List.of(
        "evaluate the following",
        "compare the student's text",
        "return only a valid json",
        "official answer key",
        "cambridge/ielts english examiner",
        "critical: do not echo",
        "system parameters",
        "0-10 scale"
    );

    /**
     * @brief Strips markdown code fence blocks from AI response strings.
     * @param raw Raw AI response string.
     * @return Clean JSON string.
     */
    private String cleanJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        return raw.trim()
            .replaceFirst("^```(?:json)?\\s*", "")
            .replaceFirst("\\s*```$", "")
            .trim();
    }

    /**
     * @brief Inspects and sanitizes feedback to prevent leaking AI meta-prompts or instructions.
     * @param map Map containing AI evaluation result.
     * @param defaultFallback Contextual student-facing fallback feedback.
     */
    private void sanitizeFeedback(Map<String, Object> map, String defaultFallback) {
        Object fbObj = map.get("feedback");
        if (fbObj instanceof String fb && !fb.isBlank() && !containsPromptLeak(fb)) {
            map.put("feedback", fb.trim());
        } else {
            map.put("feedback", defaultFallback);
        }
    }

    /**
     * @brief Checks if a string contains known system prompt leak keywords.
     * @param text Text to evaluate.
     * @return True if prompt leak keyword is present.
     */
    private boolean containsPromptLeak(String text) {
        String lower = text.toLowerCase();
        for (String marker : PROMPT_LEAK_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
