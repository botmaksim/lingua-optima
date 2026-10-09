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
        return buildTaskGenerationPrompt(cefrLevel, grammarTopic, domain, taskType, difficulty, numberOfQuestions, null, null);
    }

    /**
     * @brief Constructs task generation prompt with injected grammar rules and target vocabulary context.
     * @param cefrLevel Target CEFR level.
     * @param grammarTopic Grammar topic under test.
     * @param domain Real-world situational context.
     * @param taskType Task format (e.g. MULTIPLE_CHOICE, FILL_IN_BLANK).
     * @param difficulty Difficulty tier.
     * @param numberOfQuestions Target question count.
     * @param targetRule Pedagogical grammar rules and structural formulas to enforce.
     * @param targetVocabulary Target vocabulary words, expressions, and collocations to integrate.
     * @return Formatted prompt string.
     */
    public static String buildTaskGenerationPrompt(String cefrLevel, String grammarTopic, String domain,
                                                   String taskType, String difficulty, int numberOfQuestions,
                                                   String targetRule, String targetVocabulary) {
        StringBuilder contextBuilder = new StringBuilder();
        if (targetRule != null && !targetRule.isBlank()) {
            contextBuilder.append("\n- TARGET GRAMMAR RULE & FORMULA TO TEST:\n").append(targetRule.trim()).append("\n");
        }
        if (targetVocabulary != null && !targetVocabulary.isBlank()) {
            contextBuilder.append("\n- TARGET VOCABULARY & COLLOCATIONS TO INCORPORATE:\n").append(targetVocabulary.trim()).append("\n");
        }
        String extraContext = contextBuilder.toString();

        if ("ESSAY".equalsIgnoreCase(taskType)) {
            return """
                Generate an English essay writing assignment and prompt based on the following parameters:
                - CEFR Level: %s
                - Topic/Focus: %s
                - Domain/Context: %s
                - Target Difficulty: %s%s

                CRITICAL REQUIREMENTS:
                1. This is an open-ended ESSAY writing assignment, NOT a multiple-choice or gap-fill grammar exercise.
                2. Do NOT include numbered blanks, bracketed options, or multiple-choice choices in the text.
                3. The "content" field must contain a rich, student-facing essay topic with background context, debate perspectives, and instructions to compose an essay of at least 250 words. If target vocabulary or grammar rules are provided above, explicitly instruct the student to incorporate them.
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
                """.formatted(cefrLevel, grammarTopic, domain, difficulty, extraContext, grammarTopic);
        }

        if ("REWRITE".equalsIgnoreCase(taskType)) {
            return """
                Generate an English sentence rewriting exercise based on the following parameters:
                - CEFR Level: %s
                - Grammar Topic: %s
                - Domain/Context: %s
                - Task Type: REWRITE (Sentence Transformation)
                - Difficulty: %s
                - Number of Questions: %d%s

                CRITICAL REQUIREMENTS:
                1. This is a SENTENCE REWRITING exercise (Sentence Transformation).
                2. For each question, provide an original sentence and instruct the student to rewrite it according to the target grammar structure (e.g., 'Rewrite the following sentence using used to or would: ...', 'Rewrite in the passive voice: ...', 'Rewrite using reported speech: ...').
                3. CRITICAL: Do NOT provide multiple-choice options. The student must write/type the answer themselves. The "options" array MUST be empty [].
                4. The "correctAnswer" field must be the complete, ideal rewritten sentence, with acceptable alternatives separated by ' / '.
                5. The "content" field must contain clear, student-facing exercise instructions, NEVER system meta-instructions.
                6. Do NOT echo, quote, or repeat these system parameters, prompts, or instructions in any JSON field.
                7. Ensure all sentences, contexts, characters, and scenarios are unique, fresh, and creative.

                Return ONLY a valid JSON object with the following structure:
                {
                  "content": "Rewrite the following sentences using the specified grammatical rules and structures.",
                  "questions": [
                    {
                      "id": 1,
                      "text": "Rewrite using 'used to' or 'would': When I lived in Kyoto, I regularly visited the quiet bamboo groves every Sunday morning.",
                      "options": [],
                      "correctAnswer": "When I lived in Kyoto, I used to visit the quiet bamboo groves every single Sunday morning. / When I lived in Kyoto, I would visit the quiet bamboo groves every single Sunday morning.",
                      "difficulty": 3,
                      "grammarRule": "Past Habitual Actions ('used to' vs 'would')"
                    }
                  ],
                  "answerKey": [
                    {
                      "questionId": 1,
                      "questionOrder": 1,
                      "correctOption": "When I lived in Kyoto, I used to visit the quiet bamboo groves every single Sunday morning.",
                      "explanation": "Both 'used to' and 'would' can express repeated past actions, but 'used to' emphasizes past habits that no longer occur."
                    }
                  ]
                }
                """.formatted(cefrLevel, grammarTopic, domain, difficulty, numberOfQuestions, extraContext);
        }

        if ("OPEN_BRACKETS".equalsIgnoreCase(taskType)) {
            return """
                Generate an English grammar exercise for opening brackets ('раскрытие скобок') based on the following parameters:
                - CEFR Level: %s
                - Grammar Topic: %s
                - Domain/Context: %s
                - Task Type: OPEN_BRACKETS (Put the verbs or phrases in brackets into the correct tense or form)
                - Difficulty: %s
                - Number of Questions: %d%s

                CRITICAL REQUIREMENTS:
                1. This is an OPEN BRACKETS exercise ('раскрытие скобок').
                2. For each question, provide a sentence containing a base-form verb or root phrase in parentheses (e.g. '(visit)', '(not / see)', '(already / leave)', '(be)'), followed by a blank or gap (e.g. '_________').
                3. CRITICAL: Do NOT provide multiple-choice options. The student must write/type the correct conjugated or transformed form of the word in brackets. The "options" array MUST be empty [].
                4. The "correctAnswer" field must contain ONLY the correct form of the word in brackets (e.g. 'used to visit' or 'had already left' or acceptable alternatives separated by ' / ').
                5. The "content" field must contain clear, student-facing exercise instructions, NEVER system meta-instructions.
                6. Do NOT echo, quote, or repeat these system parameters, prompts, or instructions in any JSON field.
                7. Ensure all sentences, contexts, characters, and scenarios are unique, fresh, and creative.

                Return ONLY a valid JSON object with the following structure:
                {
                  "content": "Put the verbs or phrases in brackets into the correct grammatical form to complete the sentences.",
                  "questions": [
                    {
                      "id": 1,
                      "text": "By the time Sarah arrived at the station, the train (already / leave) _________.",
                      "options": [],
                      "correctAnswer": "had already left",
                      "difficulty": 2,
                      "grammarRule": "Past Perfect with 'already'"
                    }
                  ],
                  "answerKey": [
                    {
                      "questionId": 1,
                      "questionOrder": 1,
                      "correctOption": "had already left",
                      "explanation": "Past Perfect is required because the departure of the train happened before the past arrival time."
                    }
                  ]
                }
                """.formatted(cefrLevel, grammarTopic, domain, difficulty, numberOfQuestions, extraContext);
        }

        boolean isMcq = "MCQ".equalsIgnoreCase(taskType);
        String optionsRequirement = isMcq
            ? "Every question MUST include 4 distinct plausible choices in the \"options\" array [\"Option A\", \"Option B\", \"Option C\", \"Option D\"]."
            : "The \"options\" array can contain 4 choices or be empty [] if testing open cloze fill-in-the-blanks.";

        return """
            Generate an English grammar exercise based on the following parameters:
            - CEFR Level: %s
            - Grammar Topic: %s
            - Domain/Context: %s
            - Task Type: %s
            - Difficulty: %s
            - Number of Questions: %d%s

            CRITICAL QUALITY CONTRACT:
            1. Every question MUST explicitly evaluate the target grammar topic and rules specified above.
            2. %s
            3. The reading content, question sentences, and options MUST actively utilize and contextualize the target vocabulary if provided.
            4. The "content" field must contain ONLY student-facing reading material or a clear assignment topic, NEVER system meta-instructions.
            5. Do NOT echo, quote, or repeat these system parameters, prompts, or instructions in any JSON field.
            6. Ensure all sentences, contexts, characters, and scenarios are unique, fresh, and creative.

            Return ONLY a valid JSON object with the following structure:
            {
              "content": "A natural reading context or exercise topic for the student",
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
            """.formatted(cefrLevel, grammarTopic, domain, taskType, difficulty, numberOfQuestions, extraContext, optionsRequirement);
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
                { "original": "bad sentence", "corrected": "good sentence", "explanation": "why it was wrong", "grammarRule": "Subject-verb agreement" }
              ],
              "weaknesses": ["Specific stylistic or grammatical weaknesses to improve"],
              "strengths": ["Strong points observed in the essay"],
              "recommendations": "Actionable pedagogical advice on what concepts to review and practice next",
              "suggestedTopics": ["Topic A", "Topic B"]
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

            Identify mistakes, provide sentence-by-sentence corrections, calculate a score from 0 to 100, and analyze what grammar gaps or topics the student needs to brush up on.
            Return ONLY a valid JSON object with the following structure:
            {
              "score": 85.0,
              "feedback": "Detailed evaluation feedback summarizing overall performance",
              "items": [
                {
                  "questionNumber": 1,
                  "sentence": "The exercise sentence or prompt",
                  "studentAnswer": "student's answer",
                  "correctAnswer": "official correct answer",
                  "isCorrect": true,
                  "explanation": "Clear explanation of the grammatical rule and why this answer is correct or incorrect",
                  "grammarRule": "Grammar rule tested"
                }
              ],
              "weaknesses": ["Specific topic or rule where mistakes occurred"],
              "strengths": ["Topics mastered correctly"],
              "recommendations": "Actionable pedagogical advice on what concepts to review and practice next",
              "suggestedTopics": ["Topic A", "Topic B"]
            }
            """.formatted(studentText, answerKey);
    }
}
