/**
 * @file ScoringServiceTest.java
 * @brief Unit and slice test suite for ScoringService.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.service.ai.AIBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for ScoringService.
 */
@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    /** @brief Test fixture or mock dependency for ai broker service. */
    @Mock
    private AIBrokerService aiBrokerService;

    /** @brief Test fixture or mock dependency for object mapper. */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    /** @brief Test fixture or mock dependency for scoring service. */
    @InjectMocks
    private ScoringService scoringService;

    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in ScoringServiceTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    /**
     * @brief Verifies unit test scenario: score grammar task valid json.
     */
    @Test
    void testScoreGrammarTaskValidJson() {
        String json = "{\"score\": 88.0, \"feedback\": \"Minor tense issue\"}";
        when(aiBrokerService.checkGrammar(anyString(), eq(user))).thenReturn(json);

        Map<String, Object> result = scoringService.scoreGrammarTask("My sentence", "Correct sentence", user);
        assertNotNull(result);
        assertEquals(88.0, scoringService.extractScore(result));
        assertEquals("Minor tense issue", result.get("feedback"));
    }

    /**
     * @brief Verifies unit test scenario: score grammar task invalid json fallback.
     */
    @Test
    void testScoreGrammarTaskInvalidJsonFallback() {
        when(aiBrokerService.checkGrammar(anyString(), eq(user))).thenReturn("Not a valid json response");

        Map<String, Object> result = scoringService.scoreGrammarTask("My sentence", "Correct", user);
        assertNotNull(result);
        assertEquals(75.0, scoringService.extractScore(result));
    }

    /**
     * @brief Verifies unit test scenario: score essay valid json.
     */
    @Test
    void testScoreEssayValidJson() {
        String json = """
            {
              \"taskAchievement\": 8.5,
              \"coherence\": 8.0,
              \"lexicalResource\": 7.5,
              \"grammarRange\": 8.0,
              \"overallScore\": 8.0,
              \"feedback\": \"Excellent essay structure\"
            }
            """;
        when(aiBrokerService.scoreEssay(anyString(), eq(user))).thenReturn(json);

        Map<String, Object> result = scoringService.scoreEssay("Essay body...", "B2", user);
        assertNotNull(result);
        assertEquals(80.0, scoringService.extractScore(result));
    }

    /**
     * @brief Verifies unit test scenario: score essay invalid json fallback.
     */
    @Test
    void testScoreEssayInvalidJsonFallback() {
        when(aiBrokerService.scoreEssay(anyString(), eq(user))).thenReturn("Malformed response");

        Map<String, Object> result = scoringService.scoreEssay("Essay body...", "B2", user);
        assertNotNull(result);
        assertEquals(70.0, scoringService.extractScore(result));
    }

    /**
     * @brief Verifies unit test scenario: extract score null and default.
     */
    @Test
    void testExtractScoreNullAndDefault() {
        assertEquals(0.0, scoringService.extractScore(null));
        assertEquals(70.0, scoringService.extractScore(Map.of("other", "field")));
    }
}
