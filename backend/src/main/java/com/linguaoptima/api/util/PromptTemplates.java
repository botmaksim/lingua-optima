/**
 * @file PromptTemplates.java
 * @brief Structured prompt generators ensuring strict JSON schema compliance from downstream AI models.
 */
package com.linguaoptima.api.util;

import com.linguaoptima.api.dto.request.TaskParamsRequest;
import com.linguaoptima.api.dto.response.PronunciationPackResponse;

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
        contextBuilder.append("\n- CEFR LEVEL LEXICAL CALIBRATION:\n")
            .append("All vocabulary, context sentences, distractors, and questions MUST strictly conform to CEFR ")
            .append(cefrLevel)
            .append(". For lower levels (A1, A2, B1), strictly avoid advanced or academic C1/C2 vocabulary. Ensure language is natural, authentic, and perfectly calibrated for ")
            .append(cefrLevel)
            .append(" learners.\n");
        String extraContext = contextBuilder.toString();

        if ("READING".equalsIgnoreCase(taskType)) {
            return buildReadingGenerationPrompt(
                cefrLevel, grammarTopic, domain, difficulty,
                null, null, null, targetVocabulary,
                3, 10, 2, 10, 2, 15
            );
        }

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
     * @brief Constructs reading generation prompt from structured TaskParamsRequest.
     * @param params Parameter request DTO.
     * @param targetRule Optional grammar rule.
     * @param targetVocabulary Optional target vocabulary.
     * @return Formatted AI generation prompt.
     */
    public static String buildReadingGenerationPrompt(TaskParamsRequest params, String targetRule, String targetVocabulary) {
        String topic = params.getGrammarTopic() != null && !params.getGrammarTopic().isBlank()
            ? params.getGrammarTopic() : "Contemporary Global Issues";
        String domain = params.getDomain() != null && !params.getDomain().isBlank()
            ? params.getDomain() : "General";
        String difficulty = params.getDifficulty() != null
            ? params.getDifficulty().name() : "MEDIUM";
        int mcqCount = params.getMcqQuestionsCount() != null ? params.getMcqQuestionsCount() : 3;
        int mcqPoints = params.getMcqPoints() != null && params.getMcqPoints() > 0 ? params.getMcqPoints() : 10;
        int tfCount = params.getTrueFalseQuestionsCount() != null ? params.getTrueFalseQuestionsCount() : 2;
        int tfPoints = params.getTrueFalsePoints() != null && params.getTrueFalsePoints() > 0 ? params.getTrueFalsePoints() : 10;
        int openCount = params.getOpenQuestionsCount() != null ? params.getOpenQuestionsCount() : 2;
        int openPoints = params.getOpenPoints() != null && params.getOpenPoints() > 0 ? params.getOpenPoints() : 15;

        return buildReadingGenerationPrompt(
            params.getCefrLevel().name(),
            topic,
            domain,
            difficulty,
            params.getReadingTitle(),
            params.getReadingLength(),
            params.getReadingParagraphs(),
            targetVocabulary,
            mcqCount,
            mcqPoints,
            tfCount,
            tfPoints,
            openCount,
            openPoints
        );
    }

    /**
     * @brief Constructs an English reading comprehension prompt generating passage text and mixed question categories.
     * @param cefrLevel Target CEFR level.
     * @param topic Reading topic / thematic subject.
     * @param domain Situational real-world domain context.
     * @param difficulty Target difficulty level.
     * @param title Custom reading passage title.
     * @param length Passage length or volume specification.
     * @param paragraphs Paragraph structure or focus guidance.
     * @param targetVocabulary Target vocabulary words or collocations to incorporate.
     * @param mcqCount Number of multiple choice questions.
     * @param mcqPoints Points allocated per multiple choice question.
     * @param tfCount Number of True/False questions.
     * @param tfPoints Points allocated per True/False question.
     * @param openCount Number of sentence completion / open questions.
     * @param openPoints Points allocated per sentence completion / open question.
     * @return Formatted AI generation prompt.
     */
    public static String buildReadingGenerationPrompt(String cefrLevel, String topic, String domain, String difficulty,
                                                     String title, String length, String paragraphs, String targetVocabulary,
                                                     int mcqCount, int mcqPoints, int tfCount, int tfPoints, int openCount, int openPoints) {
        StringBuilder vocabBuilder = new StringBuilder();
        if (targetVocabulary != null && !targetVocabulary.isBlank()) {
            vocabBuilder.append("\n- TARGET VOCABULARY & COLLOCATIONS TO INCORPORATE IN THE TEXT:\n")
                .append(targetVocabulary.trim()).append("\n");
        }
        String resolvedTitle = (title != null && !title.isBlank()) ? title.trim() : topic;
        String resolvedLength = (length != null && !length.isBlank()) ? length.trim() : "Medium (250-400 words)";
        String resolvedParagraphs = (paragraphs != null && !paragraphs.isBlank()) ? paragraphs.trim() : "3 to 4 distinct paragraphs with clear narrative progression";
        int effMcqCount = Math.max(0, mcqCount);
        int effTfCount = Math.max(0, tfCount);
        int effOpenCount = Math.max(0, openCount);
        if (effMcqCount == 0 && effTfCount == 0 && effOpenCount == 0) {
            effMcqCount = 3;
            effTfCount = 2;
            effOpenCount = 2;
        }

        return """
            Generate an English Reading Comprehension assignment consisting of an engaging reading article/story and targeted comprehension questions based strictly on the text.

            PARAMETERS:
            - CEFR Benchmark Level: %s
            - Reading Topic: %s
            - Assignment Title: %s
            - Domain / Context: %s
            - Difficulty Tier: %s
            - Target Passage Length: %s
            - Paragraph Structure & Focus: %s%s

            CRITICAL READING TASK CONTRACT:
            1. "content" FIELD:
               - Must contain the COMPLETE reading passage text.
               - Format the passage into clear, readable paragraphs separated by double newlines (\\n\\n).
               - Language, grammar, and lexical complexity MUST strictly conform to CEFR %s.
               - Naturally integrate the target vocabulary if specified.
               - The "content" field must contain ONLY student-facing reading material, NEVER questions, headings like 'Questions:', or system meta-instructions.

            2. "questions" ARRAY:
               Generate EXACTLY the requested question breakdown based strictly on the facts, arguments, and details in the reading passage:
               - %d Multiple Choice Questions (MCQ):
                 Each question must test comprehension (main ideas, specific details, inferences). "options" array MUST contain 4 plausible choices ["Choice A", "Choice B", "Choice C", "Choice D"]. "correctAnswer" must match the correct option. "points": %d. "grammarRule": "Reading: Multiple Choice".
               - %d True / False / Not Given Questions:
                 Each question presents a statement to verify against the passage. "options" MUST be ["True", "False", "Not Given"]. "correctAnswer" must be "True", "False", or "Not Given". "points": %d. "grammarRule": "Reading: True/False/Not Given".
               - %d Sentence Completion / Information Extraction Questions:
                 Each question prompts the student to complete a sentence or answer a factual inquiry from the text. "options" MUST be empty []. "correctAnswer" must contain the expected concise phrase or fact (with acceptable variants separated by ' / '). "points": %d. "grammarRule": "Reading: Sentence Completion".

            3. "answerKey" ARRAY:
               - For every question, provide an entry with questionId, questionOrder, correctOption, and a clear explanation quoting the exact sentence or paragraph from the passage that justifies the answer.

            4. CRITICAL: Do NOT echo, quote, or repeat these system parameters, prompts, or instructions in any JSON field.

            Return ONLY a valid JSON object with the following structure:
            {
              "content": "Paragraph 1 text...\\n\\nParagraph 2 text...\\n\\nParagraph 3 text...",
              "questions": [
                {
                  "id": 1,
                  "text": "What is the primary factor mentioned in paragraph 1 regarding...?",
                  "options": ["Choice A", "Choice B", "Choice C", "Choice D"],
                  "correctAnswer": "Choice A",
                  "points": %d,
                  "difficulty": 2,
                  "grammarRule": "Reading: Multiple Choice"
                }
              ],
              "answerKey": [
                {
                  "questionId": 1,
                  "questionOrder": 1,
                  "correctOption": "Choice A",
                  "explanation": "In paragraph 1, the text explicitly notes that '...' which confirms Choice A."
                }
              ]
            }
            """.formatted(
                cefrLevel, topic, resolvedTitle, domain, difficulty, resolvedLength, resolvedParagraphs,
                vocabBuilder.toString(), cefrLevel, effMcqCount, mcqPoints, effTfCount, tfPoints, effOpenCount, openPoints, mcqPoints
            );
    }

    /**
     * @brief Constructs speaking practice generation prompt based on TaskParamsRequest.
     * @param params Task generation parameters.
     * @param targetRule Optional target rule or context.
     * @param targetVocabulary Optional target vocabulary.
     * @return Formatted speaking generation prompt string.
     */
    public static String buildSpeakingGenerationPrompt(TaskParamsRequest params, String targetRule, String targetVocabulary) {
        return buildSpeakingGenerationPrompt(params, targetRule, targetVocabulary, null);
    }

    /**
     * @brief Constructs speaking practice generation prompt based on TaskParamsRequest and optional PronunciationPackResponse.
     * @param params Task generation parameters.
     * @param targetRule Optional target rule or context.
     * @param targetVocabulary Optional target vocabulary.
     * @param pack Optional CEFR-calibrated pronunciation drill pack from JSON.
     * @return Formatted speaking generation prompt string.
     */
    public static String buildSpeakingGenerationPrompt(TaskParamsRequest params, String targetRule, String targetVocabulary,
                                                        PronunciationPackResponse pack) {
        String level = params.getCefrLevel() != null ? params.getCefrLevel().name() : "B1";
        String topic = params.getGrammarTopic() != null && !params.getGrammarTopic().isBlank()
            ? params.getGrammarTopic() : "Contemporary English Communication";
        String domain = params.getDomain() != null && !params.getDomain().isBlank()
            ? params.getDomain() : "Daily Life";
        String difficulty = params.getDifficulty() != null ? params.getDifficulty().name() : "MEDIUM";
        String title = params.getSpeakingTitle() != null && !params.getSpeakingTitle().isBlank()
            ? params.getSpeakingTitle() : (topic + " - Pronunciation & Speech Practice");
        String targetSounds = params.getTargetSounds() != null && !params.getTargetSounds().isBlank()
            ? params.getTargetSounds() : (pack != null ? pack.getTitle() : "Natural Rhythm, Intonation & Clear Articulation");
        String length = params.getSpeakingLength() != null && !params.getSpeakingLength().isBlank()
            ? params.getSpeakingLength() : "MEDIUM";
        int totalPoints = params.getTotalPoints() != null && params.getTotalPoints() > 0
            ? params.getTotalPoints() : 100;
        boolean ecoMode = Boolean.TRUE.equals(params.getEcoMode());

        return buildSpeakingGenerationPrompt(level, topic, domain, difficulty, title, targetSounds, length, totalPoints,
            targetVocabulary, pack, ecoMode);
    }

    /**
     * @brief Detailed prompt generator for speaking and pronunciation exercises.
     * @param cefrLevel Target CEFR level.
     * @param topic Topic or theme.
     * @param domain Real-world domain.
     * @param difficulty Difficulty tier.
     * @param title Title of assignment.
     * @param targetSounds Target sounds or phonemes.
     * @param length Length preset.
     * @param totalPoints Total points budget.
     * @param targetVocabulary Target vocabulary words.
     * @return Formatted prompt string.
     */
    public static String buildSpeakingGenerationPrompt(String cefrLevel, String topic, String domain, String difficulty,
                                                       String title, String targetSounds, String length, int totalPoints,
                                                       String targetVocabulary) {
        return buildSpeakingGenerationPrompt(cefrLevel, topic, domain, difficulty, title, targetSounds, length, totalPoints,
            targetVocabulary, null, false);
    }

    /**
     * @brief Detailed prompt generator for speaking and pronunciation exercises with phonetics pack and ecoMode support.
     * @param cefrLevel Target CEFR level.
     * @param topic Topic or theme.
     * @param domain Real-world domain.
     * @param difficulty Difficulty tier.
     * @param title Title of assignment.
     * @param targetSounds Target sounds or phonemes.
     * @param length Length preset.
     * @param totalPoints Total points budget.
     * @param targetVocabulary Target vocabulary words.
     * @param pack Optional pronunciation pack.
     * @param ecoMode Eco mode flag.
     * @return Formatted prompt string.
     */
    public static String buildSpeakingGenerationPrompt(String cefrLevel, String topic, String domain, String difficulty,
                                                       String title, String targetSounds, String length, int totalPoints,
                                                       String targetVocabulary, PronunciationPackResponse pack,
                                                       boolean ecoMode) {
        String lengthGuidance = switch (length.toUpperCase()) {
            case "SHORT" -> "Short reading passage: 30 to 50 words (2-3 sentences). Ideal for quick drill and focused sound repetition.";
            case "LONG" -> "Long reading passage: 120 to 180 words (2-3 connected paragraphs). Ideal for advanced rhythm, stress, and continuous discourse.";
            case "CUSTOM" -> length;
            default -> "Medium reading passage: 60 to 90 words (1 rich, natural paragraph). Balanced flow and articulation.";
        };

        StringBuilder contextBuilder = new StringBuilder();

        if (ecoMode) {
            contextBuilder.append("\n- ECO MODE ACTIVE: Generate a concise, natural passage prioritizing conversational flow and cadence without heavy drill word constraints.\n");
        } else if (pack != null) {
            contextBuilder.append("\n- TARGET PHONETICS PACK: ").append(pack.getTitle()).append("\n");
            if (pack.getPhonemes() != null && !pack.getPhonemes().isEmpty()) {
                contextBuilder.append("- PHONEMES: ").append(String.join(", ", pack.getPhonemes())).append("\n");
            }
            if (pack.getDrillWords() != null && !pack.getDrillWords().isEmpty()) {
                contextBuilder.append("- TARGET DRILL WORDS (weave 4-6 into the passage): ")
                    .append(String.join(", ", pack.getDrillWords())).append("\n");
            }
            if (pack.getMinimalPairs() != null && !pack.getMinimalPairs().isEmpty()) {
                contextBuilder.append("- MINIMAL PAIR CONTRASTS (weave at least 1 contrast pair):\n");
                for (var pair : pack.getMinimalPairs()) {
                    contextBuilder.append("  * ").append(pair.getWord1()).append(" (").append(pair.getPhoneme1())
                        .append(") vs ").append(pair.getWord2()).append(" (").append(pair.getPhoneme2()).append(")\n");
                }
            }
            if (pack.getContrastFocus() != null && !pack.getContrastFocus().isBlank()) {
                contextBuilder.append("- ARTICULATION FOCUS: ").append(pack.getContrastFocus().trim()).append("\n");
            }
            if (pack.getTongueTwister() != null && !pack.getTongueTwister().isBlank()) {
                contextBuilder.append("- RHYTHMIC REFERENCE: ").append(pack.getTongueTwister().trim()).append("\n");
            }
        }

        if (targetVocabulary != null && !targetVocabulary.isBlank() && !ecoMode) {
            contextBuilder.append("\n- TARGET VOCABULARY WORDS TO INTEGRATE:\n").append(targetVocabulary.trim()).append("\n");
        }

        String soundReq = (!ecoMode && pack != null && pack.getDrillWords() != null && !pack.getDrillWords().isEmpty())
            ? "The passage must naturally incorporate at least 4-6 target drill words and at least one minimal pair contrast from the phonetics pack specified above."
            : "The passage must emphasize the target sounds and rhythm (" + targetSounds + ") while maintaining natural communicative flow.";

        return """
            Generate an English Speaking & Pronunciation reading practice assignment based on the following parameters:
            - CEFR Target Level: %s
            - Topic/Theme: %s
            - Assignment Title: %s
            - Situational Domain: %s
            - Difficulty Tier: %s
            - Target Sounds / Phonemes: %s
            - Passage Length: %s
            %s
            CRITICAL PEDAGOGICAL REQUIREMENTS:
            1. Create an authentic, natural, and engaging English passage for the student to read aloud into a microphone.
            2. %s
            3. Lexical and syntactic complexity MUST strictly calibrate to CEFR %s. Avoid vocabulary above this level.
            4. In the "questions" array, provide 1 item whose "text" and "correctAnswer" contain the passage text to be read aloud, with points allocated equal to %d.
            5. Return ONLY a valid JSON object matching this exact schema:
            {
              "title": "%s",
              "content": "Full text of the speech passage to read aloud...",
              "targetSounds": "%s",
              "questions": [
                {
                  "id": 1,
                  "questionOrder": 1,
                  "type": "SPEAKING",
                  "text": "Full text of the speech passage to read aloud...",
                  "correctAnswer": "Full text of the speech passage to read aloud...",
                  "grammarRule": "Target Pronunciation Focus: %s",
                  "difficulty": 2,
                  "points": %d,
                  "options": []
                }
              ],
              "answerKey": [
                {
                  "questionId": 1,
                  "questionOrder": 1,
                  "correctAnswer": "Full text of the speech passage to read aloud...",
                  "explanation": "Read clearly with proper sentence stress, intonation, and accurate pronunciation of %s."
                }
              ]
            }
            """.formatted(
                cefrLevel, topic, title, domain, difficulty, targetSounds, lengthGuidance,
                contextBuilder.toString(), soundReq, cefrLevel, totalPoints,
                title, targetSounds, targetSounds, totalPoints, targetSounds
            );
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
        return buildEssayScoringPrompt(cefrLevel, essayText, null, null, null);
    }

    /**
     * @brief Constructs an essay grading prompt returning IELTS/CEFR 4-pillar rubric in JSON,
     * verifying and penalizing missing or inadequate target grammar rules and target vocabulary.
     * @param cefrLevel Target benchmark level.
     * @param essayText Student's submitted essay.
     * @param topic Optional assigned essay topic / title.
     * @param targetRule Optional target grammar rules to be actively demonstrated in the essay.
     * @param targetVocabulary Optional target vocabulary words or collocations to incorporate.
     * @return Formatted evaluation prompt string.
     */
    public static String buildEssayScoringPrompt(String cefrLevel, String essayText, String topic, String targetRule, String targetVocabulary) {
        StringBuilder contextBuilder = new StringBuilder();
        if (topic != null && !topic.isBlank()) {
            contextBuilder.append("\n- ASSIGNED ESSAY TOPIC / PROMPT:\n")
                .append(topic.trim()).append("\n");
        }
        if (targetRule != null && !targetRule.isBlank()) {
            contextBuilder.append("\n- TARGET GRAMMAR RULES TO BE ACTIVELY DEMONSTRATED:\n")
                .append(targetRule.trim()).append("\n");
        }
        if (targetVocabulary != null && !targetVocabulary.isBlank()) {
            contextBuilder.append("\n- TARGET VOCABULARY WORDS & COLLOCATIONS TO INCORPORATE:\n")
                .append(targetVocabulary.trim()).append("\n");
        }

        String ruleContract = "";
        if ((targetRule != null && !targetRule.isBlank()) || (targetVocabulary != null && !targetVocabulary.isBlank())) {
            ruleContract = """

                TARGET RULES & VOCABULARY EVALUATION CONTRACT:
                1. The assignment specifically tests whether the student can actively incorporate the target rules and vocabulary listed above.
                2. Scrutinize the essay paragraphs for genuine, grammatically accurate usage of these target rules and terms.
                3. If the student failed to apply the target rules or vocabulary, or applied them only superficially / inaccurately:
                   - Penalize the "grammarRange" and "taskAchievement" scores (deduct 1.0 to 2.5 points based on the shortfall).
                   - Adjust "overallScore" downwards accordingly.
                   - In "feedback", explicitly report whether target rules and vocabulary were successfully demonstrated.
                   - Flag missing rules/terms in "weaknesses" and "recommendations".
                4. If the student demonstrated the target rules and vocabulary accurately and naturally, award credit in "grammarRange" and "taskAchievement" and acknowledge this in "strengths".
                """;
        }

        return """
            Evaluate the following English essay for a student at the %s level.%s%s
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
                {
                  "original": "exact erroneous word, phrase, or clause from the student essay to highlight in red",
                  "corrected": "grammatically accurate and natural English rewrite",
                  "explanation": "detailed pedagogical explanation of what was wrong, why, and how to fix it",
                  "grammarRule": "Grammar rule category (e.g. Subject-Verb Agreement, Past Simple vs Present Perfect, Article Use, Preposition)"
                }
              ],
              "weaknesses": ["Specific stylistic or grammatical weaknesses to improve"],
              "strengths": ["Strong points observed in the essay"],
              "recommendations": "Actionable pedagogical advice on what concepts to review and practice next",
              "suggestedTopics": ["Topic A", "Topic B"]
            }
            """.formatted(cefrLevel, contextBuilder.toString(), ruleContract, essayText);
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
            IMPORTANT FOR OPEN-ENDED QUESTIONS, SENTENCE COMPLETIONS, AND REWRITES:
            A non-exact match with the answer key does NOT mean the answer is wrong. If the student's answer is semantically accurate, aligns with the context or reading passage, and is grammatically acceptable, evaluate it as isCorrect: true, award points, and explain why it is valid.

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

    /**
     * @brief Constructs prompt for explaining an English sentence or phrase.
     * @param sentence Target sentence to explain.
     * @param context Optional surrounding context.
     * @param cefrLevel Student CEFR proficiency level string.
     * @return Formatted AI explanation prompt string.
     */
    public static String buildSentenceExplanationPrompt(String sentence, String context, String cefrLevel) {
        String level = (cefrLevel != null && !cefrLevel.isBlank()) ? cefrLevel : "B1";
        String ctxStr = (context != null && !context.isBlank()) ? "\nContext / Passage: " + context : "";
        String safeSentence = sentence != null ? sentence.replace("\"", "\\\"") : "";

        return """
            Explain the following English sentence for a language learner at CEFR level %s:
            Sentence: "%s"%s

            Provide:
            1. A natural Russian translation of the sentence in this context.
            2. Grammatical structure breakdown (tense, voice, clauses, syntax).
            3. Key vocabulary or idioms with concise definitions.
            4. Clear pedagogical explanation and practical usage tip.

            Return ONLY a valid JSON object with the following structure:
            {
              "originalSentence": "%s",
              "translation": "Естественный перевод предложения на русский язык",
              "grammarBreakdown": "Краткий разбор грамматических конструкций (время, залог, структуры)",
              "keyVocabulary": ["слово / идиома — значение и контекст"],
              "explanation": "Понятное методическое объяснение смысла и правил употребления"
            }
            """.formatted(level, safeSentence, ctxStr, safeSentence);
    }
}
