/**
 * @file phonetics.ts
 * @brief Phonetic utilities, IPA transcription dictionary, Text-to-Speech (TTS), and speech alignment.
 *
 * Provides browser-native speech synthesis, word-by-word pronunciation alignment using
 * Levenshtein distance, confidence scoring, and IPA phonetic guides.
 */

/**
 * @brief Pronunciation status of a word in a speaking exercise.
 */
export type WordPronunciationStatus = 'CORRECT' | 'MISPRONOUNCED' | 'OMITTED';

/**
 * @brief Alignment and diagnostic result for an individual word.
 */
export interface AlignedWord {
  word: string;
  cleanWord: string;
  ipa: string;
  status: WordPronunciationStatus;
  heard?: string;
  confidence?: number;
  tip?: string;
}

/**
 * @brief Comprehensive pronunciation evaluation result.
 */
export interface PronunciationResult {
  accuracyScore: number;
  fluencyScore: number;
  completenessScore: number;
  overallScore: number;
  correctCount: number;
  mispronouncedCount: number;
  omittedCount: number;
  totalWords: number;
  words: AlignedWord[];
  mispronouncedWords: AlignedWord[];
  generalFeedback: string[];
}

/**
 * @brief Common English words to IPA dictionary covering phonetically challenging and core vocabulary.
 */
