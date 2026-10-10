/**
 * @file GroupLeaderboard.tsx
 * @brief Intra-group student leaderboard displaying progress, completion, and weekly rankings.
 */

import React, { useState, useEffect } from 'react';
import { Trophy, Flame, Shield, Users, CheckCircle2, Award, ChevronDown } from 'lucide-react';
import { leaderboardApi } from '../../api/leaderboardApi';
import { groupApi } from '../../api/groupApi';
import { LeaderboardEntry } from '../../types/leaderboard';
import { Group } from '../../types/group';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Renders the weekly points leaderboard scoped strictly within the student's assigned group.
 * @return JSX group leaderboard view.
 */
export const GroupLeaderboard: React.FC = () => {
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<string>('');
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [groupName, setGroupName] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingBoard, setIsLoadingBoard] = useState(false);

  useEffect(() => {
    const fetchGroups = async () => {
      try {
        const userGroups = await groupApi.getGroups();
        setGroups(userGroups);
        if (userGroups.length > 0) {
          setSelectedGroupId(userGroups[0].id);
          setGroupName(userGroups[0].name);
        }
      } catch (err) {
        console.error('Failed to load user groups:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchGroups();
  }, []);

  useEffect(() => {
    if (!selectedGroupId) return;

    const fetchBoard = async () => {
      setIsLoadingBoard(true);
      try {
        const data = await leaderboardApi.getGroupLeaderboard(selectedGroupId);
        setEntries(data.entries || []);
        const matched = groups.find((g) => g.id === selectedGroupId);
        setGroupName(matched?.name || data.groupName || 'Cohort');
      } catch (err) {
        console.error('Failed to load group leaderboard:', err);
        setEntries([]);
      } finally {
        setIsLoadingBoard(false);
      }
    };

    fetchBoard();
  }, [selectedGroupId, groups]);

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading group leaderboard..." />;
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header and Group Selector */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <Trophy className="w-6 h-6 text-amber-500" />
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              Group Leaderboard
            </h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            {groupName ? `Rankings and task progress for ${groupName}` : 'Weekly cohort standings'}
          </p>
        </div>

        {groups.length > 1 && (
          <div className="relative">
            <select
              value={selectedGroupId}
              onChange={(e) => setSelectedGroupId(e.target.value)}
              className="appearance-none bg-white border border-slate-200 text-slate-800 font-bold text-xs rounded-xl px-4 py-2.5 pr-8 focus:outline-none focus:ring-2 focus:ring-primary shadow-sm cursor-pointer"
            >
              {groups.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.name}
                </option>
              ))}
            </select>
            <ChevronDown className="w-4 h-4 text-slate-400 absolute right-2.5 top-3 pointer-events-none" />
          </div>
        )}
      </div>

      {/* Info & Privacy Banner */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div className="p-4 rounded-2xl bg-slate-100/80 border border-slate-200 text-xs text-slate-600 flex items-start space-x-3">
          <Shield className="w-4 h-4 text-slate-500 flex-shrink-0 mt-0.5" />
          <p>
            <span className="font-bold text-slate-800">Cohort Scoped: </span>
            Competition is strictly limited to your teacher-led study group. Global leaderboards across strangers do not exist.
          </p>
        </div>
        <div className="p-4 rounded-2xl bg-indigo-50/80 border border-indigo-100 text-xs text-indigo-700 flex items-start space-x-3">
          <Award className="w-4 h-4 text-indigo-500 flex-shrink-0 mt-0.5" />
          <p>
            <span className="font-bold text-indigo-900">Weekly Score: </span>
            Sum of all homework and practice evaluation points earned this week (Mon–Sun). Progress shows teacher tasks completed.
          </p>
        </div>
      </div>

      {/* Leaderboard Table / Cards */}
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        {isLoadingBoard ? (
          <div className="p-12 text-center">
            <LoadingSpinner size="md" message="Updating group rankings..." />
          </div>
        ) : entries.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <Users className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-slate-800">No group scores recorded yet</h3>
            <p className="text-xs text-slate-500 mt-1">
              Complete teacher assignments or practice tasks to earn points for your cohort.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {entries.map((entry) => {
              const isTop3 = entry.rank <= 3;
              const medalColors = {
                1: 'bg-amber-100 text-amber-800 border-amber-300 shadow-sm shadow-amber-100',
                2: 'bg-slate-100 text-slate-800 border-slate-300 shadow-sm shadow-slate-100',
                3: 'bg-amber-50 text-amber-900 border-amber-200 shadow-sm shadow-amber-50',
              }[entry.rank] || 'bg-slate-50 text-slate-600 border-slate-200';

              const displayName = entry.displayAlias || entry.alias || entry.fullName || 'Student';
              const showFullNameSubtext = entry.fullName && entry.fullName !== displayName;

              const totalTasks = entry.totalTasks ?? 0;
              const completedTasks = entry.completedTasks ?? 0;
              const completionRate = entry.completionRate ?? 0;
              const avgScore = entry.averageScore ?? 0;

              return (
                <div
                  key={entry.studentId || entry.userId || entry.rank}
                  className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50/60 transition"
                >
                  {/* Left: Rank & Student Identity */}
                  <div className="flex items-center space-x-3.5">
                    <div
                      className={`w-9 h-9 rounded-xl border flex items-center justify-center font-black text-sm font-mono flex-shrink-0 ${medalColors}`}
                    >
                      {entry.rank}
                    </div>

                    <div>
                      <div className="flex items-center space-x-2">
                        <h3 className="text-sm font-bold text-slate-800">
                          {displayName}
                        </h3>
                        {isTop3 && <Trophy className="w-3.5 h-3.5 text-amber-500 flex-shrink-0" />}
                        {entry.cefrLevel && (
                          <span className="text-[10px] font-black uppercase px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-700 border border-indigo-200 font-mono">
                            {entry.cefrLevel}
                          </span>
                        )}
                      </div>

                      {showFullNameSubtext && (
                        <p className="text-[11px] text-slate-400 font-medium">
                          {entry.fullName}
                        </p>
                      )}

                      <div className="flex items-center space-x-2 text-xs text-slate-400 mt-1">
                        <Flame className="w-3.5 h-3.5 text-amber-500" />
                        <span>{entry.streakCount} day streak</span>
                      </div>
                    </div>
                  </div>

                  {/* Right: Task Progress, Average Score, Weekly Points */}
                  <div className="flex items-center justify-between sm:justify-end space-x-4 sm:space-x-6 border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
                    {/* Tasks Completed */}
                    <div className="text-left sm:text-right">
                      <div className="flex items-center sm:justify-end space-x-1 text-xs font-bold text-slate-700">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        <span>
                          {completedTasks}/{totalTasks}
                        </span>
                      </div>
                      <div className="flex items-center space-x-1.5 mt-1">
                        <div className="w-16 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-emerald-500 rounded-full transition-all duration-300"
                            style={{ width: `${Math.min(100, completionRate)}%` }}
                          />
                        </div>
                        <span className="text-[10px] text-slate-400 font-semibold font-mono">
                          {completionRate}%
                        </span>
                      </div>
                      <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block mt-0.5">
                        Tasks Done
                      </span>
                    </div>

                    {/* Average Score */}
                    <div className="text-center sm:text-right">
                      <div className="text-xs font-black text-slate-800 font-mono">
                        {avgScore.toFixed(1)}%
                      </div>
                      <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block mt-1">
                        Avg Score
                      </span>
                    </div>

                    {/* Weekly Score Points */}
                    <div className="text-right pl-2 sm:pl-4 border-l border-slate-100">
                      <div className="text-base font-black text-primary font-mono">
                        {Math.round(entry.weeklyScore)}
                      </div>
                      <span className="text-[9px] text-slate-400 uppercase font-bold tracking-wider block">
                        Weekly Pts
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
