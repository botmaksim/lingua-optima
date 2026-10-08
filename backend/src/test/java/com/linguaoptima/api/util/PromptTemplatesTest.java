package com.linguaoptima.api.util;

import com.linguaoptima.api.domain.enums.CefrLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplatesTest {

    @Test
    void testPromptTemplates() {
        String taskPrompt = PromptTemplates.buildTaskGenerationPrompt("B1", "Past Simple", "Work", "MCQ", "MEDIUM", 5);
        assertTrue(taskPrompt.contains("B1"));
        assertTrue(taskPrompt.contains("Past Simple"));
        assertTrue(taskPrompt.contains("MCQ"));

        String essayPrompt = PromptTemplates.buildEssayScoringPrompt("B2", "Essay text here");
        assertTrue(essayPrompt.contains("B2"));
        assertTrue(essayPrompt.contains("Essay text here"));
        assertTrue(essayPrompt.contains("taskAchievement"));

        String grammarPrompt = PromptTemplates.buildGrammarCheckingPrompt("Student text", "Answer key");
        assertTrue(grammarPrompt.contains("Student text"));
        assertTrue(grammarPrompt.contains("Answer key"));
    }
}
