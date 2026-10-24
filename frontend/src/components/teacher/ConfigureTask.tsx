/**
 * @file ConfigureTask.tsx
 * @brief Educator task generation, parameter configuration, preview, custom curriculum context, and cohort deployment interface.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Send,
  Bookmark,
  Eye,
  CheckCircle2,
  Cpu,
  Trash2,
  Info,
  Plus,
  RefreshCw,
  Check,
  AlertTriangle,
  Key,
  ArrowRightLeft,
  Smartphone,
  Zap,
  SlidersHorizontal,
  Volume2,
  BookOpen,
  FileText,
  Mic,
  PenTool,
  Target,
  Shuffle,
  ArrowRight,
  GripVertical,
} from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { taskApi } from '../../api/taskApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
import { progressApi } from '../../api/progressApi';
import { Group } from '../../types/group';
import {
  Task,
  TaskType,
  DifficultyLevel,
  TopicsCatalogResponse,
  TaskQuestion,
  CreateCustomTaskRequest,
} from '../../types/task';
import { CefrLevel } from '../../types/user';
import { CefrBadge } from '../common/CefrBadge';
import { useNotificationStore } from '../../store/notificationStore';
import {
  AIProviderType,
  getDefaultModelForProvider,
} from '../../constants/aiModels';
import { useUIStore } from '../../store/uiStore';
import { customCurriculumApi } from '../../api/customCurriculumApi';
import { CustomCurriculumEntry } from '../../types/curriculum';
import {
  DEFAULT_CEFR_TOPICS,
  getRandomThematicTopic,
  findThematicTopicByTitle,
} from '../../constants/topics';
import { ReadingConfigPanel, ReadingConfig } from './ReadingConfigPanel';
import { SpeakingConfigPanel, TARGET_SOUND_PRESETS } from './SpeakingConfigPanel';
import { GrammarConfigPanel, UploadedCurriculumFile } from './GrammarConfigPanel';
import { EssayConfigPanel } from './EssayConfigPanel';
import { QuickModePanel, QuickGenerateParams } from '../common/QuickModePanel';
import { AutoExpandingTextarea } from '../common/AutoExpandingTextarea';
import { NumberStepper } from '../common/NumberStepper';
import { speakText } from '../../utils/phonetics';

/**
 * @brief Teacher component for configuring and deploying AI-generated assignments to student groups.
 * @return React component element.
 */
