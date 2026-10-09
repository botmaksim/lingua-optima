/**
 * @file TeacherDashboard.tsx
 * @brief Educator central hub displaying cohort groups, enrolled students, and grading controls.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Sparkles, CheckCircle, FileText, ArrowRight, TrendingUp } from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { Group } from '../../types/group';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Teacher command center showing cohort statistics, quick actions, and group management.
 * @return JSX teacher dashboard element.
 */
export const TeacherDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [groups, setGroups] = useState<Group[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    groupApi
      .getGroups()
      .then((data) => setGroups(data))
      .catch((err) => console.error('Failed to load groups:', err))
      .finally(() => setIsLoading(false));
  }, []);

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading educator overview..." />;
  }

  const totalStudents = groups.reduce((acc, g) => acc + g.studentCount, 0);

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      <div className="bg-gradient-to-r from-sky-600 to-indigo-700 rounded-3xl p-6 sm:p-8 text-white shadow-xl shadow-sky-100 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight mb-2">
            Educator Command Center
          </h1>
          <p className="text-sky-100 text-sm max-w-xl">
            Manage your student cohorts, generate targeted curriculum tasks with AI, and review student homework submissions.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          <button
            onClick={() => navigate('/teacher/groups')}
            className="flex-1 md:flex-initial flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl bg-white text-sky-700 font-bold hover:bg-sky-50 transition shadow-md"
          >
            <Users className="w-4 h-4" />
            <span>Create Group</span>
          </button>
          <button
            onClick={() => navigate('/teacher/configure')}
            className="flex-1 md:flex-initial flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-medium transition backdrop-blur-sm"
          >
            <Sparkles className="w-4 h-4" />
            <span>Configure Task</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-sky-50 text-accent flex items-center justify-center">
            <Users className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{groups.length}</div>
            <p className="text-xs text-slate-500 font-medium">Active Cohort Groups</p>
          </div>
        </div>

        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center">
            <TrendingUp className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{totalStudents}</div>
            <p className="text-xs text-slate-500 font-medium">Enrolled Students</p>
          </div>
        </div>

        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <CheckCircle className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">AI Scoring</div>
            <p className="text-xs text-slate-500 font-medium">Auto-Evaluation Active</p>
          </div>
        </div>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900 flex items-center space-x-2">
            <Users className="w-5 h-5 text-accent" />
            <span>Your Student Groups</span>
          </h2>
          <button
            onClick={() => navigate('/teacher/groups')}
            className="text-xs font-semibold text-accent hover:underline flex items-center space-x-1"
          >
            <span>Manage All</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {groups.length === 0 ? (
          <div className="bg-white rounded-3xl p-10 text-center border border-dashed border-slate-200">
            <p className="text-sm text-slate-500 mb-4">No student groups created yet.</p>
            <button
              onClick={() => navigate('/teacher/groups')}
              className="px-4 py-2 bg-primary text-white text-xs font-bold rounded-xl"
            >
              Create First Group
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {groups.map((group) => (
              <div
                key={group.id}
                className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm hover:shadow-md transition space-y-4"
              >
                <div>
                  <h3 className="text-base font-bold text-slate-900">{group.name}</h3>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Created on {new Date(group.createdAt).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-slate-50 text-xs">
                  <span className="text-slate-500 font-medium">{group.studentCount} Students</span>
                  {group.avgScore != null && (
                    <span className="font-bold text-primary font-mono">
                      Avg Score: {group.avgScore}%
                    </span>
                  )}
                </div>

                <div className="flex space-x-2 pt-2">
                  <button
                    onClick={() => navigate('/teacher/groups')}
                    className="flex-1 py-2 px-3 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-semibold transition"
                  >
                    View Students
                  </button>
                  <button
                    onClick={() => navigate('/teacher/export')}
                    className="py-2 px-3 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 text-xs transition"
                    title="Export Group Report"
                  >
                    <FileText className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
