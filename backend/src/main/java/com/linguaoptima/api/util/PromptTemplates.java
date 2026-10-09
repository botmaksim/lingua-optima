/**
 * @file PromptTemplates.java
 * @brief Structured prompt generators ensuring strict JSON schema compliance from downstream AI models.
 */
package com.linguaoptima.api.util;

/**
 * @brief Structured prompt generators ensuring strict JSON schema compliance from downstream AI models.
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    /**
     * @brief System instruction prompt for task generation models.
     */
    public static final String TASK_GENERATION_SYSTEM =
        "You are an expert English teacher creating targeted exercises.";

    /**
     * @brief Constructs task generation prompt with strict JSON formatting contract.
     * @param cefrLevel Target CEFR level.
     * @param grammarTopic Grammar topic under test.
     * @param domain Real-world situational context.
     * @param taskType Task format (e.g. MULTIPLE_CHOICE, FILL_IN_BLANK).
     * @param difficulty Difficulty tier.
     * @param numberOfQuestions Target question count.
     * @return Formatted prompt string.
     */
    public static String buildTaskGenerationPrompt(String cefrLevel, String grammarTopic, String domain,
                                                   String taskType, String difficulty, int numberOfQuestions) {
        if ("ESSAY".equalsIgnoreCase(taskType)) {
            return """
                Generate an English essay writing assignment and prompt based on the following parameters:
                - CEFR Level: %s
                - Topic/Focus: %s
                - Domain/Context: %s
                - Target Difficulty: %s

                CRITICAL REQUIREMENTS:
                1. This is an open-ended ESSAY writing assignment, NOT a multiple-choice or gap-fill grammar exercise.
                2. Do NOT include numbered blanks, bracketed options, or multiple-choice choices in the text.
                3. The "content" field must contain a rich, student-facing essay topic with background context, debate perspectives, and instructions to compose an essay of at least 250 words.
                4. The "questions" array must contain 2 to 4 guiding discussion prompts or structural questions to help the student organize their essay arguments (each with empty options array []).
                5. Do NOT echo, quote, or repeat these system parameters, prompts, or instructions in any JSON field.

                Return ONLY a valid JSON object with the following structure:
                {
                  "content": "An engaging background scenario and clear essay task instructions for the student...",
                  "questions": [
                    {
                      "id": 1,
                      "text": "Guiding discussion question for the student's essay...",
                      "options": [],
                      "correctAnswer": "Open-ended essay response",
                      "difficulty": 3,
                      "grammarRule": "%s"
                    }
                  ],
                  "answerKey": []
                }
                """.formatted(cefrLevel, grammarTopic, domain, difficulty, grammarTopic);
        }

        return """
            Generate an English grammar exercise based on the following parameters:
            - CEFR Level: %s
            - Grammar Topic: %s
            - Domain/Context: %s
            - Task Type: %s
            - Difficulty: %s
            - Number of Questions: %d

            CRITICAL: Do NOT echo, quote, or repeat these system parameters, prompts, or instructions in any JSON field.
            The "content" field must contain ONLY student-facing reading material or a clear assignment topic, NEVER system meta-instructions.

            Return ONLY a valid JSON object with the following structure:
            {
              "content": "A natural reading context or essay topic for the student",
              "questions": [
                {
                  "id": 1,
                  "text": "Question text here...",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctAnswer": "Option A",
                  "difficulty": 2,
                  "grammarRule": "Grammar rule tested"
                }
              ],
              "answerKey": [
                {
                  "questionId": 1,
                  "questionOrder": 1,
                  "correctOption": "Option A",
                  "explanation": "Why this is correct"
                }
              ]
            }
            """.formatted(cefrLevel, grammarTopic, domain, taskType, difficulty, numberOfQuestions);
    }

    /**
     * @brief System instruction prompt for essay scoring models.
     */
    public static final String ESSAY_SCORING_SYSTEM =
        "You are a strict Cambridge/IELTS English examiner.";

    /**
     * @brief Constructs an essay grading prompt returning IELTS/CEFR 4-pillar rubric in JSON.
     * @param cefrLevel Target benchmark level.
     * @param essayText Student's submitted essay.
     * @return Formatted evaluation prompt string.
     */
    public static String buildEssayScoringPrompt(String cefrLevel, String essayText) {
        return """
            Evaluate the following English essay for a student at the %s level.

            Essay:
            %s

            CRITICAL: Do NOT echo, quote, or repeat these system instructions, prompts, or scoring parameters in the "feedback" or anywhere in the JSON output.
            The "feedback" field must contain ONLY student-facing evaluation and advice, NEVER system meta-instructions or prompt text.

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

    /**
     * @brief System instruction prompt for grammar checking models.
     */
    public static final String GRAMMAR_CHECKING_SYSTEM =
        "You are an automated grading assistant.";

    /**
     * @brief Constructs a grammar checking prompt comparing student answers to an answer key.
     * @param studentText Student's submitted text.
     * @param answerKey Expected answer key.
     * @return Formatted evaluation prompt string.
     */
    public static String buildGrammarCheckingPrompt(String studentText, String answerKey) {
        return """
            Compare the student's text against the official answer key and score it.

            Student Text: %s
            Answer Key: %s

            CRITICAL: Do NOT echo, quote, or repeat these system instructions, prompts, or grading parameters in the "feedback" or anywhere in the JSON output.
            The "feedback" field must contain ONLY student-facing evaluation and advice, NEVER system meta-instructions or prompt text.

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
