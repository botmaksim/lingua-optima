/**
 * @file MyUnits.tsx
 * @brief Student history view displaying past submissions, evaluations, and searchable completed units.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Search, Filter } from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { SubmissionResult } from '../../types/submission';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { CustomSelect } from '../common/CustomSelect';
import { formatDate } from '../../utils/formatDate';

/**
 * @brief Renders the history of student tasks and submissions with filtering and search.
 * @return JSX student units view.
 */
export const MyUnits: React.FC = () => {
  const navigate = useNavigate();
  const [submissions, setSubmissions] = useState<SubmissionResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');

  useEffect(() => {
    submissionApi
      .getMySubmissions()
      .then((data) => setSubmissions(data))
      .catch((err) => console.error('Failed to load submissions:', err))
      .finally(() => setIsLoading(false));
  }, []);

  const filtered = submissions.filter((sub) => {
    const matchesSearch =
      sub.originalText.toLowerCase().includes(searchQuery.toLowerCase()) ||
      sub.feedback.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesType = selectedType === 'ALL' || sub.submissionType === selectedType;
    return matchesSearch && matchesType;
  });

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading your completed units..." />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">My Units & History</h1>
        <p className="text-sm text-slate-500 mt-1">
          Review all your past exercises, essays, and OCR evaluations.
        </p>
      </div>

      <div className="bg-white rounded-3xl p-4 border border-slate-100 shadow-sm flex flex-col sm:flex-row items-center gap-3">
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by topic or text..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
          />
        </div>

        <div className="flex items-center space-x-2 w-full sm:w-52">
          <Filter className="w-4 h-4 text-slate-400 flex-shrink-0" />
          <CustomSelect
            size="sm"
            className="flex-1"
            value={selectedType}
            onChange={(val) => setSelectedType(val)}
            options={[
              { value: 'ALL', label: 'All Types' },
              { value: 'TEXT', label: 'Text & Grammar' },
              { value: 'IMAGE', label: 'OCR Homework' },
            ]}
            ariaLabel="Filter by submission type"
          />
        </div>
      </div>

      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 text-center border border-dashed border-slate-200">
            <BookOpen className="w-12 h-12 text-slate-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-slate-800">No practice units found</h3>
            <p className="text-xs text-slate-500 mt-1 mb-4">
              Complete your first exercise or submit homework to build your personal unit collection.
            </p>
            <button
              onClick={() => navigate('/student/generate')}
              className="py-2.5 px-5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover transition"
            >
              Start Practice
            </button>
          </div>
        ) : (
          filtered.map((sub) => {
            const effectiveScore = sub.effectiveScore ?? sub.score;
            return (
              <div
                key={sub.id}
                className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm hover:border-slate-200 transition flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4"
              >
                <div className="space-y-1.5 flex-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-50 text-primary">
                      {sub.submissionType}
                    </span>
                    <span className="text-xs text-slate-400">
                      {formatDate(sub.submittedAt)}
                    </span>
                    {sub.overrideScore != null && (
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-amber-50 text-amber-700">
                        Teacher Reviewed
                      </span>
                    )}
                  </div>
                  <p className="text-sm font-bold text-slate-800 line-clamp-1">
                    {sub.originalText.slice(0, 100)}...
                  </p>
                  <p className="text-xs text-slate-500 line-clamp-1">{sub.feedback}</p>
                </div>

                <div className="flex items-center space-x-4 self-end sm:self-center">
                  <div className="text-right">
                    <div className="text-xl font-black text-primary font-mono">
                      {Math.round(effectiveScore)}
                    </div>
                    <span className="text-[10px] text-slate-400 font-bold uppercase">Score</span>
                  </div>

                  <button
                    onClick={() => navigate(`/student/review/${sub.id}`)}
                    className="py-2 px-3.5 rounded-xl bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold transition flex items-center space-x-1"
                  >
                    <span>View Review</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
