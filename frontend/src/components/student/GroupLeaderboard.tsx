import React, { useState, useEffect } from 'react';
import { Trophy, Flame, Shield, Users } from 'lucide-react';
import { leaderboardApi } from '../../api/leaderboardApi';
import { groupApi } from '../../api/groupApi';
import { LeaderboardEntry } from '../../types/leaderboard';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const GroupLeaderboard: React.FC = () => {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [groupName, setGroupName] = useState<string>('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadGroupLeaderboard = async () => {
      try {
        const groups = await groupApi.getGroups();
        if (groups.length > 0) {
          const groupId = groups[0].id;
          const data = await leaderboardApi.getGroupLeaderboard(groupId);
          setEntries(data.entries || []);
          setGroupName(data.groupName || groups[0].name);
        }
      } catch (err) {
        console.error('Failed to load group leaderboard:', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadGroupLeaderboard();
  }, []);

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading group leaderboard..." />;
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <div className="flex items-center space-x-2">
          <Trophy className="w-6 h-6 text-amber-500" />
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Group Leaderboard
          </h1>
        </div>
        <p className="text-sm text-slate-500 mt-1">
          {groupName ? `Active rankings for group: ${groupName}` : 'Weekly group performance'}
        </p>
      </div>

      {/* Strict Architectural Guarantee Notice */}
      <div className="p-4 rounded-2xl bg-slate-100/70 border border-slate-200 text-xs text-slate-600 flex items-start space-x-3">
        <Shield className="w-4 h-4 text-slate-500 flex-shrink-0 mt-0.5" />
        <p>
          <span className="font-bold">Group-Only Leaderboard: </span>
          Global leaderboards are intentionally disabled. You only compete within your assigned teacher-led study group, and display aliases protect individual student privacy.
        </p>
      </div>

      {/* Leaderboard Table / Card List */}
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        {entries.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            <Users className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-slate-800">No group scores recorded yet</h3>
            <p className="text-xs text-slate-500 mt-1">
              Join a teacher group and complete tasks to start earning weekly points.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {entries.map((entry) => {
              const isTop3 = entry.rank <= 3;
              const medalColors = {
                1: 'bg-amber-100 text-amber-800 border-amber-300',
                2: 'bg-slate-100 text-slate-800 border-slate-300',
                3: 'bg-amber-50 text-amber-900 border-amber-200',
              }[entry.rank] || 'bg-slate-50 text-slate-600 border-slate-200';

              return (
                <div
                  key={entry.userId}
                  className="p-4 sm:p-5 flex items-center justify-between hover:bg-slate-50/60 transition"
                >
                  <div className="flex items-center space-x-4">
                    {/* Rank badge */}
                    <div
                      className={`w-9 h-9 rounded-xl border flex items-center justify-center font-black text-sm font-mono ${medalColors}`}
                    >
                      {entry.rank}
                    </div>

                    <div>
                      <h3 className="text-sm font-bold text-slate-800 flex items-center space-x-2">
                        <span>{entry.alias}</span>
                        {isTop3 && <Trophy className="w-3.5 h-3.5 text-amber-500" />}
                      </h3>
                      <div className="flex items-center space-x-2 text-xs text-slate-400 mt-0.5">
                        <Flame className="w-3.5 h-3.5 text-amber-500" />
                        <span>{entry.streakCount} day streak</span>
                      </div>
                    </div>
                  </div>

                  {/* Score */}
                  <div className="text-right">
                    <div className="text-base font-black text-primary font-mono">
                      {Math.round(entry.weeklyScore)}
                    </div>
                    <span className="text-[10px] text-slate-400 font-bold uppercase">
                      Weekly Pts
                    </span>
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
