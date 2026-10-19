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
        assertEquals("Your submission has been evaluated. Review the score and accuracy breakdown above.", result.get("feedback"));
    }

    /**
     * @brief Verifies unit test scenario: score grammar task with markdown fences and prompt leak.
     */
    @Test
    void testScoreGrammarTaskWithMarkdownAndPromptLeak() {
        String wrappedJson = """
            ```json
            {
              "score": 90.0,
              "feedback": "Compare the student's text against the official answer key and identify mistakes."
            }
            ```
            """;
        when(aiBrokerService.checkGrammar(anyString(), eq(user))).thenReturn(wrappedJson);

        Map<String, Object> result = scoringService.scoreGrammarTask("My text", "Key", user);
        assertNotNull(result);
        assertEquals(90.0, scoringService.extractScore(result));
        assertEquals("Your submission has been evaluated. Review the score and accuracy breakdown above.", result.get("feedback"));
    }

    /**
     * @brief Verifies unit test scenario: score essay valid json.
     */
    @Test
    void testScoreEssayValidJson() {
        String json = """
            {
              "taskAchievement": 8.5,
              "coherence": 8.0,
              "lexicalResource": 7.5,
              "grammarRange": 8.0,
              "overallScore": 8.0,
              "feedback": "Excellent essay structure"
            }
            """;
        when(aiBrokerService.scoreEssay(anyString(), eq(user))).thenReturn(json);

        Map<String, Object> result = scoringService.scoreEssay("Essay body...", "B2", user);
        assertNotNull(result);
        assertEquals(80.0, scoringService.extractScore(result));
        assertEquals("Excellent essay structure", result.get("feedback"));
    }

    /**
     * @brief Verifies unit test scenario: score essay with markdown fences and prompt leak in feedback.
     */
    @Test
    void testScoreEssayWithMarkdownAndPromptLeak() {
        String wrappedJson = """
            ```
            {
              "taskAchievement": 8.0,
              "coherence": 7.5,
              "lexicalResource": 7.0,
              "grammarRange": 8.5,
              "overallScore": 7.75,
              "feedback": "Evaluate the following English essay for a student at the B2 level. Cambridge/IELTS English examiner 0-10 scale"
            }
            ```
            """;
        when(aiBrokerService.scoreEssay(anyString(), eq(user))).thenReturn(wrappedJson);

        Map<String, Object> result = scoringService.scoreEssay("Essay body...", "B2", user);
        assertNotNull(result);
        assertEquals(77.5, scoringService.extractScore(result));
        assertEquals("Your essay has been evaluated according to CEFR standards. Review the scoring criteria breakdown above for detailed criteria ratings.", result.get("feedback"));
    }

    /**
     * @brief Verifies unit test scenario: score essay with non-string feedback.
     */
    @Test
    void testScoreEssayWithNonStringOrBlankFeedback() {
        String json = """
            {
              "overallScore": 8.0,
              "feedback": ""
            }
            """;
        when(aiBrokerService.scoreEssay(anyString(), eq(user))).thenReturn(json);

        Map<String, Object> result = scoringService.scoreEssay("Essay body...", "B2", user);
        assertNotNull(result);
        assertEquals("Your essay has been evaluated according to CEFR standards. Review the scoring criteria breakdown above for detailed criteria ratings.", result.get("feedback"));
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
        assertEquals("Your essay has been evaluated according to CEFR standards. Review the scoring criteria breakdown above for detailed criteria ratings.", result.get("feedback"));
    }

    /**
     * @brief Verifies unit test scenario: extract score null and default.
     */
    @Test
    void testExtractScoreNullAndDefault() {
        assertEquals(0.0, scoringService.extractScore(null));
        assertEquals(70.0, scoringService.extractScore(Map.of("other", "field")));
    }

    /**
     * @brief Verifies unit test scenario: score speaking task with full diagnostics payload.
     */
    @Test
    void testScoreSpeakingTaskValidPayload() {
        String payload = """
            [SPEAKING_PRACTICE]
            Overall Score: 94%
            Pronunciation Accuracy: 96%
            Fluency Score: 92%
            Completeness: 100%
            Speech Rate: 135 WPM
            Pacing Score: 98%
            Pacing Diagnostics: Optimal tempo and cadence.

            Spoken Transcript: Consistent pronunciation practice enhances phonetic awareness.

            Reference Passage: Consistent pronunciation practice enhances phonetic awareness.
            """;

        Map<String, Object> result = scoringService.scoreSpeakingTask(payload, "Fallback key", user);
        assertNotNull(result);
        assertEquals(94.0, result.get("score"));
        assertEquals(96.0, result.get("accuracyScore"));
        assertEquals(92.0, result.get("fluencyScore"));
        assertEquals(98.0, result.get("pacingScore"));
        assertEquals(135, result.get("speechRateWpm"));
        assertTrue(result.get("feedback").toString().contains("135 WPM"));

        @SuppressWarnings("unchecked")
        var items = (java.util.List<Map<String, Object>>) result.get("items");
        assertNotNull(items);
        assertEquals(1, items.size());
        assertTrue((Boolean) items.get(0).get("isCorrect"));
        assertEquals("Consistent pronunciation practice enhances phonetic awareness.", items.get(0).get("studentAnswer"));
    }

    /**
     * @brief Verifies unit test scenario: score speaking task with low score and null payload defaults.
     */
    @Test
    void testScoreSpeakingTaskDefaultsAndLowScore() {
        String payload = """
            [SPEAKING_PRACTICE]
            Overall Score: 45%
            Speech Rate: 80 WPM
            """;

        Map<String, Object> result = scoringService.scoreSpeakingTask(payload, "Reference passage", user);
        assertNotNull(result);
        assertEquals(45.0, result.get("score"));

        @SuppressWarnings("unchecked")
        var items = (java.util.List<Map<String, Object>>) result.get("items");
        assertNotNull(items);
        assertFalse((Boolean) items.get(0).get("isCorrect"));
        assertEquals("Reference passage", items.get(0).get("correctAnswer"));

        Map<String, Object> nullResult = scoringService.scoreSpeakingTask(null, null, user);
        assertNotNull(nullResult);
        assertEquals(85.0, nullResult.get("score"));
    }
}

