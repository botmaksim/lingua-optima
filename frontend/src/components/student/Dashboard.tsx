import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Flame, Sparkles, Camera, ArrowRight, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { taskApi } from '../../api/taskApi';
import { progressApi } from '../../api/progressApi';
import { submissionApi } from '../../api/submissionApi';
import { Task } from '../../types/task';
import { ProgressRecord } from '../../types/progress';
import { SubmissionResult } from '../../types/submission';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { LevelUpModal } from './LevelUpModal';
import { formatDate } from '../../utils/formatDate';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tasks, setTasks] = useState<Task[]>([]);
  const [gaps, setGaps] = useState<ProgressRecord[]>([]);
  const [recentSubmissions, setRecentSubmissions] = useState<SubmissionResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        const [taskList, gapList, subList] = await Promise.all([
          taskApi.getTasks(),
          progressApi.getGaps(),
          submissionApi.getMySubmissions(),
        ]);
        setTasks(taskList);
        setGaps(gapList);
        setRecentSubmissions(subList);
      } catch (err) {
        console.error('Error loading dashboard:', err);
      } finally {
        setIsLoading(false);
      }
    };
    loadDashboardData();
  }, []);

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading your personalized dashboard..." />;
  }

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Level Up Suggestion Modal if applicable */}
      {user?.levelUpSuggestedAt && <LevelUpModal />}

      {/* Top Welcome & Actions Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-sky-600 rounded-3xl p-6 sm:p-8 text-white shadow-xl shadow-indigo-100 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center space-x-3 mb-2">
            <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
              Hello, {user?.fullName || 'Student'}!
            </h1>
            {user?.cefrLevel && <CefrBadge level={user.cefrLevel} size="md" className="bg-white/20 text-white border-white/30" />}
          </div>
          <p className="text-indigo-100 text-sm max-w-xl">
            Continue your adaptive learning journey. Practice targeted exercises or upload handwritten homework for instant AI feedback.
          </p>
        </div>

        {/* Quick Action Buttons */}
        <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
          <button
            onClick={() => navigate('/student/generate')}
            className="flex-1 md:flex-initial flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl bg-white text-primary font-bold hover:bg-indigo-50 transition shadow-md"
          >
            <Sparkles className="w-4 h-4" />
            <span>Generate New Task</span>
          </button>
          <button
            onClick={() => navigate('/student/ocr')}
            className="flex-1 md:flex-initial flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-medium transition backdrop-blur-sm"
          >
            <Camera className="w-4 h-4" />
            <span>Submit Homework Photo</span>
          </button>
        </div>
      </div>

      {/* Stat Cards Grid: Streak & CEFR Progress */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Streak card */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-amber-50 text-amber-500 flex items-center justify-center">
            <Flame className="w-8 h-8 fill-amber-500" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{user?.streakCount || 0} Days</div>
            <p className="text-xs text-slate-500 font-medium">Daily Learning Streak</p>
          </div>
        </div>

        {/* CEFR Level & Target */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Current CEFR</span>
            {user?.cefrLevel && <CefrBadge level={user.cefrLevel} size="sm" />}
          </div>
          <div className="text-xl font-bold text-slate-900 mb-1">
            Level {user?.cefrLevel || 'B1'} Mastered
          </div>
          <div className="w-full bg-slate-100 rounded-full h-2 mt-3">
            <div
              className="bg-primary h-2 rounded-full transition-all duration-500"
              style={{
                width: user?.cefrLevel === 'C1' ? '100%' : user?.cefrLevel === 'B2' ? '65%' : '35%',
              }}
            />
          </div>
        </div>

        {/* Weak Grammar Gaps Alert */}
        <div className="bg-white rounded-2xl p-6 border border-slate-100 shadow-sm flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-rose-50 text-rose-500 flex items-center justify-center">
            <AlertCircle className="w-8 h-8" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900">{gaps.length} Topics</div>
            <p className="text-xs text-slate-500 font-medium">Areas Needing Attention (&lt;60%)</p>
          </div>
        </div>
      </div>

      {/* Main Grid: Active Assignments & Grammar Gaps */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column (2 cols): Available & Active Tasks */}
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900 flex items-center space-x-2">
              <Clock className="w-5 h-5 text-indigo-500" />
              <span>Available Assignments</span>
            </h2>
            <button
              onClick={() => navigate('/student/my-units')}
              className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1"
            >
              <span>View All</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="space-y-3">
            {tasks.length === 0 ? (
              <div className="bg-white rounded-2xl p-8 text-center border border-dashed border-slate-200">
                <p className="text-sm text-slate-500 mb-4">No pending assignments at the moment.</p>
                <button
                  onClick={() => navigate('/student/generate')}
                  className="px-4 py-2 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary-hover transition"
                >
                  Generate Practice Task
                </button>
              </div>
            ) : (
              tasks.slice(0, 5).map((task) => (
                <div
                  key={task.id}
                  onClick={() => navigate(`/student/task/${task.id}`)}
                  className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-100 shadow-sm hover:border-indigo-200 hover:shadow-md transition cursor-pointer flex items-center justify-between group"
                >
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <CefrBadge level={task.cefrLevel} size="sm" />
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700">
                        {task.type}
                      </span>
                      <span className="text-xs text-slate-400">· {task.domain}</span>
                    </div>
                    <h3 className="text-sm font-bold text-slate-800 group-hover:text-primary transition">
                      {task.grammarTopic || 'General English Practice'}
                    </h3>
                  </div>

                  <div className="flex items-center space-x-2 text-primary font-medium text-xs">
                    <span className="hidden sm:inline">Start</span>
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition" />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column (1 col): Grammar Gaps & Quick Targeted Practice */}
        <div className="space-y-6">
          <h2 className="text-lg font-bold text-slate-900 flex items-center space-x-2">
            <AlertCircle className="w-5 h-5 text-rose-500" />
            <span>Grammar Gaps</span>
          </h2>

          <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm space-y-4">
            {gaps.length === 0 ? (
              <div className="text-center py-6">
                <CheckCircle className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
                <p className="text-sm font-semibold text-slate-800">All clear!</p>
                <p className="text-xs text-slate-500">You have no critical grammar gaps right now.</p>
              </div>
            ) : (
              gaps.slice(0, 4).map((gap) => (
                <div key={gap.id} className="space-y-1.5 pb-3 border-b border-slate-50 last:border-0 last:pb-0">
                  <div className="flex items-center justify-between text-xs font-medium">
                    <span className="text-slate-700">{gap.grammarTopic}</span>
                    <span className="font-bold text-rose-600">
                      {Math.round(gap.masteryScore * 100)}%
                    </span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-1.5">
                    <div
                      className="bg-rose-500 h-1.5 rounded-full"
                      style={{ width: `${Math.round(gap.masteryScore * 100)}%` }}
                    />
                  </div>
                </div>
              ))
            )}

            {recentSubmissions.length > 0 && (
              <div className="pt-4 border-t border-slate-100">
                <h3 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">Recent Submissions</h3>
                <div className="space-y-2">
                  {recentSubmissions.slice(0, 3).map((sub) => (
                    <div key={sub.id} className="text-xs flex justify-between items-center text-slate-600">
                      <span>Score: {Math.round(sub.effectiveScore ?? sub.score)}%</span>
                      <span className="font-semibold text-slate-900">{formatDate(sub.submittedAt)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {gaps.length > 0 && (
              <button
                onClick={() => navigate('/student/generate')}
                className="w-full py-2.5 px-4 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition flex items-center justify-center space-x-1"
              >
                <span>Practice Weak Topics</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
