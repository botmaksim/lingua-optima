/**
 * @file ConfigureTask.tsx
 * @brief Educator task generation, parameter configuration, preview, custom curriculum context, and cohort deployment interface.
 */

import React, { useState, useEffect, useRef } from 'react';
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
} from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { taskApi } from '../../api/taskApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
import { Group } from '../../types/group';
import { Task, TaskType, DifficultyLevel, TopicsCatalogResponse } from '../../types/task';
import { CefrLevel } from '../../types/user';
import { CefrBadge } from '../common/CefrBadge';
import { CustomSelect } from '../common/CustomSelect';
import { sanitizeTaskContent } from '../../utils/textSanitizer';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  useProviderModels,
} from '../../constants/aiModels';

const DEFAULT_CEFR_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'Present Simple (to be & common verbs)',
    'Articles (a, an, the) & Demonstratives',
    'Basic Prepositions of Place & Time (in, at, on)',
    "Can / Can't for Ability & Permission",
    "Possessive Adjectives & Possessive 's",
    'Imperatives & Basic Question Formation',
  ],
  A2: [
    'Past Simple (Regular & Irregular Verbs)',
    "Future with 'Going to' vs 'Will'",
    'Comparative and Superlative Adjectives',
    'Countable vs Uncountable Nouns (some, any, much, many)',
    'Have to & Must (Basic Rules)',
    'Present Continuous for Future Arrangements',
  ],
  B1: [
    'Present Perfect vs Past Simple',
    'Past Continuous',
    'Conditionals (First & Second)',
    'Modal Verbs of Obligation',
    'Passive Voice (Basic)',
    'Relative Clauses (Defining)',
    'Used to & Would',
  ],
  B2: [
    'Third & Mixed Conditionals',
    'Passive Voice (Advanced & Causative)',
    'Reported Speech',
    'Wish & If Only Structures',
    'Modal Verbs of Deduction',
    'Inversion for Emphasis',
    'Participle Clauses',
  ],
  C1: [
    'Advanced Inversion & Fronting',
    'Subjunctive Mood',
    'Cleft Sentences',
    'Complex Gerunds & Infinitives',
    'Discourse Markers & Nuance',
    'Ellipsis & Substitution',
  ],
  C2: [
    'Stylistic Inversion & Rhetorical Fronting',
    'Subtle Modal Nuances & Speculative Stance',
    'Complex Cleft Constructions & Focalization',
    'Idiomatic Phrasal Collocations & Register Shifts',
    'Advanced Ellipsis, Substitution & Cohesive Ties',
    'Figurative Language & Lexical Precision',
  ],
};

const DEFAULT_MIXED_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'Present Simple vs Present Continuous in Daily Routines',
    'Articles, Plurals, and Demonstrative Pronouns',
    'Question Formation with To Be, Do/Does, and Can',
  ],
  A2: [
    'Past Simple vs Past Continuous Narrative Interruption',
    'Future Plans: Going to vs Present Continuous vs Will',
    'Comparatives, Superlatives, and As...As Equality',
  ],
  B1: [
    'Narrative Tenses: Past Simple, Continuous, and Perfect',
    'Mixed Modal Verbs: Obligation, Permission, and Advice',
    'Zero, First, and Second Conditionals with Unless',
  ],
  B2: [
    'Mixed Conditionals (Past Cause with Present Result)',
    'Advanced Passive and Causative Structures (Have/Get something done)',
    'Reported Speech Shifts with Reporting Verbs & Modals',
  ],
  C1: [
    'Negative Inversion and Cleft Sentences Combined',
    'Participle Clauses with Reduced Relatives & Adverbials',
    'Subjunctive Mood and Formulaic Mandative Expressions',
  ],
  C2: [
    'Stylistic Inversion, Clefting, and Focal Fronting',
    'Epistemic Stance, Subtle Modal Nuances, and Hedging',
    'Advanced Ellipsis, Substitution, and Cohesive Chaining',
  ],
};

/**
 * @brief Teacher component for configuring and deploying AI-generated assignments to student groups.
 * @return React component element.
 */