const IPA_DICTIONARY: Record<string, string> = {
  // Dental fricatives (/θ/, /ð/)
  'the': '/ðiː, ðə/',
  'this': '/ðɪs/',
  'that': '/ðæt/',
  'these': '/ðiːz/',
  'those': '/ðoʊz/',
  'there': '/ðɛər/',
  'their': '/ðɛər/',
  'they': '/ðeɪ/',
  'them': '/ðɛm/',
  'then': '/ðɛn/',
  'than': '/ðæn/',
  'though': '/ðoʊ/',
  'although': '/ɔːlˈðoʊ/',
  'breathe': '/briːð/',
  'smooth': '/smuːð/',
  'clothe': '/kloʊð/',
  'father': '/ˈfɑːðər/',
  'mother': '/ˈmʌðər/',
  'brother': '/ˈbrʌðər/',
  'weather': '/ˈwɛðər/',
  'feather': '/ˈfɛðər/',
  'together': '/təˈɡɛðər/',
  'think': '/θɪŋk/',
  'thought': '/θɔːt/',
  'thoughtful': '/ˈθɔːtfəl/',
  'through': '/θruː/',
  'thorough': '/ˈθɜːroʊ/',
  'throughout': '/θruːˈaʊt/',
  'threat': '/θrɛt/',
  'three': '/θriː/',
  'third': '/θɜːrd/',
  'thirty': '/ˈθɜːrti/',
  'thousand': '/ˈθaʊzənd/',
  'thief': '/θiːf/',
  'thieves': '/θiːvz/',
  'thing': '/θɪŋ/',
  'theme': '/θiːm/',
  'theory': '/ˈθɪəri/',
  'therapy': '/ˈθɛrəpi/',
  'breath': '/brɛθ/',
  'death': '/dɛθ/',
  'earth': '/ɜːrθ/',
  'health': '/hɛlθ/',
  'wealth': '/wɛlθ/',
  'mouth': '/maʊθ/',
  'south': '/saʊθ/',
  'north': '/nɔːrθ/',
  'both': '/boʊθ/',
  'truth': '/truːθ/',
  'youth': '/juːθ/',
  'path': '/pæθ/',
  'math': '/mæθ/',
  'faith': '/feɪθ/',

  // Silent letters & non-phonetic spellings
  'comfortable': '/ˈkʌmftəbl/',
  'wednesday': '/ˈwɛnzdeɪ/',
  'colonel': '/ˈkɜːrnəl/',
  'island': '/ˈaɪlənd/',
  'aisle': '/aɪl/',
  'doubt': '/daʊt/',
  'debt': '/dɛt/',
  'subtle': '/ˈsʌtəl/',
  'receipt': '/rɪˈsiːt/',
  'salmon': '/ˈsæmən/',
  'almond': '/ˈɑːmənd/',
  'listen': '/ˈlɪsən/',
  'castle': '/ˈkæsəl/',
  'whistle': '/ˈwɪsəl/',
  'fasten': '/ˈfæsən/',
  'soften': '/ˈsɔːfən/',
  'knight': '/naɪt/',
  'knife': '/naɪf/',
  'knot': '/nɑːt/',
  'know': '/noʊ/',
  'knowledge': '/ˈnɑːlɪdʒ/',
  'knee': '/niː/',
  'wrap': '/ræp/',
  'write': '/raɪt/',
  'wrist': '/rɪst/',
  'wrong': '/rɔːŋ/',
  'sword': '/sɔːrd/',
  'answer': '/ˈænsər/',
  'chaos': '/ˈkeɪ.ɑːs/',
  'choir': '/ˈkwaɪər/',
  'stomach': '/ˈstʌmək/',
  'ache': '/eɪk/',
  'schedule': '/ˈskɛdʒuːl/',
  'queue': '/kjuː/',
  'iron': '/ˈaɪərn/',
  'vehicle': '/ˈviːɪkəl/',
  'clothes': '/kloʊðz/',

  // Minimal pairs (v / w, r / l, short / long vowels)
  'vine': '/vaɪn/',
  'wine': '/waɪn/',
  'very': '/ˈvɛri/',
  'wary': '/ˈwɛəri/',
  'vest': '/vɛst/',
  'west': '/wɛst/',
  'vet': '/vɛt/',
  'wet': '/wɛt/',
  'right': '/raɪt/',
  'light': '/laɪt/',
  'red': '/rɛd/',
  'led': '/lɛd/',
  'row': '/roʊ/',
  'low': '/loʊ/',
  'raw': '/rɔː/',
  'law': '/lɔː/',
  'ship': '/ʃɪp/',
  'sheep': '/ʃiːp/',
  'chip': '/tʃɪp/',
  'cheap': '/tʃiːp/',
  'fit': '/fɪt/',
  'feet': '/fiːt/',
  'hit': '/hɪt/',
  'heat': '/hiːt/',
  'sit': '/sɪt/',
  'seat': '/siːt/',
  'bit': '/bɪt/',
  'beat': '/biːt/',
  'pull': '/pʊl/',
  'pool': '/puːl/',
  'full': '/fʊl/',
  'fool': '/fuːl/',
  'cat': '/kæt/',
  'cut': '/kʌt/',
  'hat': '/hæt/',
  'hut': '/hʌt/',
  'bad': '/bæd/',
  'bed': '/bɛd/',

  // Common high-frequency words
  'hello': '/həˈloʊ/',
  'world': '/wɜːrld/',
  'english': '/ˈɪŋɡlɪʃ/',
  'language': '/ˈlæŋɡwɪdʒ/',
  'practice': '/ˈpræktɪs/',
  'student': '/ˈstjuːdənt/',
  'teacher': '/ˈtiːtʃər/',
  'education': '/ˌɛdʒʊˈkeɪʃən/',
  'learning': '/ˈlɜːrnɪŋ/',
  'reading': '/ˈriːdɪŋ/',
  'speaking': '/ˈspiːkɪŋ/',
  'pronounce': '/prəˈnaʊns/',
  'pronunciation': '/prəˌnʌnsiˈeɪʃən/',
  'accent': '/ˈæksɛnt/',
  'fluency': '/ˈfluːənsi/',
  'accuracy': '/ˈækjərəsi/',
  'rhythm': '/ˈrɪðəm/',
  'sentence': '/ˈsɛntəns/',
  'paragraph': '/ˈpærəɡræf/',
  'question': '/ˈkwɛstʃən/',
  'exercise': '/ˈɛksərsaɪz/',
  'success': '/səkˈsɛs/',
  'great': '/ɡreɪt/',
  'today': '/təˈdeɪ/',
  'tomorrow': '/təˈmɔːroʊ/',
  'yesterday': '/ˈjɛstərdeɪ/',
  'morning': '/ˈmɔːrnɪŋ/',
  'evening': '/ˈiːvnɪŋ/',
  'people': '/ˈpiːpəl/',
  'friend': '/frɛnd/',
  'water': '/ˈwɔːtər/',
  'future': '/ˈfjuːtʃər/',
  'science': '/ˈsaɪəns/',
  'nature': '/ˈneɪtʃər/',
  'environment': '/ɪnˈvaɪrənmənt/',
  'important': '/ɪmˈpɔːrtənt/',
  'beautiful': '/ˈbjuːtɪfəl/',
  'difficult': '/ˈdɪfɪkəlt/',
  'interesting': '/ˈɪntrəstɪŋ/',
  'opportunity': '/ˌɑːpərˈtuːnəti/',
  'international': '/ˌɪntərˈnæʃənəl/',
  'walk': '/wɔːk/',
  'quick': '/kwɪk/',
};