export const ConfigureTask: React.FC = () => {
  const navigate = useNavigate();
  const { addToast } = useNotificationStore();
  const { openUpgradeWall } = useUIStore();

  const [generationMode, setGenerationMode] = useState<'QUICK' | 'DETAILED'>('DETAILED');
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([]);
  const [catalog, setCatalog] = useState<TopicsCatalogResponse | null>(null);
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>('B1');
  const [grammarTopic, setGrammarTopic] = useState<string>('Passive Voice (Basic)');
  const [domain, setDomain] = useState<string>('Academic');
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
  const [numberOfQuestions, setNumberOfQuestions] = useState<number>(5);
  const [totalPoints, setTotalPoints] = useState<number>(100);
  const [dueDate, setDueDate] = useState<string>('');
  const [maxAttempts, setMaxAttempts] = useState<number>(1);
  const [provider, setProvider] = useState<AIProviderType>('GEMINI');
  const [modelName, setModelName] = useState<string>('gemini-flash-latest');
  const [aiEngineMode, setAiEngineMode] = useState<'PLATFORM' | 'BYOK'>('PLATFORM');
  const [savedKeys, setSavedKeys] = useState<ApiKeyItem[]>([]);
  const [ecoMode, setEcoMode] = useState<boolean>(false);

  // Dedicated Reading task configuration state
  const [readingConfig, setReadingConfig] = useState<ReadingConfig>({
    readingTitle: '',
    readingLength: 'Medium (250-400 words)',
    readingParagraphs: '3 to 4 distinct paragraphs with clear narrative progression',
    mcqQuestionsCount: 3,
    mcqPoints: 10,
    trueFalseQuestionsCount: 2,
    trueFalsePoints: 10,
    openQuestionsCount: 2,
    openPoints: 15,
  });

  // Dedicated Speaking task configuration state
  const [speakingTitle, setSpeakingTitle] = useState<string>('');
  const [targetSounds, setTargetSounds] = useState<string>(TARGET_SOUND_PRESETS[0].value);
  const [pronunciationPackId, setPronunciationPackId] = useState<string | null>(null);
  const [speakingLength, setSpeakingLength] = useState<string>('MEDIUM');
  const [customWordCount, setCustomWordCount] = useState<number | undefined>(undefined);
  const [customInstructions, setCustomInstructions] = useState<string>('');

  // Curriculum context state
  const [customRule, setCustomRule] = useState<string>('');
  const [customVocabulary, setCustomVocabulary] = useState<string>('');
  const [uploadedRuleFile, setUploadedRuleFile] = useState<UploadedCurriculumFile | null>(null);
  const [uploadedVocabFile, setUploadedVocabFile] = useState<UploadedCurriculumFile | null>(null);

  const [previewTask, setPreviewTask] = useState<Task | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [concurrentConflict, setConcurrentConflict] = useState<boolean>(false);
  const [isTakingOver, setIsTakingOver] = useState<boolean>(false);
  const [pendingAction, setPendingAction] = useState<'preview' | 'template' | 'deploy' | null>(null);

  // Saved curriculum sets from library
  const [savedRules, setSavedRules] = useState<CustomCurriculumEntry[]>([]);
  const [savedVocabSets, setSavedVocabSets] = useState<CustomCurriculumEntry[]>([]);
  const [isSavingRule, setIsSavingRule] = useState<boolean>(false);
  const [isSavingVocab, setIsSavingVocab] = useState<boolean>(false);

  // Resizable layout split state (percentage for left generator column on desktop)
  const [splitRatio, setSplitRatio] = useState<number>(55);
  const [isDraggingSplitter, setIsDraggingSplitter] = useState<boolean>(false);
  const [isDesktop, setIsDesktop] = useState<boolean>(
    () => typeof window !== 'undefined' && window.innerWidth >= 1024
  );
  const layoutContainerRef = React.useRef<HTMLDivElement>(null);

  // Drag-and-drop question card reordering state
  const [draggedQIndex, setDraggedQIndex] = useState<number | null>(null);

  useEffect(() => {
    const handleResize = () => {
      setIsDesktop(window.innerWidth >= 1024);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    if (!isDraggingSplitter) return;
    const handleMouseMove = (e: MouseEvent) => {
      if (!layoutContainerRef.current) return;
      const rect = layoutContainerRef.current.getBoundingClientRect();
      const raw = ((e.clientX - rect.left) / rect.width) * 100;
      setSplitRatio(Math.min(75, Math.max(25, Math.round(raw))));
    };
    const handleMouseUp = () => {
      setIsDraggingSplitter(false);
    };
    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDraggingSplitter]);

  const handleDragStart = (e: React.DragEvent, index: number) => {
    setDraggedQIndex(index);
    e.dataTransfer.effectAllowed = 'move';
    const emptyImg = new Image();
    emptyImg.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
    e.dataTransfer.setDragImage(emptyImg, 0, 0);
  };

  const handleDragOver = (e: React.DragEvent, targetIndex: number) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    if (draggedQIndex === null || draggedQIndex === targetIndex) return;

    setPreviewTask((prev) => {
      if (!prev || !prev.questions) return prev;
      const list = [...prev.questions];
      const [moved] = list.splice(draggedQIndex, 1);
      list.splice(targetIndex, 0, moved);
      const reindexed = list.map((q, idx) => ({ ...q, questionOrder: idx + 1 }));
      return { ...prev, questions: reindexed };
    });
    setDraggedQIndex(targetIndex);
  };

  const handleDragEnd = () => {
    setDraggedQIndex(null);
  };

  const [searchParams] = useSearchParams();
  const urlGroupId = searchParams.get('groupId');

  useEffect(() => {
    groupApi
      .getGroups()
      .then((data) => {
        setGroups(data);
        if (urlGroupId && data.some((g) => g.id === urlGroupId)) {
          setSelectedGroupIds([urlGroupId]);
        } else if (data.length > 0) {
          setSelectedGroupIds([data[0].id]);
        }
      })
      .catch((err) => console.error('Failed to load groups:', err));

    taskApi
      .getTopicsCatalog()
      .then((data) => setCatalog(data))
      .catch((err) => console.warn('Could not load topics catalog:', err));

    apiKeyApi
      .getKeys()
      .then((keys) => {
        setSavedKeys(keys);
      })
      .catch(() => {});

    customCurriculumApi.getCustomCurriculum('RULE')
      .then(setSavedRules)
      .catch(() => {});

    customCurriculumApi.getCustomCurriculum('VOCABULARY')
      .then(setSavedVocabSets)
      .catch(() => {});
  }, []);

  const handleSaveRuleToLibrary = async () => {
    if (!customRule.trim()) return;
    setIsSavingRule(true);
    try {
      const title = grammarTopic ? `${grammarTopic} Rule` : 'Custom Grammar Rule';
      const saved = await customCurriculumApi.createCustomCurriculum({
        title,
        cefrLevel,
        curriculumType: 'RULE',
        content: customRule.trim(),
      });
      setSavedRules((prev) => [saved, ...prev]);
      addToast({
        type: 'success',
        title: 'Rule Saved to Library',
        message: `Custom rule "${saved.title}" saved.`,
      });
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to save rule.';
      addToast({ type: 'error', title: 'Save Failed', message: msg });
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(msg);
      }
    } finally {
      setIsSavingRule(false);
    }
  };

  const handleSaveVocabToLibrary = async () => {
    if (!customVocabulary.trim()) return;
    setIsSavingVocab(true);
    try {
      const title = grammarTopic ? `${grammarTopic} Vocabulary` : 'Custom Vocabulary';
      const saved = await customCurriculumApi.createCustomCurriculum({
        title,
        cefrLevel,
        curriculumType: 'VOCABULARY',
        content: customVocabulary.trim(),
      });
      setSavedVocabSets((prev) => [saved, ...prev]);
      addToast({
        type: 'success',
        title: 'Vocabulary Saved to Library',
        message: `Custom vocabulary "${saved.title}" saved.`,
      });
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to save vocabulary.';
      addToast({ type: 'error', title: 'Save Failed', message: msg });
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(msg);
      }
    } finally {
      setIsSavingVocab(false);
    }
  };


  const toggleGroup = (id: string) => {
    setSelectedGroupIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
    );
  };

  const isGrammarTask = ['MCQ', 'GAP_FILL', 'OPEN_BRACKETS', 'REWRITE'].includes(taskType);

  /**
   * @brief Helper returning current task parameters including curriculum context.
   */
  const buildTaskParams = () => {
    const readingTotalPoints =
      (readingConfig.mcqQuestionsCount || 0) * (readingConfig.mcqPoints || 0) +
      (readingConfig.trueFalseQuestionsCount || 0) * (readingConfig.trueFalsePoints || 0) +
      (readingConfig.openQuestionsCount || 0) * (readingConfig.openPoints || 0);

    const readingQuestionsCount =
      (readingConfig.mcqQuestionsCount || 0) +
      (readingConfig.trueFalseQuestionsCount || 0) +
      (readingConfig.openQuestionsCount || 0);

    return {
      cefrLevel,
      grammarTopic: grammarTopic.trim() || 'General Practice',
      domain,
      taskType,
      difficulty,
      numberOfQuestions: taskType === 'READING' ? readingQuestionsCount : numberOfQuestions,
      totalPoints: taskType === 'READING' ? readingTotalPoints : totalPoints,
      provider,
      modelName,
      customRule: customRule.trim() || undefined,
      customVocabulary: customVocabulary.trim() || undefined,
      ruleFilePath: uploadedRuleFile?.serverPath || undefined,
      vocabularyFilePath: uploadedVocabFile?.serverPath || undefined,
      ecoMode,
      groupIds: selectedGroupIds.length > 0 ? selectedGroupIds : undefined,
      dueDate: dueDate || undefined,
      maxAttempts,
      ...(taskType === 'READING'
        ? {
            readingTitle: readingConfig.readingTitle.trim() || undefined,
            readingLength: readingConfig.readingLength.trim() || undefined,
            readingParagraphs: readingConfig.readingParagraphs.trim() || undefined,
            mcqQuestionsCount: readingConfig.mcqQuestionsCount,
            mcqPoints: readingConfig.mcqPoints,
            trueFalseQuestionsCount: readingConfig.trueFalseQuestionsCount,
            trueFalsePoints: readingConfig.trueFalsePoints,
            openQuestionsCount: readingConfig.openQuestionsCount,
            openPoints: readingConfig.openPoints,
          }
        : {}),
      ...(taskType === 'SPEAKING'
        ? {
            speakingTitle: speakingTitle.trim() || undefined,
            targetSounds: targetSounds.trim() || undefined,
            speakingLength: speakingLength.trim() || undefined,
            pronunciationPackId: pronunciationPackId || undefined,
            numberOfQuestions: 1,
            totalPoints: totalPoints || 100,
          }
        : {}),
    };
  };

  /**
   * @brief Fast generation triggered from QuickModePanel in educator view.
   */
  const handleQuickGenerate = async (params: QuickGenerateParams) => {
    setIsLoading(true);
    setStatusMessage(null);
    try {
      setTaskType(params.taskType);
      setGrammarTopic(params.grammarTopic);
      setDomain(params.domain);
      setDifficulty(params.difficulty);
      setNumberOfQuestions(params.numberOfQuestions);
      setEcoMode(params.ecoMode);
      if (params.readingConfig) {
        setReadingConfig((prev) => ({ ...prev, ...params.readingConfig }));
      }
      if (params.speakingConfig) {
        setSpeakingTitle(params.speakingConfig.speakingTitle);
        setTargetSounds(params.speakingConfig.targetSounds);
        setSpeakingLength(params.speakingConfig.speakingLength);
        if (params.speakingConfig.pronunciationPackId) {
          setPronunciationPackId(params.speakingConfig.pronunciationPackId);
        }
      }

      const readingTotalPoints = params.readingConfig
        ? (params.readingConfig.mcqQuestionsCount || 0) * (params.readingConfig.mcqPoints || 0) +
          (params.readingConfig.trueFalseQuestionsCount || 0) * (params.readingConfig.trueFalsePoints || 0) +
          (params.readingConfig.openQuestionsCount || 0) * (params.readingConfig.openPoints || 0)
        : undefined;

      const readingQuestionsCount = params.readingConfig
        ? (params.readingConfig.mcqQuestionsCount || 0) +
          (params.readingConfig.trueFalseQuestionsCount || 0) +
          (params.readingConfig.openQuestionsCount || 0)
        : undefined;

      const task = await taskApi.previewTask({
        cefrLevel,
        grammarTopic: params.grammarTopic,
        domain: params.domain,
        taskType: params.taskType,
        difficulty: params.difficulty,
        numberOfQuestions: params.taskType === 'READING' ? readingQuestionsCount : params.numberOfQuestions,
        totalPoints: readingTotalPoints || totalPoints || 100,
        provider,
        modelName,
        ecoMode: params.ecoMode,
        groupIds: selectedGroupIds.length > 0 ? selectedGroupIds : undefined,
        dueDate: dueDate || undefined,
        maxAttempts,
        ...(params.readingConfig
          ? {
              readingTitle: params.readingConfig.readingTitle,
              readingLength: params.readingConfig.readingLength,
              readingParagraphs: params.readingConfig.readingParagraphs,
              mcqQuestionsCount: params.readingConfig.mcqQuestionsCount,
              mcqPoints: params.readingConfig.mcqPoints,
              trueFalseQuestionsCount: params.readingConfig.trueFalseQuestionsCount,
              trueFalsePoints: params.readingConfig.trueFalsePoints,
              openQuestionsCount: params.readingConfig.openQuestionsCount,
              openPoints: params.readingConfig.openPoints,
            }
          : {}),
        ...(params.speakingConfig
          ? {
              speakingTitle: params.speakingConfig.speakingTitle,
              targetSounds: params.speakingConfig.targetSounds,
              speakingLength: params.speakingConfig.speakingLength,
              pronunciationPackId: params.speakingConfig.pronunciationPackId,
              numberOfQuestions: 1,
              totalPoints: totalPoints || 100,
            }
          : {}),
      });

      const effTotal = task.totalPoints || totalPoints || 100;
      const qLen = task.questions && task.questions.length > 0 ? task.questions.length : 1;
      const basePoints = Math.floor(effTotal / qLen);
      const remPoints = effTotal % qLen;
      const enrichedQuestions = (task.questions || []).map((q, idx) => ({
        ...q,
        points: q.points && q.points > 0 ? q.points : Math.max(1, basePoints + (idx === 0 ? remPoints : 0)),
      }));

      setPreviewTask({
        ...task,
        totalPoints: effTotal,
        questions: enrichedQuestions,
      });

      addToast({
        type: 'success',
        title: 'Quick Task Generated',
        message: 'Task preview generated! Review the questions or deploy directly to cohorts.',
      });
    } catch (err: any) {
      if (err.response?.status === 409 || err.response?.data?.errorCode === 'CONCURRENT_GENERATION') {
        setPendingAction('preview');
        setConcurrentConflict(true);
      } else {
        const errMsg = err.response?.data?.message || 'Failed to preview quick task.';
        addToast({ type: 'error', title: 'Generation Failed', message: errMsg });
        setStatusMessage(errMsg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Fills the weakest student gap or foundational topic and thematic vocabulary into the active fields.
   */
  const handleFillRecommended = async () => {
    try {
      const gaps = await progressApi.getGaps();
      if (gaps && gaps.length > 0) {
        const weakest = [...gaps].sort((a, b) => a.masteryScore - b.masteryScore)[0];
        setGrammarTopic(weakest.grammarTopic);
        const thematic = findThematicTopicByTitle(weakest.grammarTopic) || getRandomThematicTopic(cefrLevel, domain);
        if (thematic) {
          if (thematic.vocabulary && thematic.vocabulary.length > 0) {
            setCustomVocabulary(thematic.vocabulary.join(', '));
          }
          if (thematic.grammarFocus) {
            setCustomRule(thematic.grammarFocus);
          }
          if (taskType === 'READING') {
            setReadingConfig((prev) => ({ ...prev, readingTitle: thematic.title }));
          } else if (taskType === 'SPEAKING') {
            setSpeakingTitle(thematic.title);
          }
        }
        addToast({
          type: 'info',
          title: 'Topic & Context Auto-Filled',
          message: `Recommended gap: ${weakest.grammarTopic} (${Math.round(weakest.masteryScore)}% mastery). Context populated.`,
        });
        return;
      }
    } catch (_) {}
    const thematic = getRandomThematicTopic(cefrLevel, domain);
    if (thematic) {
      if (taskType === 'ESSAY') {
        setGrammarTopic(thematic.prompt || thematic.title);
      } else {
        setGrammarTopic(thematic.grammarFocus || thematic.title);
      }
      if (thematic.vocabulary && thematic.vocabulary.length > 0) {
        setCustomVocabulary(thematic.vocabulary.join(', '));
      }
      if (thematic.grammarFocus) {
        setCustomRule(thematic.grammarFocus);
      }
      if (taskType === 'READING') {
        setReadingConfig((prev) => ({ ...prev, readingTitle: thematic.title }));
      } else if (taskType === 'SPEAKING') {
        setSpeakingTitle(thematic.title);
      }
      addToast({
        type: 'info',
        title: 'Topic & Context Auto-Filled',
        message: `Syllabus foundation: ${thematic.title}. Title, grammar and vocabulary filled.`,
      });
    }
  };

  /**
   * @brief Fills a random topic and thematic vocabulary matching format and CEFR level.
   */
  const handleFillRandom = () => {
    const thematic = getRandomThematicTopic(cefrLevel, domain);
    if (thematic) {
      if (taskType === 'ESSAY') {
        setGrammarTopic(thematic.prompt || thematic.title);
      } else {
        setGrammarTopic(thematic.grammarFocus || thematic.title);
      }
      if (thematic.vocabulary && thematic.vocabulary.length > 0) {
        setCustomVocabulary(thematic.vocabulary.join(', '));
      }
      if (thematic.grammarFocus) {
        setCustomRule(thematic.grammarFocus);
      }
      if (taskType === 'READING') {
        setReadingConfig((prev) => ({ ...prev, readingTitle: thematic.title }));
      } else if (taskType === 'SPEAKING') {
        setSpeakingTitle(thematic.title);
      }
      addToast({
        type: 'info',
        title: 'Random Topic Rolled',
        message: `Rolled: ${thematic.title}. Title, grammar and vocabulary filled.`,
      });
    }
  };

  const handlePreview = async () => {
    setIsLoading(true);
    setStatusMessage(null);
    try {
      const task = await taskApi.previewTask(buildTaskParams());
      const effTotal = task.totalPoints || totalPoints || 100;
      const qLen = task.questions && task.questions.length > 0 ? task.questions.length : 1;
      const basePoints = Math.floor(effTotal / qLen);
      const remPoints = effTotal % qLen;
      const enrichedQuestions = (task.questions || []).map((q, idx) => ({
        ...q,
        points: q.points && q.points > 0 ? q.points : Math.max(1, basePoints + (idx === 0 ? remPoints : 0)),
      }));

      setPreviewTask({
        ...task,
        totalPoints: effTotal,
        questions: enrichedQuestions,
      });
    } catch (err: any) {
      if (err.response?.status === 409 || err.response?.data?.errorCode === 'CONCURRENT_GENERATION') {
        setPendingAction('preview');
        setConcurrentConflict(true);
      } else {
        setStatusMessage(err.response?.data?.message || 'Failed to preview task.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Updates total points for the preview task and maintains form state.
   */
  const handleUpdateTotalPoints = (newTotal: number) => {
    const val = Math.max(1, newTotal);
    setTotalPoints(val);
    if (previewTask) {
      setPreviewTask({ ...previewTask, totalPoints: val });
    }
  };

  /**
   * @brief Updates points / price for a specific question in the preview task.
   */
  const handleUpdateQuestionPoints = (qIndex: number, newPoints: number) => {
    if (!previewTask || !previewTask.questions) return;
    const val = Math.max(1, newPoints);
    handleUpdateQuestion(qIndex, { points: val });
  };

  /**
   * @brief Evenly distributes the total points across all questions in the preview.
   */
  const handleDistributePointsEvenly = () => {
    if (!previewTask || !previewTask.questions || previewTask.questions.length === 0) return;
    const effTotal = previewTask.totalPoints || totalPoints || 100;
    const count = previewTask.questions.length;
    const base = Math.floor(effTotal / count);
    const rem = effTotal % count;
    const updated = previewTask.questions.map((q, idx) => ({
      ...q,
      points: Math.max(1, base + (idx === 0 ? rem : 0)),
    }));
    setPreviewTask({ ...previewTask, questions: updated });
    addToast({
      type: 'info',
      title: 'Points Distributed',
      message: `Distributed ${effTotal} total points across ${count} questions.`,
    });
  };

  /**
   * @brief Automatically sums question points and sets total points to that sum.
   */
  const handleSyncTotalFromQuestions = () => {
    if (!previewTask || !previewTask.questions) return;
    const sum = previewTask.questions.reduce((acc, q) => acc + (q.points || 0), 0);
    const val = Math.max(1, sum);
    setTotalPoints(val);
    setPreviewTask({ ...previewTask, totalPoints: val });
    addToast({
      type: 'info',
      title: 'Total Points Updated',
      message: `Total points updated to match sum of questions (${val} pts).`,
    });
  };

  /**
   * @brief Updates the instruction text of the previewed task.
   */
  const handleUpdateInstruction = (content: string) => {
    if (!previewTask) return;
    setPreviewTask({ ...previewTask, content });
  };

  /**
   * @brief Updates fields of a specific question in the previewed task.
   */
  const handleUpdateQuestion = (index: number, updatedFields: Partial<TaskQuestion>) => {
    if (!previewTask || !previewTask.questions) return;
    const updated = [...previewTask.questions];
    updated[index] = { ...updated[index], ...updatedFields };
    setPreviewTask({ ...previewTask, questions: updated });
  };

  /**
   * @brief Updates option choice text at a given index and keeps correct answer synchronized.
   */
  const handleOptionChange = (qIndex: number, optIndex: number, newOptionText: string) => {
    if (!previewTask || !previewTask.questions) return;
    const q = previewTask.questions[qIndex];
    const prevOptions = q.options || [];
    const oldOptionText = prevOptions[optIndex];
    const updatedOptions = [...prevOptions];
    updatedOptions[optIndex] = newOptionText;

    const updatedCorrectAnswer = q.correctAnswer === oldOptionText ? newOptionText : q.correctAnswer;
    handleUpdateQuestion(qIndex, {
      options: updatedOptions,
      correctAnswer: updatedCorrectAnswer,
    });
  };

  /**
   * @brief Designates which option or string is the correct answer for a question.
   */
  const handleSetCorrectAnswer = (qIndex: number, answerText: string) => {
    handleUpdateQuestion(qIndex, { correctAnswer: answerText });
  };

  /**
   * @brief Adds a new choice option to an MCQ question.
   */
  const handleAddOption = (qIndex: number) => {
    if (!previewTask || !previewTask.questions) return;
    const q = previewTask.questions[qIndex];
    const currentOptions = q.options || [];
    const newOpt = `Option ${String.fromCharCode(65 + currentOptions.length)}`;
    handleUpdateQuestion(qIndex, {
      options: [...currentOptions, newOpt],
    });
  };

  /**
   * @brief Removes an option from an MCQ question.
   */
  const handleRemoveOption = (qIndex: number, optIndex: number) => {
    if (!previewTask || !previewTask.questions) return;
    const q = previewTask.questions[qIndex];
    const currentOptions = q.options || [];
    if (currentOptions.length <= 2) return;
    const removedText = currentOptions[optIndex];
    const updatedOptions = currentOptions.filter((_, idx) => idx !== optIndex);
    const updatedCorrectAnswer =
      q.correctAnswer === removedText ? updatedOptions[0] || '' : q.correctAnswer;
    handleUpdateQuestion(qIndex, {
      options: updatedOptions,
      correctAnswer: updatedCorrectAnswer,
    });
  };

  /**
   * @brief Adds a new blank question to the task preview.
   */
  const handleAddQuestion = () => {
    if (!previewTask) return;
    const currentQuestions = previewTask.questions || [];
    const effTotal = previewTask.totalPoints || totalPoints || 100;
    const defaultQPoints = Math.max(1, Math.floor(effTotal / (currentQuestions.length + 1)));
    const newQ: TaskQuestion = {
      id: `custom-${Date.now()}`,
      questionOrder: currentQuestions.length + 1,
      questionText: '',
      correctAnswer: previewTask.type === 'MCQ' ? 'Option A' : '',
      options: previewTask.type === 'MCQ' ? ['Option A', 'Option B', 'Option C', 'Option D'] : [],
      difficulty: 0.5,
      points: defaultQPoints,
      grammarRule: previewTask.grammarTopic || '',
    };
    setPreviewTask({
      ...previewTask,
      questions: [...currentQuestions, newQ],
    });
  };

  /**
   * @brief Deletes a question from the preview and re-indexes remaining questions.
   */
  const handleDeleteQuestion = (qIndex: number) => {
    if (!previewTask || !previewTask.questions) return;
    const updatedQuestions = previewTask.questions
      .filter((_, idx) => idx !== qIndex)
      .map((q, idx) => ({ ...q, questionOrder: idx + 1 }));
    setPreviewTask({
      ...previewTask,
      questions: updatedQuestions,
    });
  };

  const handleSaveTemplate = async () => {
    setIsLoading(true);
    setStatusMessage(null);
    try {
      if (previewTask && previewTask.questions && previewTask.questions.length > 0) {
        const customReq: CreateCustomTaskRequest = {
          cefrLevel: previewTask.cefrLevel,
          grammarTopic: previewTask.grammarTopic,
          domain: previewTask.domain,
          taskType: previewTask.type,
          difficulty: previewTask.difficulty,
          totalPoints: previewTask.totalPoints || totalPoints,
          content: previewTask.content,
          questions: previewTask.questions.map((q, idx) => ({
            questionOrder: idx + 1,
            questionText: q.questionText,
            correctAnswer: q.correctAnswer,
            options: q.options || [],
            difficulty: q.difficulty,
            points: q.points || 10,
            grammarRule: q.grammarRule,
          })),
          isTemplate: true,
        };
        await taskApi.createCustomTask(customReq);
      } else {
        await taskApi.saveTemplate(buildTaskParams());
      }
      addToast({
        type: 'success',
        title: 'Template Saved',
        message: 'Exercise saved as a reusable curriculum template!',
      });
      setStatusMessage('Template saved to your curriculum catalog!');
    } catch (err: any) {
      if (err.response?.status === 409 || err.response?.data?.errorCode === 'CONCURRENT_GENERATION') {
        setPendingAction('template');
        setConcurrentConflict(true);
      } else {
        const errMsg = err.response?.data?.message || 'Failed to save template.';
        addToast({
          type: 'error',
          title: 'Save Template Failed',
          message: errMsg,
        });
        setStatusMessage(errMsg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeploy = async () => {
    if (selectedGroupIds.length === 0) {
      addToast({
        type: 'warning',
        title: 'Cohort Required',
        message: 'Please select at least one cohort group before deploying this assignment.',
      });
      setStatusMessage('Please select at least one cohort group to deploy this assignment.');
      return;
    }

    setIsLoading(true);
    setStatusMessage(null);

    try {
      if (previewTask && previewTask.questions && previewTask.questions.length > 0) {
        const customReq: CreateCustomTaskRequest = {
          cefrLevel: previewTask.cefrLevel,
          grammarTopic: previewTask.grammarTopic,
          domain: previewTask.domain,
          taskType: previewTask.type,
          difficulty: previewTask.difficulty,
          totalPoints: previewTask.totalPoints || totalPoints,
          content: previewTask.content,
          questions: previewTask.questions.map((q, idx) => ({
            questionOrder: idx + 1,
            questionText: q.questionText,
            correctAnswer: q.correctAnswer,
            options: q.options || [],
            difficulty: q.difficulty,
            points: q.points || 10,
            grammarRule: q.grammarRule,
          })),
          groupIds: selectedGroupIds,
          dueDate: dueDate || undefined,
          maxAttempts,
        };
        await taskApi.createCustomTask(customReq);
      } else {
        await taskApi.generateTask(buildTaskParams());
      }

      addToast({
        type: 'success',
        title: 'Assignment Deployed',
        message: 'Assignment successfully deployed to selected student cohorts!',
      });
      setStatusMessage('Assignment deployed successfully to selected cohort groups!');
      setTimeout(() => navigate('/teacher/dashboard'), 1500);
    } catch (err: any) {
      if (err.response?.status === 409 || err.response?.data?.errorCode === 'CONCURRENT_GENERATION') {
        setPendingAction('deploy');
        setConcurrentConflict(true);
      } else {
        const errMsg = err.response?.data?.message || 'Failed to deploy assignment.';
        addToast({
          type: 'error',
          title: 'Deployment Failed',
          message: errMsg,
        });
        setStatusMessage(errMsg);
      }
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Forcefully transfers AI generation control to current device and retries pending action.
   */
  const handleTakeoverAndProceed = async () => {
    setIsTakingOver(true);
    try {
      await taskApi.takeoverGeneration();
      setConcurrentConflict(false);
      addToast({
        type: 'success',
        title: 'Session Switched',
        message: 'Generation control was successfully transferred to this device.',
      });
      if (pendingAction === 'preview') {
        handlePreview();
      } else if (pendingAction === 'deploy') {
        handleDeploy();
      } else if (pendingAction === 'template') {
        handleSaveTemplate();
      }
    } catch (e: any) {
      addToast({
        type: 'error',
        title: 'Transfer Failed',
        message: e.response?.data?.message || 'Could not transfer generation control.',
      });
    } finally {
      setIsTakingOver(false);
    }
  };

  // Keyboard shortcuts: Ctrl+Enter (Deploy), Ctrl+P (Preview) & Ctrl+S (Save Template)
  useEffect(() => {
    const handleConfigureKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        if (previewTask && previewTask.questions && previewTask.questions.length > 0) {
          handleDeploy();
        } else {
          handlePreview();
        }
      } else if ((e.ctrlKey || e.metaKey) && (e.key === 'p' || e.key === 'з')) {
        e.preventDefault();
        handlePreview();
      } else if ((e.ctrlKey || e.metaKey) && (e.key === 's' || e.key === 'ы')) {
        e.preventDefault();
        handleSaveTemplate();
      }
    };

    window.addEventListener('keydown', handleConfigureKeyDown);
    return () => window.removeEventListener('keydown', handleConfigureKeyDown);
  }, [previewTask, selectedGroupIds, grammarTopic, cefrLevel, taskType, difficulty, totalPoints, dueDate, maxAttempts, provider, modelName]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Configure & Deploy Task
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Design custom exercises, inject syllabus rules, and deploy assignments to student cohorts.
          </p>
        </div>

        <div className="flex items-center p-1.5 rounded-2xl bg-slate-100 border border-slate-200/90 shadow-2xs self-start sm:self-auto gap-1">
          <button
            type="button"
            onClick={() => setGenerationMode('QUICK')}
            className={`py-2 px-3.5 rounded-xl text-xs font-black transition flex items-center space-x-2 cursor-pointer ${
              generationMode === 'QUICK'
                ? 'bg-amber-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
            }`}
          >
            <Zap className={`w-3.5 h-3.5 ${generationMode === 'QUICK' ? 'fill-white' : 'text-amber-500'}`} />
            <div className="text-left">
              <span className="block leading-tight">Quick Practice</span>
              <span className={`text-[10px] block font-medium ${generationMode === 'QUICK' ? 'text-amber-100' : 'text-slate-400'}`}>
                1-Click Express
              </span>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setGenerationMode('DETAILED')}
            className={`py-2 px-3.5 rounded-xl text-xs font-black transition flex items-center space-x-2 cursor-pointer ${
              generationMode === 'DETAILED'
                ? 'bg-indigo-600 text-white shadow-sm ring-1 ring-indigo-500/30'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
            }`}
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <div className="text-left">
              <span className="block leading-tight">Detailed Studio</span>
              <span className={`text-[10px] block font-medium ${generationMode === 'DETAILED' ? 'text-indigo-200' : 'text-slate-400'}`}>
                Full Customization
              </span>
            </div>
          </button>
        </div>
      </div>

      {statusMessage && (
        <div
          className={`p-4 rounded-2xl flex items-center space-x-2 text-xs font-semibold ${
            statusMessage.includes('success') || statusMessage.includes('saved') || statusMessage.includes('deployed')
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : 'bg-rose-50 text-rose-800 border border-rose-200'
          }`}
        >
          <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          <span>{statusMessage}</span>
        </div>
      )}

      {/* Preset width controls (desktop only) */}
      <div className="hidden lg:flex items-center justify-between py-1 text-xs text-slate-500">
        <div className="flex items-center space-x-2">
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Layout split:</span>
          <span className="font-mono font-bold text-primary">{splitRatio}% / {100 - splitRatio}%</span>
        </div>
        <div className="flex items-center space-x-1.5">
          {[
            { label: '50 / 50', val: 50 },
            { label: '55 / 45', val: 55 },
            { label: '40 / 60', val: 40 },
            { label: '30 / 70', val: 30 },
          ].map((preset) => (
            <button
              key={preset.label}
              type="button"
              onClick={() => setSplitRatio(preset.val)}
              className={`px-2 py-0.5 rounded-lg text-[11px] font-semibold transition border ${
                splitRatio === preset.val
                  ? 'bg-indigo-50 border-primary text-primary font-bold'
                  : 'bg-white border-slate-200 text-slate-600 hover:border-slate-300'
              }`}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </div>

      <div ref={layoutContainerRef} className="flex flex-col lg:flex-row gap-0 lg:gap-3 items-stretch relative">
        <div
          className="w-full lg:shrink-0 space-y-6"
          style={isDesktop ? { flex: `0 0 ${splitRatio}%`, maxWidth: `${splitRatio}%`, width: `${splitRatio}%` } : undefined}
        >
          {generationMode === 'QUICK' ? (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-xs space-y-6">
              {/* Cohort Groups */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">
                    Target Cohort Groups (Multi-select)
                  </label>
                  {groups.length > 1 && (
                    <div className="flex items-center space-x-2">
                      <button
                        type="button"
                        onClick={() => setSelectedGroupIds(groups.map((g) => g.id))}
                        className="text-[11px] font-semibold text-primary hover:underline"
                      >
                        Select All
                      </button>
                      <span className="text-slate-300">|</span>
                      <button
                        type="button"
                        onClick={() => setSelectedGroupIds([])}
                        className="text-[11px] font-semibold text-slate-500 hover:underline"
                      >
                        Clear
                      </button>
                    </div>
                  )}
                </div>

                {selectedGroupIds.length > 1 && (
                  <div className="mb-2.5 p-2.5 rounded-xl bg-sky-50 border border-sky-200 text-xs text-sky-800 flex items-center space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-sky-600 flex-shrink-0" />
                    <span>
                      Deploying simultaneously to <strong>{selectedGroupIds.length}</strong> cohorts ({
                        groups
                          .filter((g) => selectedGroupIds.includes(g.id))
                          .reduce((acc, g) => acc + (g.studentCount || 0), 0)
                      } students total).
                    </span>
                  </div>
                )}

                <div className="flex flex-wrap gap-2">
                  {groups.map((g) => {
                    const isSelected = selectedGroupIds.includes(g.id);
                    return (
                      <button
                        type="button"
                        key={g.id}
                        onClick={() => toggleGroup(g.id)}
                        className={`py-2 px-3.5 rounded-xl border text-xs font-semibold transition flex items-center space-x-1.5 ${
                          isSelected
                            ? 'border-accent bg-sky-50 text-sky-800 ring-2 ring-sky-200'
                            : 'border-slate-200 hover:border-slate-300 text-slate-600'
                        }`}
                      >
                        {isSelected && <Check className="w-3.5 h-3.5 text-sky-700 stroke-[3]" />}
                        <span>{g.name}</span>
                        <span className="text-[10px] bg-white/80 px-1.5 py-0.5 rounded-full text-slate-500 font-mono">
                          {g.studentCount}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <QuickModePanel
                cefrLevel={cefrLevel}
                onCefrLevelChange={(lvl: CefrLevel) => {
                  setCefrLevel(lvl);
                  const defaultList = catalog?.topicsByLevel?.[lvl] || DEFAULT_CEFR_TOPICS[lvl];
                  if (defaultList && defaultList.length > 0) {
                    setGrammarTopic(defaultList[0]);
                  }
                }}
                activeTopic={grammarTopic}
                onActiveTopicChange={setGrammarTopic}
                onQuickGenerate={handleQuickGenerate}
                isLoading={isLoading}
                isStudent={false}
                onSwitchToDetailed={() => setGenerationMode('DETAILED')}
              />
            </div>
          ) : (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-xs space-y-6">
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="space-y-0.5">
                  <div className="flex items-center space-x-2">
                    <span className="text-xs font-black text-slate-800">Quick Fill Topic & Goal</span>
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-indigo-100/80 text-primary font-bold">
                      Freely editable below
                    </span>
                  </div>
                  <span className="text-[11px] text-slate-500 block">
                    Auto-fill standard syllabus topics or student gaps with one click.
                  </span>
                </div>

                <div className="flex items-center space-x-2 self-start sm:self-auto">
                  <button
                    type="button"
                    onClick={handleFillRecommended}
                    className="px-3 py-1.5 rounded-xl border border-amber-300 bg-amber-50/90 hover:bg-amber-100 text-amber-900 text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shadow-2xs"
                  >
                    <Target className="w-3.5 h-3.5 text-amber-600" />
                    <span>Recommended</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleFillRandom}
                    className="px-3 py-1.5 rounded-xl border border-indigo-200 bg-white hover:bg-indigo-50 text-indigo-900 text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shadow-2xs"
                  >
                    <Shuffle className="w-3.5 h-3.5 text-indigo-600" />
                    <span>Surprise Topic</span>
                  </button>
                </div>
              </div>

              {/* Cohort Groups */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">
                    Target Cohort Groups (Multi-select)
                  </label>
                  {groups.length > 1 && (
                    <div className="flex items-center space-x-2">
                      <button
                        type="button"
                        onClick={() => setSelectedGroupIds(groups.map((g) => g.id))}
                        className="text-[11px] font-semibold text-primary hover:underline"
                      >
                        Select All
                      </button>
                      <span className="text-slate-300">|</span>
                      <button
                        type="button"
                        onClick={() => setSelectedGroupIds([])}
                        className="text-[11px] font-semibold text-slate-500 hover:underline"
                      >
                        Clear
                      </button>
                    </div>
                  )}
                </div>

                {selectedGroupIds.length > 1 && (
                  <div className="mb-2.5 p-2.5 rounded-xl bg-sky-50 border border-sky-200 text-xs text-sky-800 flex items-center space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-sky-600 flex-shrink-0" />
                    <span>
                      Deploying simultaneously to <strong>{selectedGroupIds.length}</strong> cohorts ({
                        groups
                          .filter((g) => selectedGroupIds.includes(g.id))
                          .reduce((acc, g) => acc + (g.studentCount || 0), 0)
                      } students total).
                    </span>
                  </div>
                )}

                <div className="flex flex-wrap gap-2">
                  {groups.map((g) => {
                    const isSelected = selectedGroupIds.includes(g.id);
                    return (
                      <button
                        type="button"
                        key={g.id}
                        onClick={() => toggleGroup(g.id)}
                        className={`py-2 px-3.5 rounded-xl border text-xs font-semibold transition flex items-center space-x-1.5 ${
                          isSelected
                            ? 'border-accent bg-sky-50 text-sky-800 ring-2 ring-sky-200'
                            : 'border-slate-200 hover:border-slate-300 text-slate-600'
                        }`}
                      >
                        {isSelected && <Check className="w-3.5 h-3.5 text-sky-700 stroke-[3]" />}
                        <span>{g.name}</span>
                        <span className="text-[10px] bg-white/80 px-1.5 py-0.5 rounded-full text-slate-500 font-mono">
                          {g.studentCount}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-200/80 space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center space-x-2">
                    <Cpu className="w-4 h-4 text-primary" />
                    <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                      AI Engine
                    </span>
                  </div>
                  <div className="flex items-center p-1 rounded-xl bg-slate-200/80 border border-slate-300/60 text-xs font-bold self-start sm:self-auto">
                    <button
                      type="button"
                      onClick={() => {
                        setAiEngineMode('PLATFORM');
                        setProvider('GEMINI');
                        setModelName('gemini-flash-latest');
                      }}
                      className={`inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg transition ${
                        aiEngineMode === 'PLATFORM'
                          ? 'bg-white text-slate-900 shadow-xs'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                      <span>Platform AI (Gemini)</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setAiEngineMode('BYOK');
                        if (savedKeys.length > 0) {
                          setProvider(savedKeys[0].provider);
                          setModelName(savedKeys[0].modelName || getDefaultModelForProvider(savedKeys[0].provider));
                        }
                      }}
                      className={`inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg transition ${
                        aiEngineMode === 'BYOK'
                          ? 'bg-white text-slate-900 shadow-xs'
                          : 'text-slate-600 hover:text-slate-900'
                      }`}
                    >
                      <Key className="w-3.5 h-3.5 text-slate-500" />
                      <span>Custom Key (BYOK)</span>
                    </button>
                  </div>
                </div>

                {aiEngineMode === 'PLATFORM' ? (
                  <div className="flex items-center justify-between p-3.5 bg-white rounded-2xl border border-slate-200/90 shadow-2xs">
                    <div className="flex items-center space-x-3">
                      <div className="w-8 h-8 rounded-xl bg-indigo-50 text-primary flex items-center justify-center flex-shrink-0">
                        <Cpu className="w-4 h-4" />
                      </div>
                      <div>
                        <span className="text-xs font-bold text-slate-800">Google Gemini Flash (Latest)</span>
                        <span className="text-[11px] text-slate-500 block">Fast, reliable educator generation included in your tier</span>
                      </div>
                    </div>
                    <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200/80 px-2.5 py-1 rounded-full">
                      Active
                    </span>
                  </div>
                ) : savedKeys.length > 0 ? (
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between p-3.5 bg-white rounded-2xl border border-slate-200/90 shadow-2xs gap-3">
                    <div className="flex items-center space-x-3">
                      <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0">
                        <Key className="w-4 h-4" />
                      </div>
                      <div>
                        {savedKeys.length > 1 ? (
                          <div className="flex items-center space-x-2">
                            <select
                              value={Math.max(0, savedKeys.findIndex((k) => k.provider === provider))}
                              onChange={(e) => {
                                const idx = parseInt(e.target.value);
                                const selected = savedKeys[idx];
                                if (selected) {
                                  setProvider(selected.provider);
                                  setModelName(selected.modelName || getDefaultModelForProvider(selected.provider));
                                }
                              }}
                              className="text-xs font-bold text-slate-800 bg-slate-50 border border-slate-200 rounded-lg px-2 py-1 focus:outline-none focus:ring-1 focus:ring-primary"
                            >
                              {savedKeys.map((k, idx) => (
                                <option key={k.id || idx} value={idx}>
                                  {k.provider} · {k.modelName || 'Default'}
                                </option>
                              ))}
                            </select>
                          </div>
                        ) : (
                          <span className="text-xs font-bold text-slate-800">
                            {savedKeys[0].provider} · {savedKeys[0].modelName || 'Default Model'}
                          </span>
                        )}
                        <span className="text-[11px] text-slate-500 block">Using your personal educator API key</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => navigate('/profile')}
                      className="text-xs font-bold text-primary hover:underline whitespace-nowrap self-end sm:self-auto"
                    >
                      Change in Profile →
                    </button>
                  </div>
                ) : (
                  <div className="p-4 bg-amber-50 border border-amber-200/90 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between text-xs text-amber-900 gap-3">
                    <div className="flex items-center space-x-2.5">
                      <Key className="w-5 h-5 text-amber-600 flex-shrink-0" />
                      <span>
                        No custom API keys configured. You can add your OpenAI, Anthropic, DeepSeek, or Groq key in Profile.
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => navigate('/profile')}
                      className="self-end sm:self-auto px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl font-bold whitespace-nowrap text-xs shadow-xs transition"
                    >
                      Configure Key
                    </button>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                  CEFR Level
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                  {(['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as CefrLevel[]).map((level) => (
                    <button
                      type="button"
                      key={level}
                      onClick={() => {
                        setCefrLevel(level);
                        const defaultList = catalog?.topicsByLevel?.[level] || DEFAULT_CEFR_TOPICS[level];
                        if (defaultList && defaultList.length > 0) {
                          setGrammarTopic(defaultList[0]);
                        }
                      }}
                      className={`py-2 px-2.5 rounded-xl border text-xs font-bold flex items-center justify-center space-x-1.5 transition ${
                        cefrLevel === level
                          ? 'border-primary bg-indigo-50 text-primary'
                          : 'border-slate-200 text-slate-600 hover:border-slate-300'
                      }`}
                    >
                      <CefrBadge level={level} size="sm" />
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                  Task Format
                </label>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                  {[
                    { id: 'GRAMMAR', label: 'Grammar', icon: BookOpen, desc: 'MCQ, Cloze, Brackets, Rewrite' },
                    { id: 'READING', label: 'Reading', icon: FileText, desc: 'Comprehension passage & questions' },
                    { id: 'SPEAKING', label: 'Speaking', icon: Mic, desc: 'Phonetics & speech evaluation' },
                    { id: 'ESSAY', label: 'Essay', icon: PenTool, desc: 'Discursive writing & rubric scoring' },
                  ].map((mod) => {
                    const isSelected =
                      (mod.id === 'GRAMMAR' && isGrammarTask) ||
                      (mod.id === 'READING' && taskType === 'READING') ||
                      (mod.id === 'SPEAKING' && taskType === 'SPEAKING') ||
                      (mod.id === 'ESSAY' && taskType === 'ESSAY');

                    return (
                      <button
                        type="button"
                        key={mod.id}
                        onClick={() => {
                          if (mod.id === 'GRAMMAR') {
                            if (!isGrammarTask) setTaskType('MCQ');
                          } else if (mod.id === 'READING') {
                            setTaskType('READING');
                          } else if (mod.id === 'SPEAKING') {
                            setTaskType('SPEAKING');
                          } else {
                            setTaskType('ESSAY');
                          }
                        }}
                        className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between cursor-pointer ${
                          isSelected
                            ? 'border-primary bg-indigo-50/90 text-primary ring-2 ring-primary/20 shadow-xs'
                            : 'border-slate-200 bg-white hover:border-slate-300 text-slate-700'
                        }`}
                      >
                        <div className="flex items-center space-x-2 mb-1">
                          <mod.icon className={`w-4 h-4 ${isSelected ? 'text-primary' : 'text-slate-500'}`} />
                          <span className={`text-xs font-bold ${isSelected ? 'text-primary' : 'text-slate-900'}`}>
                            {mod.label}
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-400 leading-tight">
                          {mod.desc}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {isGrammarTask && (
                <GrammarConfigPanel
                  taskType={taskType}
                  onTaskTypeChange={setTaskType}
                  cefrLevel={cefrLevel}
                  grammarTopic={grammarTopic}
                  onGrammarTopicChange={setGrammarTopic}
                  domain={domain}
                  onDomainChange={setDomain}
                  difficulty={difficulty}
                  onDifficultyChange={setDifficulty}
                  numberOfQuestions={numberOfQuestions}
                  onNumberOfQuestionsChange={setNumberOfQuestions}
                  ecoMode={ecoMode}
                  onEcoModeChange={setEcoMode}
                  customRule={customRule}
                  onCustomRuleChange={setCustomRule}
                  customVocabulary={customVocabulary}
                  onCustomVocabularyChange={setCustomVocabulary}
                  uploadedRuleFile={uploadedRuleFile}
                  onUploadedRuleFileChange={setUploadedRuleFile}
                  uploadedVocabFile={uploadedVocabFile}
                  onUploadedVocabFileChange={setUploadedVocabFile}
                  catalog={catalog}
                  isStudent={false}
                  savedRules={savedRules}
                  savedVocabSets={savedVocabSets}
                  onSaveRuleToLibrary={handleSaveRuleToLibrary}
                  onSaveVocabToLibrary={handleSaveVocabToLibrary}
                  isSavingRule={isSavingRule}
                  isSavingVocab={isSavingVocab}
                />
              )}

              {taskType === 'READING' && (
                <div className="animate-in fade-in duration-200">
                  <ReadingConfigPanel
                    config={readingConfig}
                    onChange={(updated) => setReadingConfig((prev) => ({ ...prev, ...updated }))}
                    cefrLevel={cefrLevel}
                    customVocabulary={customVocabulary}
                    onCustomVocabularyChange={setCustomVocabulary}
                    savedVocabSets={savedVocabSets}
                    isStudent={false}
                  />
                </div>
              )}

              {taskType === 'SPEAKING' && (
                <div className="animate-in fade-in duration-200">
                  <SpeakingConfigPanel
                    cefrLevel={cefrLevel}
                    speakingTitle={speakingTitle}
                    onTitleChange={setSpeakingTitle}
                    targetSounds={targetSounds}
                    onTargetSoundsChange={setTargetSounds}
                    pronunciationPackId={pronunciationPackId}
                    onPronunciationPackIdChange={setPronunciationPackId}
                    speakingLength={speakingLength}
                    onSpeakingLengthChange={setSpeakingLength}
                    totalPoints={totalPoints}
                    onTotalPointsChange={setTotalPoints}
                    ecoMode={ecoMode}
                    onEcoModeChange={setEcoMode}
                    isStudent={false}
                    customWordCount={customWordCount}
                    onCustomWordCountChange={setCustomWordCount}
                    customInstructions={customInstructions}
                    onCustomInstructionsChange={setCustomInstructions}
                  />
                </div>
              )}

              {taskType === 'ESSAY' && (
                <div className="animate-in fade-in duration-200">
                  <EssayConfigPanel
                    cefrLevel={cefrLevel}
                    topic={grammarTopic}
                    onTopicChange={setGrammarTopic}
                    domain={domain}
                    onDomainChange={setDomain}
                    customRule={customRule}
                    onCustomRuleChange={setCustomRule}
                    customVocabulary={customVocabulary}
                    onCustomVocabularyChange={setCustomVocabulary}
                    totalPoints={totalPoints}
                    onTotalPointsChange={setTotalPoints}
                    savedRules={savedRules}
                    savedVocabSets={savedVocabSets}
                    isStudent={false}
                  />
                </div>
              )}

              <div className="p-3.5 bg-amber-50/70 rounded-2xl border border-amber-200/80 flex flex-col sm:flex-row sm:items-center justify-between text-xs text-amber-900 gap-2">
                <div className="flex items-center space-x-2">
                  <Zap className="w-4 h-4 text-amber-600 shrink-0" />
                  <span>Prefer 1-Click Practice without adjusting manual parameters?</span>
                </div>
                <button
                  type="button"
                  onClick={() => setGenerationMode('QUICK')}
                  className="px-3.5 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition flex items-center space-x-1.5 whitespace-nowrap text-xs self-end sm:self-auto cursor-pointer shadow-2xs"
                >
                  <span>Switch to Quick Practice</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Due Date & Attempt Limit */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                    Submission Due Date
                  </label>
                  <input
                    type="datetime-local"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                    Allowed Student Attempts
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setMaxAttempts(1)}
                      className={`py-2.5 px-3 rounded-xl border text-xs font-bold transition ${
                        maxAttempts === 1
                          ? 'border-primary bg-indigo-50 text-primary shadow-xs'
                          : 'border-slate-200 text-slate-600 hover:border-slate-300'
                      }`}
                    >
                      1 Attempt (Default)
                    </button>
                    <button
                      type="button"
                      onClick={() => setMaxAttempts(0)}
                      className={`py-2.5 px-3 rounded-xl border text-xs font-bold transition ${
                        maxAttempts === 0
                          ? 'border-emerald-600 bg-emerald-50 text-emerald-700 shadow-xs'
                          : 'border-slate-200 text-slate-600 hover:border-slate-300'
                      }`}
                    >
                      ∞ Unlimited
                    </button>
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1">
                    {maxAttempts === 1
                      ? 'Single attempt: once submitted, students view their graded results.'
                      : 'Unlimited attempts: students can retry this assignment for practice.'}
                  </p>
                </div>
              </div>

              {/* Total Assignment Points / Max Score */}
              {taskType !== 'READING' && (
                <div className="p-4 rounded-2xl bg-indigo-50/40 border border-indigo-100/80 space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                      Total Assignment Points / Max Score
                    </label>
                    <span className="text-xs font-mono font-bold text-primary bg-white px-2 py-0.5 rounded-lg border border-indigo-100 shadow-xs">
                      {totalPoints} pts
                    </span>
                  </div>
                  <div className="flex items-center space-x-3">
                    <NumberStepper
                      value={totalPoints}
                      min={1}
                      max={1000}
                      step={10}
                      onChange={(val) => {
                        setTotalPoints(val);
                        if (previewTask) {
                          setPreviewTask({ ...previewTask, totalPoints: val });
                        }
                      }}
                      size="sm"
                      ariaLabel="Total Assignment Points"
                    />
                    <p className="text-[11px] text-slate-500 leading-snug">
                      Overall maximum score for this homework. Automatically distributed across questions during preview.
                    </p>
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="flex flex-wrap items-center gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={handlePreview}
                  disabled={isLoading}
                  className="flex items-center space-x-1.5 py-2.5 px-4 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold transition"
                >
                  <Eye className="w-4 h-4" />
                  <span>Preview</span>
                  <kbd className="kbd-shortcut hidden md:inline-block px-1.5 py-0.5 rounded bg-slate-100 border border-slate-200 text-[10px] font-mono font-bold text-slate-500 ml-1">
                    Ctrl+P
                  </kbd>
                </button>

                <button
                  type="button"
                  onClick={handleSaveTemplate}
                  disabled={isLoading}
                  className="flex items-center space-x-1.5 py-2.5 px-4 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold transition"
                >
                  <Bookmark className="w-4 h-4" />
                  <span>Save Template</span>
                  <kbd className="kbd-shortcut hidden md:inline-block px-1.5 py-0.5 rounded bg-slate-100 border border-slate-200 text-[10px] font-mono font-bold text-slate-500 ml-1">
                    Ctrl+S
                  </kbd>
                </button>

                <button
                  type="button"
                  onClick={handleDeploy}
                  disabled={isLoading}
                  className="flex-1 flex items-center justify-center space-x-2 py-3 px-5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
                >
                  <Send className="w-4 h-4" />
                  <span>Deploy to Students</span>
                  <kbd className="kbd-shortcut hidden md:inline-block px-1.5 py-0.5 rounded bg-white/20 text-[10px] font-mono font-bold ml-1">
                    Ctrl+Enter
                  </kbd>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Draggable Vertical Splitter Bar (Desktop only) */}
        <div
          onMouseDown={() => setIsDraggingSplitter(true)}
          className={`hidden lg:flex flex-col items-center justify-center w-3 cursor-col-resize select-none shrink-0 group relative z-10 transition-colors ${
            isDraggingSplitter ? 'opacity-100' : 'hover:opacity-100'
          }`}
          title="Drag to resize columns"
        >
          <div
            className={`w-1.5 h-36 rounded-full transition-all duration-150 ${
              isDraggingSplitter
                ? 'bg-primary ring-4 ring-primary/20 scale-y-110'
                : 'bg-slate-200 group-hover:bg-primary/60'
            }`}
          />
        </div>

        {/* Live Preview Column */}
        <div className="w-full lg:min-w-0 flex-1 space-y-4 mt-6 lg:mt-0">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
              Live Preview & Customizer
            </h2>
            {previewTask && (
              <button
                type="button"
                onClick={handlePreview}
                disabled={isLoading}
                className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border border-slate-200 text-xs font-semibold text-slate-600 hover:bg-slate-50 transition"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
                <span>Regenerate</span>
              </button>
            )}
          </div>

          {previewTask ? (
            <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-sm space-y-6">
              {/* Header Badges & Total Points Toolbar */}
              <div className="space-y-3 pb-4 border-b border-slate-100">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center space-x-2">
                    <CefrBadge level={previewTask.cefrLevel} size="sm" />
                    <span className="text-xs font-bold text-slate-800 px-2.5 py-1 rounded-lg bg-slate-100">
                      {previewTask.type}
                    </span>
                    <span className="text-xs font-semibold text-slate-500">
                      {previewTask.grammarTopic}
                    </span>
                  </div>
                  <span className="text-xs font-bold text-primary bg-indigo-50 px-2.5 py-1 rounded-lg border border-indigo-100">
                    {previewTask.questions?.length || 0} Questions
                  </span>
                </div>

                {/* Editable Total Points & Distribution Toolbar */}
                <div className="flex flex-wrap items-center justify-between gap-3 p-3.5 rounded-2xl bg-slate-50/80 border border-slate-200/90">
                  <div className="flex items-center space-x-2.5">
                    <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                      Total Points:
                    </span>
                    <NumberStepper
                      value={previewTask.totalPoints ?? totalPoints}
                      min={1}
                      max={1000}
                      step={10}
                      onChange={handleUpdateTotalPoints}
                      size="sm"
                      ariaLabel="Preview Total Points"
                    />
                  </div>

                  {/* Question Points Sum Indicator & Quick Balance Tools */}
                  <div className="flex flex-wrap items-center gap-2">
                    {(() => {
                      const questionsSum = (previewTask.questions || []).reduce((acc, q) => acc + (q.points || 0), 0);
                      const currentTotal = previewTask.totalPoints ?? totalPoints;
                      const isMatching = questionsSum === currentTotal;
                      return (
                        <div className="flex flex-wrap items-center gap-2">
                          <span
                            className={`px-2.5 py-1 rounded-xl text-[11px] font-bold border flex items-center space-x-1.5 ${
                              isMatching
                                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                                : 'bg-amber-50 text-amber-800 border-amber-200'
                            }`}
                            title={`Sum of question prices: ${questionsSum} pts. Total score: ${currentTotal} pts.`}
                          >
                            <span>Questions Sum:</span>
                            <span className="font-mono">{questionsSum} / {currentTotal} pts</span>
                            {isMatching ? (
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                            ) : (
                              <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
                            )}
                          </span>

                          <button
                            type="button"
                            onClick={handleDistributePointsEvenly}
                            className="px-2.5 py-1 rounded-xl bg-white hover:bg-slate-100 border border-slate-200 text-xs font-bold text-slate-700 transition shadow-xs"
                            title="Distribute total points evenly across all questions"
                          >
                            Distribute Evenly
                          </button>

                          {!isMatching && (
                            <button
                              type="button"
                              onClick={handleSyncTotalFromQuestions}
                              className="px-2.5 py-1 rounded-xl bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 text-xs font-bold text-primary transition"
                              title="Set total points equal to question sum"
                            >
                              Sync Total ({questionsSum})
                            </button>
                          )}
                        </div>
                      );
                    })()}
                  </div>
                </div>
              </div>

              {/* Informational Hint */}
              <div className="p-3.5 rounded-2xl bg-indigo-50/70 border border-indigo-100/80 text-xs text-indigo-900 leading-relaxed flex items-start space-x-2">
                <Info className="w-4 h-4 text-primary shrink-0 mt-0.5" />
                <p>
                  <span className="font-bold">Interactive Editor:</span> You can edit instructions, question prompts, option choices, question point prices, and switch correct answers before deploying.
                </p>
              </div>

              {/* Editable Instructions / Reading / Speaking Passage */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider">
                    {previewTask.type === 'SPEAKING'
                      ? 'Oral Reading Passage (Editable by Teacher)'
                      : previewTask.type === 'READING'
                      ? 'Reading Passage (Editable)'
                      : 'Exercise Instructions / Reading Passage'}
                  </label>
                  <div className="flex items-center space-x-2">
                    {previewTask.type === 'SPEAKING' && previewTask.content && (
                      <button
                        type="button"
                        onClick={() => speakText(previewTask.content)}
                        className="flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition"
                        title="Listen to pronunciation of passage"
                      >
                        <Volume2 className="w-3.5 h-3.5" />
                        <span>Listen</span>
                      </button>
                    )}
                    {(previewTask.type === 'READING' || previewTask.type === 'SPEAKING') && (
                      <span className="text-[11px] font-semibold text-primary">
                        {previewTask.content?.split(/\s+/).filter(Boolean).length || 0} words
                      </span>
                    )}
                  </div>
                </div>
                <AutoExpandingTextarea
                  minRows={previewTask.type === 'READING' ? 8 : previewTask.type === 'SPEAKING' ? 5 : 3}
                  value={previewTask.content}
                  onChange={handleUpdateInstruction}
                  placeholder={
                    previewTask.type === 'SPEAKING'
                      ? 'Review and edit the oral speech passage text here...'
                      : previewTask.type === 'READING'
                      ? 'Review and edit the full reading comprehension passage text here...'
                      : 'Enter exercise instructions or reading context here...'
                  }
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition leading-relaxed font-serif"
                />
              </div>

              {/* Questions Section */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                    Questions ({previewTask.questions?.length || 0})
                  </h3>
                  <button
                    type="button"
                    onClick={handleAddQuestion}
                    className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Add Question</span>
                  </button>
                </div>

                {(!previewTask.questions || previewTask.questions.length === 0) ? (
                  <div className="p-6 text-center rounded-2xl border border-dashed border-slate-200 text-xs text-slate-400">
                    No questions in this task. Click "+ Add Question" to create questions manually.
                  </div>
                ) : (
                  <div className="space-y-4">
                    {previewTask.questions.map((q, qIdx) => (
                      <div
                        key={q.id || `q-${qIdx}`}
                        onDragOver={(e) => handleDragOver(e, qIdx)}
                        className={`rounded-2xl border p-4 space-y-3.5 transition-all duration-150 ${
                          draggedQIndex === qIdx
                            ? 'border-primary ring-2 ring-primary/40 bg-indigo-50/70 shadow-lg scale-[1.01]'
                            : 'border-slate-200 bg-slate-50/50 hover:border-slate-300'
                        }`}
                      >
                        {/* Question Card Header with Drag Handle & Points Price Stepper */}
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            {/* Grip Drag Handle */}
                            <div
                              draggable
                              onDragStart={(e) => handleDragStart(e, qIdx)}
                              onDragEnd={handleDragEnd}
                              className="p-1 text-slate-400 hover:text-slate-700 cursor-grab active:cursor-grabbing rounded-lg hover:bg-slate-200/60 transition"
                              title="Drag to reorder question"
                            >
                              <GripVertical className="w-4 h-4" />
                            </div>

                            <span className="w-6 h-6 rounded-lg bg-primary/10 text-primary text-xs font-black flex items-center justify-center">
                              {qIdx + 1}
                            </span>
                            <span className="text-xs font-bold text-slate-700">
                              Question {qIdx + 1}
                            </span>
                            {previewTask.type === 'READING' && (
                              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-200 text-slate-700">
                                {Array.isArray(q.options) && q.options.length === 2 && (q.options.includes('True') || q.options.includes('False'))
                                  ? 'True / False'
                                  : Array.isArray(q.options) && q.options.length > 0
                                  ? 'Multiple Choice'
                                  : 'Sentence Completion'}
                              </span>
                            )}
                          </div>

                          <div className="flex items-center space-x-2">
                            {/* Question Point Price Stepper */}
                            <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-xl bg-white border border-slate-200 shadow-xs">
                              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                                Points:
                              </span>
                              <NumberStepper
                                value={q.points ?? 20}
                                min={1}
                                max={500}
                                step={5}
                                onChange={(val) => handleUpdateQuestionPoints(qIdx, val)}
                                size="sm"
                                ariaLabel={`Question ${qIdx + 1} Points`}
                              />
                            </div>

                            <button
                              type="button"
                              onClick={() => handleDeleteQuestion(qIdx)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                              title="Delete question"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>

                        <div>
                          <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                            Prompt / Sentence
                          </label>
                          <AutoExpandingTextarea
                            minRows={2}
                            value={q.questionText}
                            onChange={(val) =>
                              handleUpdateQuestion(qIdx, { questionText: val })
                            }
                            placeholder="Enter the question sentence or prompt..."
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                          />
                        </div>

                        {/* MCQ & Choice Options vs Open Answers */}
                        {previewTask.type === 'MCQ' ||
                        (previewTask.type === 'READING' && Array.isArray(q.options) && q.options.length > 0) ? (
                          <div className="space-y-2">
                            <div className="flex items-center justify-between">
                              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                                Answer Choices (Select the correct option)
                              </label>
                              <div className="flex items-center space-x-2">
                                <button
                                  type="button"
                                  onClick={() => handleAddOption(qIdx)}
                                  className="text-[11px] font-bold text-primary hover:underline inline-flex items-center space-x-1"
                                >
                                  <Plus className="w-3 h-3" />
                                  <span>Add Choice</span>
                                </button>
                                {previewTask.type === 'READING' && (
                                  <button
                                    type="button"
                                    onClick={() => handleUpdateQuestion(qIdx, { options: [] })}
                                    className="text-[11px] font-semibold text-slate-500 hover:text-slate-700"
                                  >
                                    Switch to Open Question
                                  </button>
                                )}
                              </div>
                            </div>

                            <div className="space-y-2">
                              {(q.options || []).map((opt, optIdx) => {
                                const isCorrect = q.correctAnswer === opt;
                                return (
                                  <div key={optIdx} className="flex items-center space-x-2">
                                    <button
                                      type="button"
                                      onClick={() => handleSetCorrectAnswer(qIdx, opt)}
                                      className={`px-2.5 py-1.5 rounded-xl border text-[11px] font-bold shrink-0 flex items-center space-x-1.5 transition ${
                                        isCorrect
                                          ? 'border-emerald-500 bg-emerald-50 text-emerald-700 shadow-sm'
                                          : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300'
                                      }`}
                                      title={isCorrect ? 'Correct answer' : 'Click to mark as correct'}
                                    >
                                      {isCorrect ? (
                                        <>
                                          <Check className="w-3 h-3 text-emerald-600 stroke-[3]" />
                                          <span>Correct</span>
                                        </>
                                      ) : (
                                        <span className="text-slate-400">Mark Correct</span>
                                      )}
                                    </button>

                                    <AutoExpandingTextarea
                                      minRows={1}
                                      value={opt}
                                      onChange={(val) =>
                                        handleOptionChange(qIdx, optIdx, val)
                                      }
                                      className={`flex-1 px-3 py-1.5 rounded-xl border text-xs font-medium focus:outline-none transition ${
                                        isCorrect
                                          ? 'border-emerald-400 bg-emerald-50/20 text-emerald-900 font-semibold'
                                          : 'border-slate-200 bg-white text-slate-800'
                                      }`}
                                    />

                                    {(q.options || []).length > 2 && (
                                      <button
                                        type="button"
                                        onClick={() => handleRemoveOption(qIdx, optIdx)}
                                        className="p-1.5 text-slate-400 hover:text-rose-500 rounded-lg transition"
                                        title="Remove choice"
                                      >
                                        <Trash2 className="w-3 h-3" />
                                      </button>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          </div>
                        ) : (
                          <div className="space-y-1.5">
                            <div className="flex items-center justify-between">
                              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                                Expected Correct Answer Key
                              </label>
                              {previewTask.type === 'READING' && (
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleUpdateQuestion(qIdx, {
                                      options: ['Option A', 'Option B', 'Option C', 'Option D'],
                                      correctAnswer: 'Option A',
                                    })
                                  }
                                  className="text-[11px] font-bold text-primary hover:underline"
                                >
                                  + Convert to Choices (MCQ)
                                </button>
                              )}
                            </div>
                            <AutoExpandingTextarea
                              minRows={1}
                              value={q.correctAnswer}
                              onChange={(val) => handleSetCorrectAnswer(qIdx, val)}
                              placeholder="e.g. clean electricity / renewable resources"
                              className="w-full px-3 py-2 rounded-xl border border-emerald-300 bg-emerald-50/20 text-emerald-950 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                            />
                            <p className="text-[11px] text-slate-400">
                              For open-ended questions, AI performs semantic evaluation—student answers matching this meaning will be accepted.
                            </p>
                          </div>
                        )}

                        {/* Grammar Rule / Hint */}
                        <div>
                          <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                            Grammar Rule / Explanatory Hint
                          </label>
                          <AutoExpandingTextarea
                            minRows={1}
                            value={q.grammarRule || ''}
                            onChange={(val) =>
                              handleUpdateQuestion(qIdx, { grammarRule: val })
                            }
                            placeholder="e.g. Present Perfect: subject + have/has + V3"
                            className="w-full px-3 py-1.5 rounded-xl border border-slate-200 bg-white text-xs text-slate-600 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Bottom Quick Action Buttons in Preview Column */}
              <div className="flex flex-wrap items-center gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={handleSaveTemplate}
                  disabled={isLoading}
                  className="flex items-center space-x-1.5 py-2.5 px-4 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold transition"
                >
                  <Bookmark className="w-4 h-4" />
                  <span>Save Edited Template</span>
                  <kbd className="kbd-shortcut hidden md:inline-block px-1.5 py-0.5 rounded bg-slate-100 border border-slate-200 text-[10px] font-mono font-bold text-slate-500 ml-1">
                    Ctrl+S
                  </kbd>
                </button>

                <button
                  type="button"
                  onClick={handleDeploy}
                  disabled={isLoading}
                  className="flex-1 flex items-center justify-center space-x-2 py-2.5 px-5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
                >
                  <Send className="w-4 h-4" />
                  <span>Deploy Edited Task</span>
                  <kbd className="kbd-shortcut hidden md:inline-block px-1.5 py-0.5 rounded bg-white/20 text-[10px] font-mono font-bold ml-1">
                    Ctrl+Enter
                  </kbd>
                </button>
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-3xl p-8 text-center text-slate-400 border border-dashed border-slate-200 text-xs">
              Click "Preview" to inspect and customize the AI-generated questions before assigning to your students.
            </div>
          )}
        </div>
      </div>

      {concurrentConflict && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-2xl border border-amber-200/80 max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-150">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-200 text-amber-600 flex items-center justify-center mx-auto shadow-sm">
              <Smartphone className="w-6 h-6" />
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-base font-bold text-slate-900">
                Active Generation on Another Device
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                An AI generation session is currently active on another device associated with your account.
                You can transfer control to this device to proceed immediately.
              </p>
            </div>
            <div className="pt-2 flex flex-col sm:flex-row gap-2">
              <button
                type="button"
                onClick={handleTakeoverAndProceed}
                disabled={isTakingOver}
                className="flex-1 py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 shadow-sm disabled:opacity-50"
              >
                <ArrowRightLeft className="w-4 h-4" />
                <span>{isTakingOver ? 'Switching...' : 'Switch to This Device'}</span>
              </button>
              <button
                type="button"
                onClick={() => setConcurrentConflict(false)}
                className="py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