export const ConfigureTask: React.FC = () => {
  const navigate = useNavigate();

  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([]);
  const [catalog, setCatalog] = useState<TopicsCatalogResponse | null>(null);
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>('B1');
  const [grammarTopic, setGrammarTopic] = useState<string>('Passive Voice (Basic)');
  const [domain, setDomain] = useState<string>('Academic');
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const difficulty: DifficultyLevel = 'MEDIUM';
  const numberOfQuestions = 5;
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
  }, []);

  const currentLevelTopics =
    catalog?.topicsByLevel?.[cefrLevel] || DEFAULT_CEFR_TOPICS[cefrLevel] || [];
  const currentLevelMixedTopics =
    catalog?.mixedTopicsByLevel?.[cefrLevel] || DEFAULT_MIXED_TOPICS[cefrLevel] || [];
  const crossLevelTopics = catalog?.crossLevelTopics || [];

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
      setPreviewTask(task);
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to preview task.');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Event handler executing save template.
   */
  const handleSaveTemplate = async () => {
    setIsLoading(true);
    setStatusMessage(null);
    try {
      await taskApi.saveTemplate(buildTaskParams());
      setStatusMessage('Template saved to your curriculum catalog!');
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to save template.');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Event handler executing assignment deployment to selected groups.
   */
  const handleDeploy = async () => {
    if (selectedGroupIds.length === 0) {
      alert('Please select at least one cohort group to deploy this assignment.');
      return;
    }

    setIsLoading(true);
    setStatusMessage(null);

    try {
      const task = await taskApi.generateTask(buildTaskParams());
      await taskApi.assignTask(task.id, selectedGroupIds, dueDate || undefined, maxAttempts);

      setStatusMessage('Assignment deployed successfully to selected cohort groups!');
      setTimeout(() => navigate('/teacher/dashboard'), 1500);
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to deploy assignment.');
    } finally {
      setIsLoading(false);
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
                  options={AI_PROVIDER_CATALOG.map((p) => ({
                    value: p.id,
                    label: p.name,
                  }))}
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

          {/* Grammar Topic & Domain */}
          <div className="space-y-3">
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">
                  Grammar Topic / Subject
                </label>
                <div className="text-[11px] text-slate-500">
                  Select a syllabus preset or enter custom topic
                </div>
              </div>

              {/* Quick Select Preset Dropdown */}
              <CustomSelect
                size="sm"
                className="mb-2"
                value={grammarTopic}
                placeholder="💡 Pick from Syllabus Presets & Mixed Challenges..."
                onChange={(val) => {
                  if (val) setGrammarTopic(val);
                }}
                ariaLabel="Grammar Topic Preset"
                groups={[
                  {
                    label: `Core Syllabus (CEFR ${cefrLevel})`,
                    options: currentLevelTopics.map((t) => ({
                      value: t,
                      label: t,
                      badge: cefrLevel,
                    })),
                  },
                  ...(currentLevelMixedTopics.length > 0
                    ? [
                        {
                          label: `🔀 Mixed Challenges (CEFR ${cefrLevel})`,
                          options: currentLevelMixedTopics.map((t) => ({
                            value: t,
                            label: t,
                            badge: 'Mixed',
                          })),
                        },
                      ]
                    : []),
                  ...(crossLevelTopics.length > 0
                    ? [
                        {
                          label: '🌐 Cross-Level & Thematic Challenges',
                          options: crossLevelTopics.map((t) => ({
                            value: t,
                            label: t,
                            badge: 'Cross-Level',
                          })),
                        },
                      ]
                    : []),
                ]}
              />

              <input
                type="text"
                value={grammarTopic}
                onChange={(e) => setGrammarTopic(e.target.value)}
                placeholder="Enter custom topic or customize selected..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                Vocabulary Domain
              </label>
              <input
                type="text"
                value={domain}
                onChange={(e) => setDomain(e.target.value)}
                placeholder="e.g. Academic, Business, Science, Law..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>
          </div>

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
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {(['MCQ', 'GAP_FILL', 'REWRITE', 'ESSAY'] as TaskType[]).map((t) => (
                <button
                  type="button"
                  key={t}
                  onClick={() => setTaskType(t)}
                  className={`py-2 px-2.5 rounded-xl border text-xs font-semibold text-center transition ${
                    taskType === t
                      ? 'border-primary bg-indigo-50 text-primary'
                      : 'border-slate-200 text-slate-600 hover:border-slate-300'
                  }`}
                >
                  {t}
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
          <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
            Live Preview
          </h2>

          {previewTask ? (
            <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4">
              <div className="flex items-center space-x-2">
                <CefrBadge level={previewTask.cefrLevel} size="sm" />
                <span className="text-xs font-bold text-slate-700">{previewTask.type}</span>
              </div>
              <h3 className="text-sm font-bold text-slate-900">{previewTask.grammarTopic}</h3>
              <p className="text-xs text-slate-600 bg-slate-50 p-3 rounded-xl leading-relaxed whitespace-pre-wrap">
                {sanitizeTaskContent(previewTask.content, 'AI-generated task assignment instructions.')}
              </p>
            </div>
          ) : (
            <div className="bg-white rounded-3xl p-8 text-center text-slate-400 border border-dashed border-slate-200 text-xs">
              Click "Preview" to inspect the AI-generated questions before assigning to your students.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
