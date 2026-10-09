/**
 * @file ConfigureTask.tsx
 * @brief Educator task generation, parameter configuration, preview, and cohort deployment interface.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Send, Bookmark, Eye, CheckCircle2, Cpu } from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { taskApi } from '../../api/taskApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
import { Group } from '../../types/group';
import { Task, TaskType, DifficultyLevel } from '../../types/task';
import { CefrLevel } from '../../types/user';
import { CefrBadge } from '../common/CefrBadge';
import { sanitizeTaskContent } from '../../utils/textSanitizer';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  getModelsForProvider,
} from '../../constants/aiModels';

/**
 * @brief Teacher component for configuring and deploying AI-generated assignments to student groups.
 * @return React component element.
 */
export const ConfigureTask: React.FC = () => {
  const navigate = useNavigate();

  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([]);
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>('B1');
  const [grammarTopic, setGrammarTopic] = useState<string>('Passive Voice (Basic)');
  const [domain, setDomain] = useState<string>('Academic');
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const difficulty: DifficultyLevel = 'MEDIUM';
  const numberOfQuestions = 5;
  const [dueDate, setDueDate] = useState<string>('');
  const [provider, setProvider] = useState<AIProviderType>('GEMINI');
  const [modelName, setModelName] = useState<string>(getDefaultModelForProvider('GEMINI'));
  const [savedKeys, setSavedKeys] = useState<ApiKeyItem[]>([]);

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
   * @brief Event handler or helper executing handle preview.
   */
  const handlePreview = async () => {
    setIsLoading(true);
    setStatusMessage(null);
    try {
      const task = await taskApi.previewTask({
        cefrLevel,
        grammarTopic,
        domain,
        taskType,
        difficulty,
        numberOfQuestions,
        provider,
        modelName,
      });
      setPreviewTask(task);
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to preview task.');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle save template.
   */
  const handleSaveTemplate = async () => {
    setIsLoading(true);
    setStatusMessage(null);
    try {
      await taskApi.saveTemplate({
        cefrLevel,
        grammarTopic,
        domain,
        taskType,
        difficulty,
        numberOfQuestions,
        provider,
        modelName,
      });
      setStatusMessage('Template saved to your curriculum catalog!');
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to save template.');
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle deploy.
   */
  const handleDeploy = async () => {
    if (selectedGroupIds.length === 0) {
      alert('Please select at least one cohort group to deploy this assignment.');
      return;
    }

    setIsLoading(true);
    setStatusMessage(null);

    try {
      const task = await taskApi.generateTask({
        cefrLevel,
        grammarTopic,
        domain,
        taskType,
        difficulty,
        numberOfQuestions,
        provider,
        modelName,
      });

      await taskApi.assignTask(task.id, selectedGroupIds, dueDate || undefined);

      setStatusMessage('Task successfully deployed to selected student cohorts!');
      setTimeout(() => navigate('/teacher'), 1500);
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to deploy task.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">
          Curriculum Task Designer
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Configure an AI-generated assignment, preview questions, and deploy directly to your student cohorts.
        </p>
      </div>

      {statusMessage && (
        <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-200 text-indigo-900 text-sm flex items-center space-x-2">
          <CheckCircle2 className="w-5 h-5 text-primary flex-shrink-0" />
          <span>{statusMessage}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
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
                <select
                  value={provider}
                  onChange={(e) => handleProviderChange(e.target.value as AIProviderType)}
                  className="w-full px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-900 font-medium text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                >
                  {AI_PROVIDER_CATALOG.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase mb-1">
                  AI Model
                </label>
                <select
                  value={modelName}
                  onChange={(e) => setModelName(e.target.value)}
                  className="w-full px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-slate-900 font-medium text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                >
                  {getModelsForProvider(provider).map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.label} ({m.badge})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

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

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              CEFR Level
            </label>
            <div className="grid grid-cols-3 gap-2">
              {(['B1', 'B2', 'C1'] as CefrLevel[]).map((level) => (
                <button
                  type="button"
                  key={level}
                  onClick={() => setCefrLevel(level)}
                  className={`py-2.5 rounded-xl border text-xs font-bold flex items-center justify-center space-x-2 transition ${
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

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                Grammar Topic
              </label>
              <input
                type="text"
                value={grammarTopic}
                onChange={(e) => setGrammarTopic(e.target.value)}
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
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>
          </div>

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