/**
 * @brief Retrieves International Phonetic Alphabet (IPA) representation of a word.
 * @param word Raw input word string.
 * @return IPA transcription string enclosed in slashes.
 */
export function getIpaTranscription(word: string): string {
  if (!word) return '';
  const clean = word.toLowerCase().replace(/[^a-z']/g, '').trim();
  if (!clean) return '';

  if (IPA_DICTIONARY[clean]) {
    return IPA_DICTIONARY[clean];
  }

  // Common suffix and prefix heuristics
  if (clean.endsWith('ing')) {
    const base = clean.slice(0, -3);
    if (IPA_DICTIONARY[base]) {
      return IPA_DICTIONARY[base].replace(/\/$/, 'ɪŋ/');
    }
    return `/${clean.slice(0, -3)}ɪŋ/`;
  }
  if (clean.endsWith('ly')) {
    const base = clean.slice(0, -2);
    if (IPA_DICTIONARY[base]) {
      return IPA_DICTIONARY[base].replace(/\/$/, 'li/');
    }
    return `/${clean.slice(0, -2)}li/`;
  }
  if (clean.endsWith('ed')) {
    const base = clean.slice(0, -2);
    if (IPA_DICTIONARY[base]) {
      return IPA_DICTIONARY[base].replace(/\/$/, 'd/');
    }
  }
  if (clean.endsWith('s')) {
    const base = clean.slice(0, -1);
    if (IPA_DICTIONARY[base]) {
      return IPA_DICTIONARY[base].replace(/\/$/, 'z/');
    }
  }

  // Heuristic approximation
  return `/${clean}/`;
}

/**
 * @brief Pronounces a specific word using the browser's speech synthesis engine.
 * @param word The word to pronounce.
 * @param accent Target accent ('en-US' or 'en-GB').
 * @param rate Speech rate (default 0.88 for clear pedagogical enunciation).
 */
export function speakWord(word: string, accent: 'en-US' | 'en-GB' = 'en-US', rate: number = 0.88): void {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;

  window.speechSynthesis.cancel();
  const clean = word.replace(/[^a-zA-Z'\s]/g, '').trim();
  if (!clean) return;

  const utterance = new SpeechSynthesisUtterance(clean);
  utterance.lang = accent;
  utterance.rate = rate;
  utterance.pitch = 1.0;

  // Prefer natural English voices if available
  const voices = window.speechSynthesis.getVoices();
  const voice = voices.find(v => v.lang.startsWith(accent.slice(0, 2)) && v.name.toLowerCase().includes('natural'))
    || voices.find(v => v.lang === accent)
    || voices.find(v => v.lang.startsWith('en'));

  if (voice) {
    utterance.voice = voice;
  }

  window.speechSynthesis.speak(utterance);
}

/**
 * @brief Pronounces a full text passage using speech synthesis.
 * @param text The full passage to read aloud.
 * @param accent Target accent ('en-US' or 'en-GB').
 * @param onEnd Optional callback when playback completes.
 */
export function speakText(text: string, accent: 'en-US' | 'en-GB' = 'en-US', onEnd?: () => void): void {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = accent;
  utterance.rate = 0.92;
  utterance.pitch = 1.0;

  if (onEnd) {
    utterance.onend = onEnd;
    utterance.onerror = onEnd;
  }

  const voices = window.speechSynthesis.getVoices();
  const voice = voices.find(v => v.lang === accent) || voices.find(v => v.lang.startsWith('en'));
  if (voice) {
    utterance.voice = voice;
  }

  window.speechSynthesis.speak(utterance);
}

/**
 * @brief Stops any active speech synthesis audio playback.
 */
export function stopSpeech(): void {
  if (typeof window !== 'undefined' && window.speechSynthesis) {
    window.speechSynthesis.cancel();
  }
}

/**
 * @brief Calculates Levenshtein edit distance between two strings.
 */
function levenshteinDistance(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  const dp: number[][] = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));

  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (a[i - 1] === b[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1];
      } else {
        dp[i][j] = 1 + Math.min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]);
      }
    }
  }

  return dp[m][n];
}

