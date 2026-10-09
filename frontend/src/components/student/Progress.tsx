/**
 * @file Progress.tsx
 * @brief Student progress analytics and mastery tracking across CEFR grammar syllabus topics.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BarChart3, Sparkles, TrendingUp, AlertCircle, CheckCircle2 } from 'lucide-react';
import { progressApi } from '../../api/progressApi';
import { ProgressRecord } from '../../types/progress';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Renders student diagnostic analytics, error frequencies, and per-topic mastery metrics.
 * @return JSX student progress view.
 */
export const Progress: React.FC = () => {
  const navigate = useNavigate();
  const [records, setRecords] = useState<ProgressRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    progressApi
      .getMyProgress()
      .then((data) => setRecords(data))
      .catch((err) => console.error('Failed to load progress records:', err))
      .finally(() => setIsLoading(false));
  }, []);

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading your grammar mastery analytics..." />;
  }

  const averageMastery =
    records.length === 0
      ? 0
      : Math.round(
          (records.reduce((acc, r) => acc + r.masteryScore, 0) / records.length) * 100
        );

  const weakTopics = records.filter((r) => r.masteryScore < 0.6);
  const masteredTopics = records.filter((r) => r.masteryScore >= 0.85);

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Grammar Mastery & Analytics
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Track your CEFR curriculum mastery, error rates, and automated learning diagnostics.
          </p>
        </div>

        <button
          onClick={() => navigate('/student/generate')}
          className="flex items-center space-x-2 py-3 px-5 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100"
        >
          <Sparkles className="w-4 h-4" />
          <span>Generate Targeted Task</span>
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center">
            <TrendingUp className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{averageMastery}%</div>
            <p className="text-xs text-slate-500 font-medium">Overall Average Mastery</p>
          </div>
        </div>

        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <CheckCircle2 className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{masteredTopics.length} Topics</div>
            <p className="text-xs text-slate-500 font-medium">Mastered (&ge;85%)</p>
          </div>
        </div>

        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-rose-50 text-rose-500 flex items-center justify-center">
            <AlertCircle className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{weakTopics.length} Topics</div>
            <p className="text-xs text-slate-500 font-medium">Needing Revision (&lt;60%)</p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-900 flex items-center space-x-2">
            <BarChart3 className="w-5 h-5 text-primary" />
            <span>Curriculum Topics Breakdown</span>
          </h2>
          <span className="text-xs font-semibold text-slate-500">{records.length} Topics tracked</span>
        </div>

        {records.length === 0 ? (
          <div className="text-center py-12 text-slate-500 text-sm">
            No topic records recorded yet. Complete exercises to generate insights.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-400 uppercase text-[11px] font-bold tracking-wider">
                <tr>
                  <th className="px-6 py-3.5">Grammar Topic</th>
                  <th className="px-6 py-3.5">Total Attempts</th>
                  <th className="px-6 py-3.5">Errors</th>
                  <th className="px-6 py-3.5">Mastery</th>
                  <th className="px-6 py-3.5 text-right">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {records.map((r) => {
                  const masteryPct = Math.round(r.masteryScore * 100);
                  const isWeak = r.masteryScore < 0.6;
                  const isMastered = r.masteryScore >= 0.85;

                  return (
                    <tr key={r.id} className="hover:bg-slate-50/50 transition">
                      <td className="px-6 py-4 font-bold text-slate-800">{r.grammarTopic}</td>
                      <td className="px-6 py-4 text-slate-600 font-mono">{r.totalAttempts}</td>
                      <td className="px-6 py-4 text-slate-600 font-mono">{r.errorCount}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center space-x-3">
                          <div className="w-24 bg-slate-100 rounded-full h-2">
                            <div
                              className={`h-2 rounded-full ${
                                isWeak
                                  ? 'bg-rose-500'
                                  : isMastered
                                  ? 'bg-emerald-500'
                                  : 'bg-primary'
                              }`}
                              style={{ width: `${masteryPct}%` }}
                            />
                          </div>
                          <span className="font-mono font-bold text-xs text-slate-700">
                            {masteryPct}%
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <span
                          className={`text-[10px] font-bold px-2.5 py-1 rounded-full uppercase ${
                            isWeak
                              ? 'bg-rose-50 text-rose-700'
                              : isMastered
                              ? 'bg-emerald-50 text-emerald-700'
                              : 'bg-indigo-50 text-primary'
                          }`}
                        >
                          {isWeak ? 'Weak' : isMastered ? 'Mastered' : 'In Progress'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
