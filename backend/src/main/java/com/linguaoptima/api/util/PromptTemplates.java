package com.linguaoptima.api.util;

public final class PromptTemplates {

    private PromptTemplates() {}

    public static final String TASK_GENERATION_SYSTEM =
        "You are an expert English teacher creating targeted exercises.";

    public static String buildTaskGenerationPrompt(String cefrLevel, String grammarTopic, String domain,
                                                   String taskType, String difficulty, int numberOfQuestions) {
        return """
            Generate an English grammar exercise based on the following parameters:
            - CEFR Level: %s
            - Grammar Topic: %s
            - Domain/Context: %s
            - Task Type: %s
            - Difficulty: %s
            - Number of Questions: %d

            Return ONLY a valid JSON object with the following structure:
            {
              "content": "Overall instructions or context passage",
              "questions": [
                {
                  "id": 1,
                  "text": "Question text here...",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "difficulty": 2,
                  "grammarRule": "Grammar rule tested"
                }
              ],
              "answerKey": [
                {
                  "questionId": 1,
                  "correctOption": "Option A",
                  "explanation": "Why this is correct"
                }
              ]
            }
            """.formatted(cefrLevel, grammarTopic, domain, taskType, difficulty, numberOfQuestions);
    }

    public static final String ESSAY_SCORING_SYSTEM =
        "You are a strict Cambridge/IELTS English examiner.";

    public static String buildEssayScoringPrompt(String cefrLevel, String essayText) {
        return """
            Evaluate the following English essay for a student at the %s level.

            Essay:
            %s

            Evaluate based on the standard rubric (0-10 scale). Return ONLY a valid JSON object with this structure:
            {
              "taskAchievement": 8.0,
              "coherence": 7.5,
              "lexicalResource": 7.0,
              "grammarRange": 8.5,
              "overallScore": 7.75,
              "feedback": "Detailed general feedback here...",
              "corrections": [
                { "original": "bad sentence", "corrected": "good sentence", "explanation": "why it was wrong" }
              ]
            }
            """.formatted(cefrLevel, essayText);
    }

    public static final String GRAMMAR_CHECKING_SYSTEM =
        "You are an automated grading assistant.";

    public static String buildGrammarCheckingPrompt(String studentText, String answerKey) {
        return """
            Compare the student's text against the official answer key and score it.

            Student Text: %s
            Answer Key: %s

            Identify mistakes, provide corrections, and calculate a score from 0 to 100. Return ONLY a valid JSON object:
            {
              "score": 85.0,
              "feedback": "Detailed evaluation feedback",
              "errors": [ { "type": "grammar/spelling", "description": "Description of mistake" } ],
              "corrections": [ { "original": "mistake", "corrected": "correction" } ]
            }
            """.formatted(studentText, answerKey);
    }
}
