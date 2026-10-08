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

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private AIBrokerService aiBrokerService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ScoringService scoringService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void testScoreGrammarTaskValidJson() {
        String json = "{\"score\": 88.0, \"feedback\": \"Minor tense issue\"}";
        when(aiBrokerService.checkGrammar(anyString(), eq(user))).thenReturn(json);

        Map<String, Object> result = scoringService.scoreGrammarTask("My sentence", "Correct sentence", user);
        assertNotNull(result);
        assertEquals(88.0, scoringService.extractScore(result));
        assertEquals("Minor tense issue", result.get("feedback"));
    }

    @Test
    void testScoreGrammarTaskInvalidJsonFallback() {
        when(aiBrokerService.checkGrammar(anyString(), eq(user))).thenReturn("Not a valid json response");

        Map<String, Object> result = scoringService.scoreGrammarTask("My sentence", "Correct", user);
        assertNotNull(result);
        assertEquals(75.0, scoringService.extractScore(result));
    }

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
        assertEquals(80.0, scoringService.extractScore(result)); // 8.0 * 10
    }

    @Test
    void testScoreEssayInvalidJsonFallback() {
        when(aiBrokerService.scoreEssay(anyString(), eq(user))).thenReturn("Malformed response");

        Map<String, Object> result = scoringService.scoreEssay("Essay body...", "B2", user);
        assertNotNull(result);
        assertEquals(70.0, scoringService.extractScore(result));
    }

    @Test
    void testExtractScoreNullAndDefault() {
        assertEquals(0.0, scoringService.extractScore(null));
        assertEquals(70.0, scoringService.extractScore(Map.of("other", "field")));
    }
}