/**
 * @brief Generates pedagogical pronunciation advice based on target phonemes and mispronounced pairs.
 */
function generatePhoneticTip(expected: string, heard?: string): string {
  const expLower = expected.toLowerCase();
  const hrdLower = heard ? heard.toLowerCase() : '';

  if (expLower.includes('th')) {
    return 'Watch the "th" (/θ/ or /ð/) sound: place your tongue tip between your teeth, do not replace it with /s/, /f/, or /t/.';
  }
  if (expLower.startsWith('w') || expLower.startsWith('wh')) {
    return 'Round your lips firmly for /w/, avoid touching your upper teeth to your lower lip as in /v/.';
  }
  if (expLower.startsWith('v')) {
    return 'Rest your upper teeth gently on your bottom lip for /v/, make sure vocal cords vibrate.';
  }
  if (expLower.includes('r') && hrdLower.includes('l')) {
    return 'Distinguish /r/ and /l/: curl the tongue back without touching the roof of the mouth for /r/.';
  }
  if (expLower.includes('ee') || expLower.includes('ea')) {
    return 'Lengthen the vowel sound /iː/ (as in "sheep"), do not shorten it to /ɪ/ (as in "ship").';
  }
  if (hrdLower) {
    return `Sounded like "${heard}". Practice saying "${expected}" with crisp enunciation.`;
  }
  return `Word was omitted or indistinct. Practice saying "${expected}" clearly.`;
}

/**
 * @brief Aligns spoken recognized words against reference passage text.
 * @param referenceText Official reference passage the student was supposed to read.
 * @param recognizedText Spoken transcript captured by speech recognition.
 * @param rawConfidence Average acoustic confidence reported by recognition (0.0 to 1.0).
 * @return Detailed PronunciationResult breakdown.
 */
