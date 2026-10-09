/**
 * @file ConfigureTask.tsx
 * @brief Educator task generation, parameter configuration, preview, and cohort deployment interface.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Send, Bookmark, Eye, CheckCircle2 } from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { taskApi } from '../../api/taskApi';
import { Group } from '../../types/group';
import { Task, TaskType, DifficultyLevel } from '../../types/task';
import { CefrLevel } from '../../types/user';
import { CefrBadge } from '../common/CefrBadge';

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
  }, []);

  const toggleGroup = (id: string) => {
    setSelectedGroupIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id]
    );
  };

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
      });
      setPreviewTask(task);
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to preview task.');
    } finally {
      setIsLoading(false);
    }
  };

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
      });
      setStatusMessage('Template saved to your curriculum catalog!');
    } catch (err: any) {
      setStatusMessage(err.response?.data?.message || 'Failed to save template.');
    } finally {
      setIsLoading(false);
    }
  };

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
        {/* Configuration Form (2 cols) */}
        <div className="lg:col-span-2 bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
          {/* Target Group Selector */}
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

          {/* Grammar Topic & Domain */}
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

          {/* Due date picker */}
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

          {/* Actions: [Generate Preview] [Save as Template] [Deploy to Students] */}
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

        {/* Preview Panel (1 col) */}
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
                {previewTask.content}
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
