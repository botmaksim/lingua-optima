/**
 * @file ConfigureTask.tsx
 * @brief Educator task generation, parameter configuration, preview, custom curriculum context, and cohort deployment interface.
 */

import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Send,
  Bookmark,
  Eye,
  CheckCircle2,
  Cpu,
  BookOpen,
  ChevronDown,
  ChevronUp,
  UploadCloud,
  FileText,
  Trash2,
  Sparkles,
  Leaf,
  Plus,
  RefreshCw,
  Check,
  Key,
  ArrowRightLeft,
  Smartphone,
} from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { taskApi } from '../../api/taskApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
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
import { CustomSelect } from '../common/CustomSelect';
import { TopicSelector } from '../common/TopicSelector';
import { useNotificationStore } from '../../store/notificationStore';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  useProviderModels,
  buildProviderOptionGroups,
  isSystemFreeProvider,
} from '../../constants/aiModels';
import { useUIStore } from '../../store/uiStore';
import { customCurriculumApi } from '../../api/customCurriculumApi';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { DEFAULT_CEFR_TOPICS } from '../../constants/topics';

/**
 * @brief Teacher component for configuring and deploying AI-generated assignments to student groups.
 * @return React component element.
 */
export const ConfigureTask: React.FC = () => {
  const navigate = useNavigate();
  const { addToast } = useNotificationStore();
  const { openUpgradeWall } = useUIStore();

  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([]);
  const [catalog, setCatalog] = useState<TopicsCatalogResponse | null>(null);
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>('B1');
  const [grammarTopic, setGrammarTopic] = useState<string>('Passive Voice (Basic)');
  const [domain, setDomain] = useState<string>('Academic');
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const difficulty: DifficultyLevel = 'MEDIUM';
  const numberOfQuestions = 5;
  const [totalPoints, setTotalPoints] = useState<number>(100);
  const [dueDate, setDueDate] = useState<string>('');
  const [maxAttempts, setMaxAttempts] = useState<number>(1);
  const [provider, setProvider] = useState<AIProviderType>('GEMINI');
  const [modelName, setModelName] = useState<string>(getDefaultModelForProvider('GEMINI'));
  const { models: providerModels, isLiveSynced } = useProviderModels(provider);
  const [savedKeys, setSavedKeys] = useState<ApiKeyItem[]>([]);
  const [ecoMode, setEcoMode] = useState<boolean>(false);

  // Curriculum context state
  const [showCurriculumPanel, setShowCurriculumPanel] = useState<boolean>(false);
  const [customRule, setCustomRule] = useState<string>('');
  const [customVocabulary, setCustomVocabulary] = useState<string>('');
  const [uploadedRuleFile, setUploadedRuleFile] = useState<{
    fileName: string;
    fileSize: number;
    contentSnippet: string;
    serverPath: string;
  } | null>(null);
  const [uploadedVocabFile, setUploadedVocabFile] = useState<{
    fileName: string;
    fileSize: number;
    contentSnippet: string;
    serverPath: string;
  } | null>(null);
  const [isUploadingRule, setIsUploadingRule] = useState<boolean>(false);
  const [isUploadingVocab, setIsUploadingVocab] = useState<boolean>(false);
  const [isLoadingRef, setIsLoadingRef] = useState<boolean>(false);
  const [refNotice, setRefNotice] = useState<string | null>(null);

  const ruleFileInputRef = useRef<HTMLInputElement>(null);
  const vocabFileInputRef = useRef<HTMLInputElement>(null);

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

  useEffect(() => {
    groupApi
      .getGroups()
      .then((data) => {
        setGroups(data);
        if (data.length > 0) setSelectedGroupIds([data[0].id]);
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
        if (keys.length > 0) {
          const first = keys[0];
          setProvider(first.provider);
          setModelName(first.modelName || getDefaultModelForProvider(first.provider));
        }
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

  const userConfiguredProviders = useMemo(() => savedKeys.map((k) => k.provider), [savedKeys]);
  const providerGroups = useMemo(() => buildProviderOptionGroups(userConfiguredProviders), [userConfiguredProviders]);
  const isSelectedProviderConfigured = isSystemFreeProvider(provider) || userConfiguredProviders.includes(provider);

  /**
   * @brief Event handler updating selected AI provider and its associated model.
   * @param nextProvider New AI provider identifier.
   */
  const handleProviderChange = (nextProvider: AIProviderType) => {
    setProvider(nextProvider);
    const matchingKey = savedKeys.find((k) => k.provider === nextProvider);
    setModelName(matchingKey?.modelName || getDefaultModelForProvider(nextProvider));
  };

  /**
   * @brief Event handler or helper executing toggle group.
   */
  const toggleGroup = (id: string) => {
    setSelectedGroupIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
    );
  };

  /**
   * @brief Loads canonical or synthesized reference curriculum from server.
   */
  const handleLoadReference = async () => {
    setIsLoadingRef(true);
    setRefNotice(null);
    try {
      const ref = await taskApi.getCurriculumReference(cefrLevel, grammarTopic);
      if (ref.referenceRule) {
        setCustomRule(ref.referenceRule);
      }
      if (ref.referenceVocabulary && ref.referenceVocabulary.length > 0) {
        setCustomVocabulary(ref.referenceVocabulary.join(', '));
      }
      setRefNotice(
        ref.source === 'CANONICAL'
          ? 'Loaded canonical curriculum rules & vocabulary from server repository!'
          : 'Synthesized reference curriculum aligned with CEFR standards.'
      );
      setShowCurriculumPanel(true);
    } catch (err: any) {
      console.error('Failed to load reference curriculum:', err);
      setRefNotice('Could not fetch reference curriculum.');
    } finally {
      setIsLoadingRef(false);
    }
  };

  /**
   * @brief Handles file upload for rule or vocabulary context.
   */
  const handleFileUpload = async (
    e: React.ChangeEvent<HTMLInputElement>,
    type: 'RULE' | 'VOCABULARY'
  ) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (type === 'RULE') {
      setIsUploadingRule(true);
    } else {
      setIsUploadingVocab(true);
    }

    try {
      const res = await taskApi.uploadCurriculumFile(file, type, grammarTopic);
      if (type === 'RULE') {
        setUploadedRuleFile({
          fileName: res.fileName,
          fileSize: res.fileSize,
          contentSnippet: res.contentSnippet,
          serverPath: res.serverPath,
        });
        if (res.fullContent) {
          setCustomRule(res.fullContent);
        }
      } else {
        setUploadedVocabFile({
          fileName: res.fileName,
          fileSize: res.fileSize,
          contentSnippet: res.contentSnippet,
          serverPath: res.serverPath,
        });
        if (res.fullContent) {
          setCustomVocabulary(res.fullContent);
        }
      }
      setShowCurriculumPanel(true);
    } catch (err: any) {
      console.error('Failed to upload curriculum file:', err);
      setStatusMessage(err.response?.data?.message || 'Failed to upload curriculum file.');
    } finally {
      if (type === 'RULE') {
        setIsUploadingRule(false);
        if (ruleFileInputRef.current) ruleFileInputRef.current.value = '';
      } else {
        setIsUploadingVocab(false);
        if (vocabFileInputRef.current) vocabFileInputRef.current.value = '';
      }
    }
  };

  /**
   * @brief Helper returning current task parameters including curriculum context.
   */
  const buildTaskParams = () => ({
    cefrLevel,
    grammarTopic: grammarTopic.trim() || 'General Practice',
    domain,
    taskType,
    difficulty,
    numberOfQuestions,
    totalPoints,
    provider,
    modelName,
    customRule: customRule.trim() || undefined,
    customVocabulary: customVocabulary.trim() || undefined,
    ruleFilePath: uploadedRuleFile?.serverPath || undefined,
    vocabularyFilePath: uploadedVocabFile?.serverPath || undefined,
    ecoMode,
  });

  /**
   * @brief Event handler executing preview generation.
   */
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

  /**
   * @brief Event handler executing save template.
   */
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

  /**
   * @brief Event handler executing assignment deployment to selected groups.
   */
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
        const task = await taskApi.generateTask(buildTaskParams());
        await taskApi.assignTask(task.id, selectedGroupIds, dueDate || undefined, maxAttempts);
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
        title: 'Управление перенесено',
        message: 'Сессия генерации успешно перенесена на это устройство.',
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
        title: 'Ошибка переноса',
        message: e.response?.data?.message || 'Не удалось перенести управление генерацией.',
      });
    } finally {
      setIsTakingOver(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">
          Configure & Deploy Task
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Design custom exercises, inject syllabus rules, and deploy assignments to student cohorts.
        </p>
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
          {/* AI Engine Selection */}
          <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-200/80 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Cpu className="w-4 h-4 text-primary" />
                <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  AI Engine & Model
                </span>
              </div>
              <span className="text-[11px] font-mono font-semibold text-primary bg-indigo-50 px-2.5 py-0.5 rounded-full">
                {provider} · {modelName}
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase mb-1">
                  AI Provider
                </label>
                <CustomSelect
                  size="sm"
                  value={provider}
                  onChange={(val) => handleProviderChange(val as AIProviderType)}
                  groups={providerGroups}
                  ariaLabel="AI Provider"
                />
              </div>

              <div>
                <label className="flex items-center justify-between text-[11px] font-bold text-slate-500 uppercase mb-1">
                  <span>AI Model</span>
                  {isLiveSynced && (
                    <span className="text-[10px] font-semibold text-emerald-600 lowercase">
                      ● live vendor sync
                    </span>
                  )}
                </label>
                <CustomSelect
                  size="sm"
                  value={modelName}
                  onChange={(val) => setModelName(val)}
                  options={providerModels.map((m) => ({
                    value: m.id,
                    label: m.label,
                    badge: m.badge,
                  }))}
                  ariaLabel="AI Model"
                />
              </div>
            </div>

            {!isSelectedProviderConfigured && (
              <div className="p-3 bg-amber-50/90 border border-amber-200/90 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between text-xs text-amber-900 gap-2">
                <div className="flex items-center space-x-2">
                  <Key className="w-4 h-4 text-amber-600 flex-shrink-0" />
                  <span>
                    <strong>{AI_PROVIDER_CATALOG.find((p) => p.id === provider)?.name}</strong> requires your own API key. You can add one in Profile &rarr; AI API Keys, or deploy using Gemini for free cloud generation.
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => navigate('/profile')}
                  className="self-end sm:self-auto px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-xl font-bold whitespace-nowrap text-[11px] shadow-sm transition"
                >
                  Add Key
                </button>
              </div>
            )}
          </div>

          {/* Cohort Groups */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Target Cohort Groups (Multi-select)
            </label>
            <div className="flex flex-wrap gap-2">
              {groups.map((g) => {
                const isSelected = selectedGroupIds.includes(g.id);
                return (
                  <button
                    type="button"
                    key={g.id}
                    onClick={() => toggleGroup(g.id)}
                    className={`py-2 px-3.5 rounded-xl border text-xs font-semibold transition ${
                      isSelected
                        ? 'border-accent bg-sky-50 text-sky-800'
                        : 'border-slate-200 hover:border-slate-300 text-slate-600'
                    }`}
                  >
                    {g.name} ({g.studentCount})
                  </button>
                );
              })}
            </div>
          </div>

          {/* CEFR Level */}
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

          {/* Topic & Domain Selection via unified TopicSelector */}
          <TopicSelector
            cefrLevel={cefrLevel}
            topic={grammarTopic}
            onTopicChange={(t) => setGrammarTopic(t)}
            domain={domain}
            onDomainChange={(d) => setDomain(d)}
            catalog={catalog}
            showDomain={true}
            size="sm"
          />

          {/* Eco Mode (Token Saver) Toggle */}
          <div className={`p-4 rounded-2xl border transition ${
            ecoMode
              ? 'bg-emerald-50/70 border-emerald-200'
              : 'bg-slate-50/80 border-slate-200/80'
          } flex items-center justify-between`}>
            <div className="flex items-center space-x-3">
              <div className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 transition ${
                ecoMode ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-500'
              }`}>
                <Leaf className="w-4 h-4" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Eco Mode (Token Saver)
                  </h4>
                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                    ecoMode ? 'bg-emerald-200 text-emerald-900' : 'bg-slate-200 text-slate-600'
                  }`}>
                    {ecoMode ? 'ACTIVE' : 'OFF'}
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  {ecoMode
                    ? 'Omit verbose grammar rules & vocabulary lists from prompt to accelerate generation and conserve quota.'
                    : 'Inject detailed curriculum rules and target vocabulary into the assignment generation prompt.'}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setEcoMode(!ecoMode)}
              className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                ecoMode ? 'bg-emerald-600' : 'bg-slate-300'
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                  ecoMode ? 'translate-x-5' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Curriculum Context (Rules & Vocabulary) Expandable Panel */}
          <div className={`rounded-2xl border transition overflow-hidden ${
            ecoMode ? 'border-slate-200 bg-slate-50/30 opacity-75' : 'border-slate-200/90 bg-slate-50/50'
          }`}>
            <button
              type="button"
              onClick={() => setShowCurriculumPanel(!showCurriculumPanel)}
              className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-100/60 transition"
            >
              <div className="flex items-center space-x-2.5">
                <div className="w-7 h-7 rounded-lg bg-indigo-100 text-primary flex items-center justify-center">
                  <BookOpen className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                    Curriculum Rules & Vocabulary Context (Optional)
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    {ecoMode
                      ? '🌱 Eco Mode is active: curriculum context will be skipped during generation'
                      : uploadedRuleFile || uploadedVocabFile || customRule || customVocabulary
                      ? '✓ Custom rules or vocabulary active'
                      : 'Attach lesson rules, target vocabulary, or upload reference files (.txt, .md, .json)'}
                  </p>
                </div>
              </div>
              <div className="flex items-center space-x-2 text-slate-400">
                {showCurriculumPanel ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
              </div>
            </button>

            {showCurriculumPanel && (
              <div className="p-4 sm:p-5 border-t border-slate-200/80 bg-white space-y-4 animate-in fade-in duration-200">
                <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-slate-100">
                  <span className="text-xs text-slate-600 font-medium">
                    Enforce exact target rules and vocabulary in the assignment:
                  </span>
                  <button
                    type="button"
                    onClick={handleLoadReference}
                    disabled={isLoadingRef}
                    className="px-3 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition flex items-center space-x-1.5 disabled:opacity-50"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>{isLoadingRef ? 'Loading...' : '💡 Auto-Fill Canonical Reference'}</span>
                  </button>
                </div>

                {refNotice && (
                  <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center space-x-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                    <span>{refNotice}</span>
                  </div>
                )}

                {/* Target Grammar Rule Section */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-slate-700">
                      1. Target Grammar Rule / Structural Guide
                    </label>
                    <div className="flex items-center space-x-2">
                      <input
                        ref={ruleFileInputRef}
                        type="file"
                        accept=".txt,.md,.json"
                        className="hidden"
                        onChange={(e) => handleFileUpload(e, 'RULE')}
                      />
                      <button
                        type="button"
                        onClick={() => ruleFileInputRef.current?.click()}
                        disabled={isUploadingRule}
                        className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1 disabled:opacity-50"
                      >
                        <UploadCloud className="w-3.5 h-3.5" />
                        <span>{isUploadingRule ? 'Uploading...' : 'Upload Rule File (.md, .txt)'}</span>
                      </button>
                    </div>
                  </div>

                  {uploadedRuleFile && (
                    <div className="p-2.5 rounded-xl bg-indigo-50/80 border border-indigo-200 flex items-center justify-between text-xs">
                      <div className="flex items-center space-x-2 truncate">
                        <FileText className="w-4 h-4 text-primary flex-shrink-0" />
                        <span className="font-semibold text-slate-800 truncate">{uploadedRuleFile.fileName}</span>
                        <span className="text-slate-500 font-mono text-[10px]">
                          ({(uploadedRuleFile.fileSize / 1024).toFixed(1)} KB)
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setUploadedRuleFile(null)}
                        className="text-slate-400 hover:text-rose-500 transition ml-2"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}

                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1">
                    {savedRules.length > 0 ? (
                      <select
                        onChange={(e) => {
                          const found = savedRules.find((r) => r.id === e.target.value);
                          if (found) setCustomRule(found.content);
                        }}
                        defaultValue=""
                        className="text-xs px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 w-full sm:w-auto"
                      >
                        <option value="" disabled>Load saved rule from library ({savedRules.length})...</option>
                        {savedRules.map((r) => (
                          <option key={r.id} value={r.id}>{r.title}</option>
                        ))}
                      </select>
                    ) : <div />}
                    {customRule.trim() && (
                      <button
                        type="button"
                        onClick={handleSaveRuleToLibrary}
                        disabled={isSavingRule}
                        className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1"
                      >
                        <Bookmark className="w-3.5 h-3.5" />
                        <span>{isSavingRule ? 'Saving...' : 'Save Rule to Library'}</span>
                      </button>
                    )}
                  </div>

                  <textarea
                    value={customRule}
                    onChange={(e) => setCustomRule(e.target.value)}
                    rows={3}
                    placeholder="e.g. Focus on Inversion after negative adverbials..."
                    className="w-full p-3 rounded-xl border border-slate-200 bg-slate-50/40 text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-mono leading-relaxed"
                  />
                </div>

                {/* Target Vocabulary Section */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-slate-700">
                      2. Target Vocabulary / Lexicon List
                    </label>
                    <div className="flex items-center space-x-2">
                      <input
                        ref={vocabFileInputRef}
                        type="file"
                        accept=".txt,.json,.csv"
                        className="hidden"
                        onChange={(e) => handleFileUpload(e, 'VOCABULARY')}
                      />
                      <button
                        type="button"
                        onClick={() => vocabFileInputRef.current?.click()}
                        disabled={isUploadingVocab}
                        className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1 disabled:opacity-50"
                      >
                        <UploadCloud className="w-3.5 h-3.5" />
                        <span>{isUploadingVocab ? 'Uploading...' : 'Upload Vocab File (.json, .csv)'}</span>
                      </button>
                    </div>
                  </div>

                  {uploadedVocabFile && (
                    <div className="p-2.5 rounded-xl bg-indigo-50/80 border border-indigo-200 flex items-center justify-between text-xs">
                      <div className="flex items-center space-x-2 truncate">
                        <FileText className="w-4 h-4 text-primary flex-shrink-0" />
                        <span className="font-semibold text-slate-800 truncate">{uploadedVocabFile.fileName}</span>
                        <span className="text-slate-500 font-mono text-[10px]">
                          ({(uploadedVocabFile.fileSize / 1024).toFixed(1)} KB)
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setUploadedVocabFile(null)}
                        className="text-slate-400 hover:text-rose-500 transition ml-2"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}

                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1">
                    {savedVocabSets.length > 0 ? (
                      <select
                        onChange={(e) => {
                          const found = savedVocabSets.find((v) => v.id === e.target.value);
                          if (found) setCustomVocabulary(found.content);
                        }}
                        defaultValue=""
                        className="text-xs px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 w-full sm:w-auto"
                      >
                        <option value="" disabled>Load saved vocabulary ({savedVocabSets.length})...</option>
                        {savedVocabSets.map((v) => (
                          <option key={v.id} value={v.id}>{v.title}</option>
                        ))}
                      </select>
                    ) : <div />}
                    {customVocabulary.trim() && (
                      <button
                        type="button"
                        onClick={handleSaveVocabToLibrary}
                        disabled={isSavingVocab}
                        className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1"
                      >
                        <Bookmark className="w-3.5 h-3.5" />
                        <span>{isSavingVocab ? 'Saving...' : 'Save Vocabulary to Library'}</span>
                      </button>
                    )}
                  </div>

                  <textarea
                    value={customVocabulary}
                    onChange={(e) => setCustomVocabulary(e.target.value)}
                    rows={2}
                    placeholder="e.g. substantiate, empirical, paradigm, ubiquitous, resilient..."
                    className="w-full p-3 rounded-xl border border-slate-200 bg-slate-50/40 text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-mono leading-relaxed"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Task Type */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Task Type
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2">
              {(
                [
                  { type: 'MCQ', label: 'MCQ' },
                  { type: 'GAP_FILL', label: 'Gap Fill' },
                  { type: 'OPEN_BRACKETS', label: 'Open Brackets' },
                  { type: 'REWRITE', label: 'Rewrite' },
                  { type: 'ESSAY', label: 'Essay' },
                ] as { type: TaskType; label: string }[]
              ).map((item) => (
                <button
                  type="button"
                  key={item.type}
                  onClick={() => setTaskType(item.type)}
                  className={`py-2 px-2.5 rounded-xl border text-xs font-semibold text-center transition ${
                    taskType === item.type
                      ? 'border-primary bg-indigo-50 text-primary'
                      : 'border-slate-200 text-slate-600 hover:border-slate-300'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          {/* Due Date & Attempt Limit */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                Submission Due Date (Optional)
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
                      ? 'border-primary bg-indigo-50 text-primary shadow-sm'
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
                      ? 'border-emerald-600 bg-emerald-50 text-emerald-700 shadow-sm'
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
          <div className="p-4 rounded-2xl bg-indigo-50/40 border border-indigo-100/80 space-y-2">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Total Assignment Points / Max Score
              </label>
              <span className="text-xs font-mono font-bold text-primary bg-white px-2 py-0.5 rounded-lg border border-indigo-100 shadow-sm">
                {totalPoints} pts
              </span>
            </div>
            <div className="flex items-center space-x-3">
              <div className="relative w-36">
                <input
                  type="number"
                  min="1"
                  max="1000"
                  aria-label="Total Assignment Points"
                  value={totalPoints}
                  onChange={(e) => {
                    const val = Math.max(1, parseInt(e.target.value) || 100);
                    setTotalPoints(val);
                    if (previewTask) {
                      setPreviewTask({ ...previewTask, totalPoints: val });
                    }
                  }}
                  className="w-full pl-3 pr-9 py-2 rounded-xl border border-indigo-200 bg-white text-xs font-black text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400 pointer-events-none">
                  pts
                </span>
              </div>
              <p className="text-[11px] text-slate-500 leading-snug">
                Overall maximum score for this homework. Automatically distributed across questions during preview.
              </p>
            </div>
          </div>

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
            </button>

            <button
              type="button"
              onClick={handleSaveTemplate}
              disabled={isLoading}
              className="flex items-center space-x-1.5 py-2.5 px-4 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold transition"
            >
              <Bookmark className="w-4 h-4" />
              <span>Save Template</span>
            </button>

            <button
              type="button"
              onClick={handleDeploy}
              disabled={isLoading}
              className="flex-1 flex items-center justify-center space-x-2 py-3 px-5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
            >
              <Send className="w-4 h-4" />
              <span>Deploy to Students</span>
            </button>
          </div>
        </div>

        {/* Live Preview Column */}
        <div className="space-y-4">
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
                    <div className="flex items-center space-x-1">
                      <input
                        type="number"
                        min="1"
                        max="1000"
                        aria-label="Preview Total Points"
                        value={previewTask.totalPoints ?? totalPoints}
                        onChange={(e) => handleUpdateTotalPoints(parseInt(e.target.value) || 100)}
                        className="w-20 px-2.5 py-1 rounded-xl border border-indigo-300 bg-white text-xs font-black text-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                      />
                      <span className="text-xs font-bold text-slate-500">pts</span>
                    </div>
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
                            <span>{isMatching ? '✓' : '⚠️'}</span>
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
                <Sparkles className="w-4 h-4 text-primary shrink-0 mt-0.5" />
                <p>
                  <span className="font-bold">Interactive Editor:</span> You can edit instructions, question prompts, option choices, question point prices, and switch correct answers before deploying.
                </p>
              </div>

              {/* Editable Instructions */}
              <div>
                <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
                  Exercise Instructions / Reading Passage
                </label>
                <textarea
                  rows={3}
                  value={previewTask.content}
                  onChange={(e) => handleUpdateInstruction(e.target.value)}
                  placeholder="Enter exercise instructions or reading context here..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs text-slate-700 font-medium focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition leading-relaxed"
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
                        className="rounded-2xl border border-slate-200 bg-slate-50/50 p-4 space-y-3.5 transition hover:border-slate-300"
                      >
                        {/* Question Card Header with Question Points Price */}
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            <span className="w-6 h-6 rounded-lg bg-primary/10 text-primary text-xs font-black flex items-center justify-center">
                              {qIdx + 1}
                            </span>
                            <span className="text-xs font-bold text-slate-700">
                              Question {qIdx + 1}
                            </span>
                          </div>

                          <div className="flex items-center space-x-2">
                            {/* Question Point Price Input */}
                            <div className="flex items-center space-x-1.5 px-2.5 py-1 rounded-xl bg-white border border-slate-200 shadow-xs">
                              <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                                Points:
                              </span>
                              <input
                                type="number"
                                min="1"
                                max="500"
                                aria-label={`Question ${qIdx + 1} Points`}
                                value={q.points ?? 20}
                                onChange={(e) =>
                                  handleUpdateQuestionPoints(qIdx, parseInt(e.target.value) || 1)
                                }
                                className="w-14 px-1 py-0.5 rounded text-xs font-black text-primary text-center focus:outline-none focus:ring-1 focus:ring-primary border-b border-indigo-200"
                              />
                              <span className="text-[11px] font-semibold text-slate-400">pts</span>
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

                        {/* Question Text */}
                        <div>
                          <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                            Prompt / Sentence
                          </label>
                          <textarea
                            rows={2}
                            value={q.questionText}
                            onChange={(e) =>
                              handleUpdateQuestion(qIdx, { questionText: e.target.value })
                            }
                            placeholder="Enter the question sentence or prompt..."
                            className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                          />
                        </div>

                        {/* MCQ Options vs Open Answers */}
                        {previewTask.type === 'MCQ' ? (
                          <div className="space-y-2">
                            <div className="flex items-center justify-between">
                              <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                                Answer Choices (Select the correct option)
                              </label>
                              <button
                                type="button"
                                onClick={() => handleAddOption(qIdx)}
                                className="text-[11px] font-bold text-primary hover:underline inline-flex items-center space-x-1"
                              >
                                <Plus className="w-3 h-3" />
                                <span>Add Choice</span>
                              </button>
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

                                    <input
                                      type="text"
                                      value={opt}
                                      onChange={(e) =>
                                        handleOptionChange(qIdx, optIdx, e.target.value)
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
                          <div>
                            <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                              Target Correct Answer Key
                            </label>
                            <input
                              type="text"
                              value={q.correctAnswer}
                              onChange={(e) => handleSetCorrectAnswer(qIdx, e.target.value)}
                              placeholder="e.g. have visited / had gone"
                              className="w-full px-3 py-2 rounded-xl border border-emerald-300 bg-emerald-50/20 text-emerald-950 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-emerald-500/20 focus:border-emerald-500 transition"
                            />
                          </div>
                        )}

                        {/* Grammar Rule / Hint */}
                        <div>
                          <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                            Grammar Rule / Explanatory Hint
                          </label>
                          <input
                            type="text"
                            value={q.grammarRule || ''}
                            onChange={(e) =>
                              handleUpdateQuestion(qIdx, { grammarRule: e.target.value })
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
                </button>

                <button
                  type="button"
                  onClick={handleDeploy}
                  disabled={isLoading}
                  className="flex-1 flex items-center justify-center space-x-2 py-2.5 px-5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
                >
                  <Send className="w-4 h-4" />
                  <span>Deploy Edited Task</span>
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

      {/* Concurrent Generation Transfer Modal */}
      {concurrentConflict && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-2xl border border-amber-200/80 max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-150">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-200 text-amber-600 flex items-center justify-center mx-auto shadow-sm">
              <Smartphone className="w-6 h-6" />
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-base font-bold text-slate-900">
                Генерация на другом устройстве
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                На другом вашем устройстве прямо сейчас выполняется генерация задания. 
                Вы можете перенести управление генерацией на это устройство и сразу продолжить.
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
                <span>{isTakingOver ? 'Перенос...' : 'Переключить на это устройство'}</span>
              </button>
              <button
                type="button"
                onClick={() => setConcurrentConflict(false)}
                className="py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition"
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