export function alignSpokenWords(
  referenceText: string,
  recognizedText: string,
  rawConfidence: number = 0.90
): PronunciationResult {
  const refTokens = referenceText
    .trim()
    .split(/\s+/)
    .filter(t => t.length > 0);

  const spokenTokens = recognizedText
    .trim()
    .toLowerCase()
    .replace(/[^\w\s']/g, '')
    .split(/\s+/)
    .filter(t => t.length > 0);

  const alignedWords: AlignedWord[] = [];
  let spokenIndex = 0;
  let correctCount = 0;
  let mispronouncedCount = 0;
  let omittedCount = 0;

  for (let i = 0; i < refTokens.length; i++) {
    const rawWord = refTokens[i];
    const cleanWord = rawWord.toLowerCase().replace(/[^\w']/g, '');
    const ipa = getIpaTranscription(cleanWord);

    if (spokenIndex < spokenTokens.length) {
      const spokenWord = spokenTokens[spokenIndex];

      if (cleanWord === spokenWord) {
        // Exact match
        alignedWords.push({
          word: rawWord,
          cleanWord,
          ipa,
          status: 'CORRECT',
          heard: spokenWord,
          confidence: Math.round(rawConfidence * 100),
        });
        correctCount++;
        spokenIndex++;
      } else {
        // Check fuzzy match within edit distance 1 (minor slip / accent)
        const distance = levenshteinDistance(cleanWord, spokenWord);
        if (distance <= 1 && cleanWord.length > 3) {
          alignedWords.push({
            word: rawWord,
            cleanWord,
            ipa,
            status: 'CORRECT',
            heard: spokenWord,
            confidence: Math.round(rawConfidence * 85),
          });
          correctCount++;
          spokenIndex++;
        } else {
          // Check if student skipped this word and spoke the next one
          const peekDistance = (spokenIndex + 1 < spokenTokens.length)
            ? levenshteinDistance(cleanWord, spokenTokens[spokenIndex + 1])
            : 99;

          if (peekDistance <= 1) {
            // Student added an extra word before this
            spokenIndex++;
            alignedWords.push({
              word: rawWord,
              cleanWord,
              ipa,
              status: 'CORRECT',
              heard: spokenTokens[spokenIndex],
              confidence: Math.round(rawConfidence * 80),
            });
            correctCount++;
            spokenIndex++;
          } else {
            // Mispronounced or substituted
            alignedWords.push({
              word: rawWord,
              cleanWord,
              ipa,
              status: 'MISPRONOUNCED',
              heard: spokenWord,
              confidence: Math.round(rawConfidence * 40),
              tip: generatePhoneticTip(cleanWord, spokenWord),
            });
            mispronouncedCount++;
            spokenIndex++;
          }
        }
      }
    } else {
      // Out of spoken words: omitted
      alignedWords.push({
        word: rawWord,
        cleanWord,
        ipa,
        status: 'OMITTED',
        tip: generatePhoneticTip(cleanWord),
      });
      omittedCount++;
    }
  }

  const totalWords = Math.max(1, refTokens.length);
  const accuracyScore = Math.min(100, Math.round((correctCount / totalWords) * 100));
  const completenessScore = Math.min(100, Math.round(((totalWords - omittedCount) / totalWords) * 100));
  const fluencyScore = Math.min(100, Math.round((accuracyScore * 0.7) + (completenessScore * 0.3)));
  const overallScore = Math.round((accuracyScore * 0.6) + (fluencyScore * 0.25) + (completenessScore * 0.15));

  const mispronouncedWords = alignedWords.filter(w => w.status === 'MISPRONOUNCED' || w.status === 'OMITTED');

  const generalFeedback: string[] = [];
  if (accuracyScore >= 90) {
    generalFeedback.push('Excellent clarity and precise articulation. Your speech rhythm and phoneme production are highly authentic.');
  } else if (accuracyScore >= 75) {
    generalFeedback.push('Good overall comprehension and speech flow. Focus on practicing the highlighted words with click-to-listen audio.');
  } else {
    generalFeedback.push('Pronunciation needs targeted practice. Take advantage of the audio buttons to hear native pronunciation for each misread word, then try reading again.');
  }

  if (omittedCount > 0) {
    generalFeedback.push(`${omittedCount} word(s) were missed or skipped. Try pacing your reading steadily.`);
  }

  return {
    accuracyScore,
    fluencyScore,
    completenessScore,
    overallScore,
    correctCount,
    mispronouncedCount,
    omittedCount,
    totalWords,
    words: alignedWords,
    mispronouncedWords,
    generalFeedback,
  };
}
