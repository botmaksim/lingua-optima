/**
 * @file SubmissionsReview.tsx
 * @brief Educator submission grading, search, filtering, hide/archive toggles, pagination, detailed question breakdown, and feedback editing interface.
 */

import React, { useState, useEffect, useMemo } from 'react';
import {
  CheckCircle,
  CheckCircle2,
  XCircle,
  Edit3,
  ChevronDown,
  ChevronUp,
  X,
  User as UserIcon,
  Search,
  Eye,
  EyeOff,
  ChevronLeft,
  ChevronRight,
  Filter,
  RotateCcw,
  Users,
  BookOpen,
  Bot,
  Lightbulb,
  Save,
  Sparkles,
} from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { userApi } from '../../api/userApi';
import { groupApi } from '../../api/groupApi';
import { SubmissionResult } from '../../types/submission';
import { Group } from '../../types/group';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useNotificationStore } from '../../store/notificationStore';
import { formatDate } from '../../utils/formatDate';

/**
 * @brief Educator component reviewing student homework, adjusting scores, inspecting question options, and editing comments.
 * @return React component element.
 */
export const SubmissionsReview: React.FC = () => {
  const { addToast } = useNotificationStore();
  const [submissions, setSubmissions] = useState<SubmissionResult[]>([]);
  const [groups, setGroups] = useState<Group[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Score override modal state
  const [editingSub, setEditingSub] = useState<SubmissionResult | null>(null);
  const [newScore, setNewScore] = useState<number>(85);
  const [teacherComment, setTeacherComment] = useState<string>('');
  const [aiFeedbackDraft, setAiFeedbackDraft] = useState<string>('');
  const [isSubmittingOverride, setIsSubmittingOverride] = useState(false);

  // Inline comment & feedback editing states
  const [editingFeedbackId, setEditingFeedbackId] = useState<string | null>(null);
  const [feedbackDraft, setFeedbackDraft] = useState<string>('');

  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [commentDraft, setCommentDraft] = useState<string>('');
  const [isSavingInline, setIsSavingInline] = useState(false);

  // Student renaming state
  const [renamingStudent, setRenamingStudent] = useState<{ id: string; name: string } | null>(null);
  const [renameInput, setRenameInput] = useState<string>('');
  const [isSavingName, setIsSavingName] = useState(false);

  // Tab & Hide / Archive state
  const [activeTab, setActiveTab] = useState<'active' | 'hidden'>('active');
  const [hiddenIds, setHiddenIds] = useState<Set<string>>(() => {
    try {
      const raw = localStorage.getItem('lingua_hidden_submission_ids');
      return raw ? new Set(JSON.parse(raw)) : new Set();
    } catch {
      return new Set();
    }
  });

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [groupFilter, setGroupFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'AI_GRADED' | 'TEACHER_GRADED'>('ALL');
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'highest_score' | 'lowest_score'>('newest');

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    if (!editingSub) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setEditingSub(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [editingSub]);

  /**
   * @brief Loads educator submissions queue and teacher cohort groups simultaneously.
   */
  const loadData = async () => {
    try {
      const [subData, groupData] = await Promise.all([
        submissionApi.getTeacherSubmissions(),
        groupApi.getGroups().catch((err) => {
          console.warn('Failed to load educator cohort groups:', err);
          return [] as Group[];
        }),
      ]);
      setSubmissions(subData);
      setGroups(groupData);
    } catch (err) {
      console.error('Failed to load educator submissions queue:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  /**
   * @brief Lookup mapping each student ID to their enrolled cohort group names.
   */
  const studentGroupsMap = useMemo(() => {
    const map: Record<string, string[]> = {};
    groups.forEach((g) => {
      g.students?.forEach((s) => {
        if (!map[s.id]) {
          map[s.id] = [];
        }
        if (!map[s.id].includes(g.name)) {
          map[s.id].push(g.name);
        }
      });
    });
    return map;
  }, [groups]);

  /**
   * @brief Toggles expansion of a submission row and fetches enriched details if needed.
   * @param sub Target submission record.
   */
  const handleToggleExpand = async (sub: SubmissionResult) => {
    if (expandedId === sub.id) {
      setExpandedId(null);
      return;
    }

    setExpandedId(sub.id);

    // If items are not loaded yet, fetch single enriched submission
    if (!sub.items || sub.items.length === 0) {
      try {
        const enriched = await submissionApi.getSubmissionById(sub.id);
        setSubmissions((prev) =>
          prev.map((s) => (s.id === sub.id ? { ...s, ...enriched } : s))
        );
      } catch (err) {
        console.warn('Could not fetch enriched submission details:', err);
      }
    }
  };

  /**
   * @brief Toggles hidden/archived state for a given submission ID with localStorage persistence.
   * @param id Submission unique identifier.
   */
  const handleToggleHide = (id: string) => {
    setHiddenIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      try {
        localStorage.setItem('lingua_hidden_submission_ids', JSON.stringify([...next]));
      } catch (err) {
        console.warn('Failed to persist hidden submissions:', err);
      }
      return next;
    });
  };

  /**
   * @brief Restores all hidden submissions back to the active queue.
   */
  const handleUnhideAll = () => {
    setHiddenIds(new Set());
    try {
      localStorage.removeItem('lingua_hidden_submission_ids');
    } catch (err) {
      console.warn('Failed to clear hidden submissions:', err);
    }
    addToast({
      type: 'info',
      title: 'Submissions Restored',
      message: 'All hidden submissions have been restored to the active queue.',
    });
  };

  /**
   * @brief Opens rename modal for an enrolled student account.
   * @param studentId Student user ID.
   * @param currentName Existing student display name.
   */
  const handleOpenRename = (studentId: string, currentName: string) => {
    setRenamingStudent({ id: studentId, name: currentName });
    setRenameInput(currentName);
  };

  /**
   * @brief Saves updated student name across submissions and class roster.
   */
  const handleSaveRename = async () => {
    if (!renamingStudent || !renameInput.trim()) return;
    setIsSavingName(true);
    try {
      const updatedName = renameInput.trim();
      await userApi.updateStudentName(renamingStudent.id, updatedName);
      setSubmissions((prev) =>
        prev.map((s) => (s.studentId === renamingStudent.id ? { ...s, studentName: updatedName } : s))
      );
      addToast({
        type: 'success',
        title: 'Student Renamed',
        message: `Student account name updated to "${updatedName}".`,
      });
      setRenamingStudent(null);
    } catch (err: any) {
      console.error('Failed to rename student:', err);
      addToast({
        type: 'error',
        title: 'Rename Failed',
        message: err.response?.data?.message || 'Could not update student name.',
      });
    } finally {
      setIsSavingName(false);
    }
  };

  /**
   * @brief Opens score, feedback, and teacher comment override modal for a submission.
   * @param sub Target submission object.
   */
  const handleOpenOverride = (sub: SubmissionResult) => {
    setEditingSub(sub);
    setNewScore(sub.overrideScore ?? sub.score);
    setTeacherComment(
      sub.teacherComment && sub.teacherComment !== 'AI grade approved by teacher.'
        ? sub.teacherComment
        : sub.teacherComment || ''
    );
    setAiFeedbackDraft(sub.feedback || '');
  };

  /**
   * @brief Saves overridden score, AI feedback, and educator comments from modal.
   */
  const handleSaveOverride = async () => {
    if (!editingSub) return;
    setIsSubmittingOverride(true);
    try {
      const updated = await submissionApi.overrideScore(editingSub.id, {
        overrideScore: Number(newScore),
        teacherComment,
        feedback: aiFeedbackDraft,
      });

      setSubmissions((prev) =>
        prev.map((s) => (s.id === editingSub.id ? { ...s, ...updated } : s))
      );

      setEditingSub(null);
      addToast({
        type: 'success',
        title: 'Grade & Feedback Updated',
        message: 'Student score, AI comment, and teacher advice have been saved.',
      });
    } catch (err) {
      console.error('Failed to save score override:', err);
      addToast({
        type: 'error',
        title: 'Update Failed',
        message: 'Could not save the grade override. Please try again.',
      });
    } finally {
      setIsSubmittingOverride(false);
    }
  };

  /**
   * @brief Saves inline edit of AI diagnostic comment directly from expanded card.
   * @param sub Target submission.
   */
  const handleSaveInlineFeedback = async (sub: SubmissionResult) => {
    setIsSavingInline(true);
    try {
      const updated = await submissionApi.overrideScore(sub.id, {
        overrideScore: sub.overrideScore ?? sub.score,
        teacherComment: sub.teacherComment,
        feedback: feedbackDraft,
      });

      setSubmissions((prev) =>
        prev.map((s) => (s.id === sub.id ? { ...s, ...updated } : s))
      );
      setEditingFeedbackId(null);
      addToast({
        type: 'success',
        title: 'AI Comment Updated',
        message: 'AI diagnostic evaluation feedback has been updated.',
      });
    } catch (err) {
      console.error('Failed to update AI feedback:', err);
      addToast({
        type: 'error',
        title: 'Save Failed',
        message: 'Could not update AI diagnostic feedback.',
      });
    } finally {
      setIsSavingInline(false);
    }
  };

  /**
   * @brief Saves inline edit of teacher advice / comment directly from expanded card.
   * @param sub Target submission.
   */
  const handleSaveInlineComment = async (sub: SubmissionResult) => {
    setIsSavingInline(true);
    try {
      const updated = await submissionApi.overrideScore(sub.id, {
        overrideScore: sub.overrideScore ?? sub.score,
        teacherComment: commentDraft,
        feedback: sub.feedback,
      });

      setSubmissions((prev) =>
        prev.map((s) => (s.id === sub.id ? { ...s, ...updated } : s))
      );
      setEditingCommentId(null);
      addToast({
        type: 'success',
        title: 'Teacher Advice Saved',
        message: 'Personalized student advice has been saved.',
      });
    } catch (err) {
      console.error('Failed to update teacher comment:', err);
      addToast({
        type: 'error',
        title: 'Save Failed',
        message: 'Could not update teacher advice.',
      });
    } finally {
      setIsSavingInline(false);
    }
  };

  /**
   * @brief Approves AI generated grade without manual numerical adjustments.
   * @param sub Target submission object.
   */
  const handleApproveAiGrade = async (sub: SubmissionResult) => {
    try {
      const updated = await submissionApi.overrideScore(sub.id, {
        overrideScore: sub.overrideScore ?? sub.score,
        teacherComment: sub.teacherComment || 'AI grade approved by teacher.',
        feedback: sub.feedback,
      });
      addToast({
        type: 'success',
        title: 'Grade Approved',
        message: 'AI evaluation score confirmed for this submission.',
      });
      setSubmissions((prev) =>
        prev.map((s) => (s.id === sub.id ? { ...s, ...updated } : s))
      );
    } catch (err) {
      console.error('Failed to approve AI grade:', err);
      addToast({
        type: 'error',
        title: 'Approval Failed',
        message: 'Could not approve the AI grade. Please try again.',
      });
    }
  };

  // Counts for tabs
  const activeCount = useMemo(() => submissions.filter((s) => !hiddenIds.has(s.id)).length, [submissions, hiddenIds]);
  const hiddenCount = useMemo(() => submissions.filter((s) => hiddenIds.has(s.id)).length, [submissions, hiddenIds]);

  // Filtered and sorted submissions list
  const filteredSubmissions = useMemo(() => {
    return submissions
      .filter((sub) => {
        const isHidden = hiddenIds.has(sub.id);
        if (activeTab === 'active' && isHidden) return false;
        if (activeTab === 'hidden' && !isHidden) return false;

        // Group filter
        if (groupFilter !== 'ALL') {
          const cohorts = studentGroupsMap[sub.studentId] || [];
          if (!cohorts.includes(groupFilter)) return false;
        }

        // Status filter
        if (statusFilter === 'AI_GRADED' && sub.overrideScore != null) return false;
        if (statusFilter === 'TEACHER_GRADED' && sub.overrideScore == null) return false;

        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase().trim();
          const studentName = (sub.studentName || '').toLowerCase();
          const studentEmail = (sub.studentEmail || '').toLowerCase();
          const cohorts = (studentGroupsMap[sub.studentId] || []).join(' ').toLowerCase();
          const topic = (sub.grammarTopic || '').toLowerCase();
          const originalText = (sub.originalText || '').toLowerCase();
          const type = (sub.submissionType || '').toLowerCase();

          const matches =
            studentName.includes(q) ||
            studentEmail.includes(q) ||
            cohorts.includes(q) ||
            topic.includes(q) ||
            originalText.includes(q) ||
            type.includes(q);

          if (!matches) return false;
        }

        return true;
      })
      .sort((a, b) => {
        const scoreA = a.overrideScore ?? a.score;
        const scoreB = b.overrideScore ?? b.score;

        if (sortBy === 'newest') {
          return new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime();
        }
        if (sortBy === 'oldest') {
          return new Date(a.submittedAt).getTime() - new Date(b.submittedAt).getTime();
        }
        if (sortBy === 'highest_score') {
          return scoreB - scoreA;
        }
        if (sortBy === 'lowest_score') {
          return scoreA - scoreB;
        }
        return 0;
      });
  }, [submissions, hiddenIds, activeTab, groupFilter, statusFilter, searchQuery, studentGroupsMap, sortBy]);

  // Reset page when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, groupFilter, statusFilter, sortBy, activeTab, pageSize]);

  // Pagination calculation
  const totalPages = Math.max(1, Math.ceil(filteredSubmissions.length / pageSize));
  const paginatedSubmissions = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredSubmissions.slice(start, start + pageSize);
  }, [filteredSubmissions, currentPage, pageSize]);

  const hasActiveFilters = searchQuery.trim() !== '' || groupFilter !== 'ALL' || statusFilter !== 'ALL' || sortBy !== 'newest';

  const resetFilters = () => {
    setSearchQuery('');
    setGroupFilter('ALL');
    setStatusFilter('ALL');
    setSortBy('newest');
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading submissions awaiting review..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Submissions Review</h1>
          <p className="text-sm text-slate-500 mt-1">
            Inspect student answers and options, edit AI evaluations, provide personalized guidance, and override grades.
          </p>
        </div>

        {/* Segmented View Tabs: Active vs Hidden */}
        <div className="flex items-center space-x-1.5 p-1 bg-slate-100 rounded-2xl shrink-0 self-start sm:self-center">
          <button
            type="button"
            onClick={() => setActiveTab('active')}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
              activeTab === 'active'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <span>Active</span>
            <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
              activeTab === 'active' ? 'bg-indigo-50 text-primary' : 'bg-slate-200/60 text-slate-600'
            }`}>
              {activeCount}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('hidden')}
            className={`flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
              activeTab === 'hidden'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <EyeOff className="w-3.5 h-3.5" />
            <span>Hidden</span>
            <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
              activeTab === 'hidden' ? 'bg-amber-100 text-amber-800' : 'bg-slate-200/60 text-slate-600'
            }`}>
              {hiddenCount}
            </span>
          </button>
        </div>
      </div>

      {/* Search & Filter Toolbar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-100 shadow-sm space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Search Input */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search student, cohort, topic..."
              className="w-full pl-9 pr-8 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 p-0.5 text-slate-400 hover:text-slate-600 rounded"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Group Filter */}
          <div className="flex items-center">
            <select
              value={groupFilter}
              onChange={(e) => setGroupFilter(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
            >
              <option value="ALL">All Cohorts / Groups</option>
              {groups.map((g) => (
                <option key={g.id} value={g.name}>
                  {g.name} ({g.studentCount})
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="flex items-center">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
            >
              <option value="ALL">All Statuses</option>
              <option value="AI_GRADED">AI Graded (Pending Review)</option>
              <option value="TEACHER_GRADED">Teacher Graded (Reviewed)</option>
            </select>
          </div>

          {/* Sort Selector */}
          <div className="flex items-center">
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
              className="w-full px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
            >
              <option value="newest">Sort: Newest First</option>
              <option value="oldest">Sort: Oldest First</option>
              <option value="highest_score">Sort: Highest Score</option>
              <option value="lowest_score">Sort: Lowest Score</option>
            </select>
          </div>
        </div>

        {/* Filter bar footer info & actions */}
        <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-100 text-xs text-slate-500">
          <div className="flex items-center gap-3">
            <span>
              Showing <span className="font-bold text-slate-800">{filteredSubmissions.length}</span> {activeTab === 'active' ? 'active' : 'hidden'} submission{filteredSubmissions.length === 1 ? '' : 's'}
            </span>

            {hasActiveFilters && (
              <button
                type="button"
                onClick={resetFilters}
                className="inline-flex items-center space-x-1 text-primary hover:text-primary-hover font-bold transition"
              >
                <RotateCcw className="w-3 h-3" />
                <span>Reset Filters</span>
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            {activeTab === 'hidden' && hiddenCount > 0 && (
              <button
                type="button"
                onClick={handleUnhideAll}
                className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-800 text-xs font-bold transition"
              >
                <Eye className="w-3.5 h-3.5" />
                <span>Restore All Hidden</span>
              </button>
            )}

            <div className="flex items-center space-x-1.5 text-xs text-slate-500">
              <span>Per page:</span>
              <select
                value={pageSize}
                onChange={(e) => setPageSize(Number(e.target.value))}
                className="px-2 py-0.5 rounded-lg bg-slate-100 border border-slate-200 text-xs font-bold text-slate-700 focus:outline-none"
              >
                <option value={5}>5</option>
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Submissions List Container */}
      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        {filteredSubmissions.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-sm space-y-2">
            {activeTab === 'hidden' ? (
              <>
                <EyeOff className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                <p className="font-semibold text-slate-600">No hidden submissions.</p>
                <p className="text-xs text-slate-400">Use the "Hide" button on any active submission to clear it from your main view.</p>
              </>
            ) : hasActiveFilters ? (
              <>
                <Filter className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                <p className="font-semibold text-slate-600">No submissions match your search or filter criteria.</p>
                <button
                  type="button"
                  onClick={resetFilters}
                  className="mt-2 px-3 py-1.5 rounded-xl bg-indigo-50 text-primary text-xs font-bold hover:bg-indigo-100 transition"
                >
                  Clear Filters
                </button>
              </>
            ) : (
              <p>No student submissions to review.</p>
            )}
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {paginatedSubmissions.map((sub) => {
              const isExpanded = expandedId === sub.id;
              const effectiveScore = sub.overrideScore ?? sub.score;
              const isHidden = hiddenIds.has(sub.id);
              const studentCohorts = studentGroupsMap[sub.studentId] || [];

              return (
                <div key={sub.id} className="p-5 hover:bg-slate-50/50 transition space-y-3">
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <div className="space-y-1.5">
                      <div className="flex flex-wrap items-center gap-2">
                        {/* Student Identity and Rename Action */}
                        <div className="inline-flex items-center space-x-1.5 text-xs font-bold text-slate-800 bg-slate-100 px-2.5 py-1 rounded-lg">
                          <UserIcon className="w-3.5 h-3.5 text-primary" />
                          <span>{sub.studentName || 'Student'}</span>
                          {sub.studentEmail && (
                            <span className="text-[11px] font-normal text-slate-500">
                              ({sub.studentEmail})
                            </span>
                          )}
                          {sub.studentId && (
                            <button
                              type="button"
                              onClick={() => handleOpenRename(sub.studentId, sub.studentName || '')}
                              className="ml-1 p-0.5 text-slate-400 hover:text-primary transition"
                              title="Rename student account for reports and review"
                            >
                              <Edit3 className="w-3 h-3" />
                            </button>
                          )}
                        </div>

                        {/* Cohort Badges */}
                        {studentCohorts.map((cohortName) => (
                          <span
                            key={cohortName}
                            className="inline-flex items-center space-x-1 text-[10px] font-bold px-2 py-0.5 rounded-md bg-purple-50 text-purple-700 border border-purple-200/60"
                          >
                            <Users className="w-3 h-3 text-purple-500" />
                            <span>{cohortName}</span>
                          </span>
                        ))}

                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-50 text-primary">
                          {sub.submissionType}
                        </span>

                        {sub.grammarTopic && (
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">
                            {sub.grammarTopic}
                          </span>
                        )}

                        <span className="text-xs text-slate-400">
                          {formatDate(sub.submittedAt)}
                        </span>

                        {sub.overrideScore != null ? (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700">
                            Teacher Graded
                          </span>
                        ) : (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-amber-50 text-amber-700">
                            AI Graded
                          </span>
                        )}
                      </div>

                      <p className="text-sm font-bold text-slate-800 line-clamp-1">
                        {sub.originalText}
                      </p>
                    </div>

                    <div className="flex items-center space-x-3 self-end sm:self-center">
                      <div className="text-right">
                        <div className="text-lg font-black text-primary font-mono">
                          {Math.round(effectiveScore)}
                        </div>
                        <span className="text-[10px] text-slate-400 font-bold uppercase">
                          Score / {sub.totalPoints ?? 100}
                        </span>
                      </div>

                      <div className="flex items-center space-x-2">
                        <button
                          type="button"
                          onClick={() => handleOpenOverride(sub)}
                          className="flex items-center space-x-1 py-1.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                          <span>Override</span>
                        </button>

                        {sub.overrideScore == null ? (
                          <button
                            type="button"
                            onClick={() => handleApproveAiGrade(sub)}
                            className="flex items-center space-x-1 py-1.5 px-3 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-semibold transition"
                          >
                            <CheckCircle className="w-3.5 h-3.5" />
                            <span>Approve</span>
                          </button>
                        ) : (
                          <span
                            className="inline-flex items-center space-x-1 py-1.5 px-2.5 rounded-xl bg-emerald-50 text-emerald-700 text-xs font-semibold border border-emerald-200/60"
                            title="Grade approved by teacher"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                            <span>Approved</span>
                          </span>
                        )}

                        {/* Hide / Unhide Action */}
                        <button
                          type="button"
                          onClick={() => handleToggleHide(sub.id)}
                          className={`flex items-center space-x-1 py-1.5 px-2.5 rounded-xl text-xs font-semibold transition ${
                            isHidden
                              ? 'bg-amber-50 text-amber-700 hover:bg-amber-100'
                              : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100'
                          }`}
                          title={isHidden ? 'Restore to active queue' : 'Hide from active queue'}
                        >
                          {isHidden ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5" />}
                          <span className="hidden md:inline">{isHidden ? 'Unhide' : 'Hide'}</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => handleToggleExpand(sub)}
                          className="p-1.5 rounded-xl hover:bg-slate-100 text-slate-400 transition"
                          title={isExpanded ? 'Collapse breakdown' : 'Detailed breakdown'}
                        >
                          {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Detailed Expanded Breakdown */}
                  {isExpanded && (
                    <div className="pt-4 border-t border-slate-100 space-y-4 text-xs animate-in fade-in duration-150">
                      {/* Assignment Task & Context Passage */}
                      {sub.taskContent && (
                        <div className="p-4 bg-indigo-50/40 rounded-2xl border border-indigo-100/70 space-y-1.5">
                          <span className="text-[10px] font-bold text-indigo-700 uppercase tracking-wider flex items-center gap-1.5">
                            <BookOpen className="w-3.5 h-3.5" />
                            <span>Assignment Task & Context Context</span>
                          </span>
                          <p className="text-xs text-slate-800 whitespace-pre-wrap leading-relaxed font-sans">
                            {sub.taskContent}
                          </p>
                        </div>
                      )}

                      {/* Question Breakdown and Answer Choices */}
                      {sub.items && sub.items.length > 0 ? (
                        <div className="space-y-3">
                          <h4 className="text-[11px] font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                            <Sparkles className="w-3.5 h-3.5 text-primary" />
                            <span>Question-by-Question Diagnostic Evaluation ({sub.items.length} items)</span>
                          </h4>

                          <div className="grid grid-cols-1 gap-2.5">
                            {sub.items.map((item) => {
                              const cleanStudent = (item.studentAnswer || '').replace(/[\u00A0\u202F\u200B]/g, ' ').trim().toLowerCase();
                              const cleanCorrect = (item.correctAnswer || '').replace(/[\u00A0\u202F\u200B]/g, ' ').trim().toLowerCase();
                              const isEffectivelyCorrect = item.isCorrect || (cleanStudent !== '' && cleanStudent === cleanCorrect);

                              return (
                              <div
                                key={item.questionNumber}
                                className={`p-4 rounded-2xl border transition space-y-2.5 ${
                                  isEffectivelyCorrect
                                    ? 'bg-emerald-50/30 border-emerald-100/80'
                                    : 'bg-rose-50/30 border-rose-100/80'
                                }`}
                              >
                                <div className="flex items-center justify-between gap-2">
                                  <div className="flex items-center gap-2">
                                    <span className="font-mono font-bold text-slate-700 text-xs">
                                      Question {item.questionNumber}
                                    </span>
                                    {typeof item.points === 'number' && (
                                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700 border border-slate-200">
                                        {item.points} pts
                                      </span>
                                    )}
                                  </div>

                                  {isEffectivelyCorrect ? (
                                    <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                                      <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                                      <span>Correct</span>
                                    </span>
                                  ) : (
                                    <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-100 text-rose-800 border border-rose-200">
                                      <XCircle className="w-3 h-3 text-rose-600" />
                                      <span>Incorrect</span>
                                    </span>
                                  )}
                                </div>

                                <p className="text-xs font-semibold text-slate-800">
                                  {item.sentence}
                                </p>

                                {/* Answer choices / options if present */}
                                {item.options && item.options.length > 0 && (
                                  <div className="space-y-1">
                                    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                                      Answer Options:
                                    </span>
                                    <div className="flex flex-wrap gap-1.5">
                                      {item.options.map((opt, optIdx) => {
                                        const optTrim = opt.replace(/[\u00A0\u202F\u200B]/g, ' ').trim().toLowerCase();
                                        const isStudentChoice = optTrim === cleanStudent;
                                        const isCorrectChoice = optTrim === cleanCorrect;

                                        let badgeStyle = 'bg-white text-slate-600 border-slate-200';
                                        if (isStudentChoice && isEffectivelyCorrect) {
                                          badgeStyle = 'bg-emerald-100 text-emerald-900 border-emerald-300 font-bold';
                                        } else if (isStudentChoice && !isEffectivelyCorrect) {
                                          badgeStyle = 'bg-rose-100 text-rose-900 border-rose-300 font-bold';
                                        } else if (isCorrectChoice) {
                                          badgeStyle = 'bg-emerald-50 text-emerald-800 border-emerald-300 font-semibold';
                                        }

                                        return (
                                          <div
                                            key={optIdx}
                                            className={`px-2.5 py-1 rounded-xl border text-xs flex items-center gap-1.5 ${badgeStyle}`}
                                          >
                                            <span>{opt}</span>
                                            {isStudentChoice && (
                                              <span className="text-[9px] px-1 py-0.2 rounded bg-black/10 font-bold">
                                                {isEffectivelyCorrect ? 'Student Pick (✓)' : 'Student Pick (✗)'}
                                              </span>
                                            )}
                                            {!isStudentChoice && isCorrectChoice && (
                                              <span className="text-[9px] px-1 py-0.2 rounded bg-emerald-200 text-emerald-900 font-bold">
                                                Correct
                                              </span>
                                            )}
                                          </div>
                                        );
                                      })}
                                    </div>
                                  </div>
                                )}

                                {/* Student Answer vs Correct Answer Summary */}
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 p-2.5 bg-white/80 rounded-xl border border-slate-100 text-xs">
                                  <div>
                                    <span className="text-[10px] font-bold text-slate-400 uppercase">Student Answer:</span>
                                    <p className={`font-mono font-bold mt-0.5 ${isEffectivelyCorrect ? 'text-emerald-700' : 'text-rose-600'}`}>
                                      {item.studentAnswer || 'No answer'}
                                    </p>
                                  </div>
                                  <div>
                                    <span className="text-[10px] font-bold text-slate-400 uppercase">Expected Answer:</span>
                                    <p className="font-mono font-bold text-emerald-700 mt-0.5">
                                      {item.correctAnswer}
                                    </p>
                                  </div>
                                </div>

                                {/* Item explanation & rule */}
                                {item.explanation && (
                                  <div className="p-2.5 rounded-xl bg-slate-50 text-slate-600 text-xs leading-relaxed space-y-0.5">
                                    <p>
                                      <span className="font-bold text-slate-700">Diagnostic Rule: </span>
                                      {item.explanation}
                                    </p>
                                    {item.grammarRule && (
                                      <span className="inline-block text-[10px] font-bold text-primary px-1.5 py-0.2 rounded bg-indigo-50 border border-indigo-100">
                                        {item.grammarRule}
                                      </span>
                                    )}
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                        </div>
                      ) : (
                        <div>
                          <span className="font-bold text-slate-500 uppercase">Student Answer Text:</span>
                          <div className="p-3 mt-1 bg-slate-50 rounded-xl font-mono text-slate-700 whitespace-pre-wrap">
                            {sub.originalText}
                          </div>
                        </div>
                      )}

                      {/* AI Diagnostic Comment Section with Inline Edit */}
                      <div className="p-4 rounded-2xl bg-indigo-50/50 border border-indigo-100/70 space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-indigo-900 uppercase tracking-wider flex items-center gap-1.5 text-[11px]">
                            <Bot className="w-3.5 h-3.5 text-primary" />
                            <span>AI Evaluation & Diagnostic Comment</span>
                          </span>

                          {editingFeedbackId !== sub.id ? (
                            <button
                              type="button"
                              onClick={() => {
                                setEditingFeedbackId(sub.id);
                                setFeedbackDraft(sub.feedback || '');
                              }}
                              className="inline-flex items-center space-x-1 text-xs font-bold text-primary hover:text-primary-hover px-2 py-1 rounded-lg hover:bg-indigo-100/60 transition"
                            >
                              <Edit3 className="w-3 h-3" />
                              <span>Edit AI Comment</span>
                            </button>
                          ) : (
                            <div className="flex items-center space-x-1.5">
                              <button
                                type="button"
                                onClick={() => setEditingFeedbackId(null)}
                                className="px-2 py-0.5 text-xs text-slate-500 hover:text-slate-700 font-semibold"
                              >
                                Cancel
                              </button>
                              <button
                                type="button"
                                onClick={() => handleSaveInlineFeedback(sub)}
                                disabled={isSavingInline}
                                className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-primary text-white text-xs font-bold hover:bg-primary-hover transition disabled:opacity-50"
                              >
                                <Save className="w-3 h-3" />
                                <span>Save</span>
                              </button>
                            </div>
                          )}
                        </div>

                        {editingFeedbackId === sub.id ? (
                          <textarea
                            rows={3}
                            value={feedbackDraft}
                            onChange={(e) => setFeedbackDraft(e.target.value)}
                            className="w-full p-3 rounded-xl border border-indigo-200 bg-white text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                            placeholder="Edit AI evaluation feedback..."
                          />
                        ) : (
                          <div className="text-slate-700 leading-relaxed whitespace-pre-wrap text-xs">
                            {sub.feedback || 'No AI commentary recorded.'}
                          </div>
                        )}
                      </div>

                      {/* Teacher Advice / Comment for Student with Inline Edit */}
                      <div className="p-4 rounded-2xl bg-amber-50/70 border border-amber-200/80 space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-amber-900 uppercase tracking-wider flex items-center gap-1.5 text-[11px]">
                            <Lightbulb className="w-3.5 h-3.5 text-amber-600" />
                            <span>Teacher Advice & Guidance for Student</span>
                          </span>

                          {editingCommentId !== sub.id ? (
                            <button
                              type="button"
                              onClick={() => {
                                setEditingCommentId(sub.id);
                                setCommentDraft(sub.teacherComment || '');
                              }}
                              className="inline-flex items-center space-x-1 text-xs font-bold text-amber-800 hover:text-amber-900 px-2 py-1 rounded-lg hover:bg-amber-100 transition"
                            >
                              <Edit3 className="w-3 h-3" />
                              <span>Edit Advice</span>
                            </button>
                          ) : (
                            <div className="flex items-center space-x-1.5">
                              <button
                                type="button"
                                onClick={() => setEditingCommentId(null)}
                                className="px-2 py-0.5 text-xs text-slate-500 hover:text-slate-700 font-semibold"
                              >
                                Cancel
                              </button>
                              <button
                                type="button"
                                onClick={() => handleSaveInlineComment(sub)}
                                disabled={isSavingInline}
                                className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-amber-600 text-white text-xs font-bold hover:bg-amber-700 transition disabled:opacity-50"
                              >
                                <Save className="w-3 h-3" />
                                <span>Save</span>
                              </button>
                            </div>
                          )}
                        </div>

                        {editingCommentId === sub.id ? (
                          <textarea
                            rows={3}
                            value={commentDraft}
                            onChange={(e) => setCommentDraft(e.target.value)}
                            className="w-full p-3 rounded-xl border border-amber-300 bg-white text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition"
                            placeholder="Write personalized guidance or praise for the student..."
                          />
                        ) : sub.teacherComment ? (
                          <div className="text-amber-950 leading-relaxed whitespace-pre-wrap text-xs italic bg-white/70 p-3 rounded-xl border border-amber-100">
                            "{sub.teacherComment}"
                          </div>
                        ) : (
                          <p className="text-xs text-amber-800/70 italic">
                            No personalized advice added yet. Click "Edit Advice" above or "Override" to give customized guidance.
                          </p>
                        )}
                      </div>

                      {/* AI Gap Analysis Summary if present */}
                      {sub.aiAnalysis && (
                        <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/70 space-y-2">
                          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                            AI Diagnostic Synthesis
                          </span>

                          {sub.aiAnalysis.summary && (
                            <p className="text-xs text-slate-700">{sub.aiAnalysis.summary}</p>
                          )}

                          <div className="flex flex-wrap gap-4 pt-1">
                            {sub.aiAnalysis.strengths && sub.aiAnalysis.strengths.length > 0 && (
                              <div className="space-y-1">
                                <span className="text-[10px] font-bold text-emerald-700 uppercase">Strengths:</span>
                                <div className="flex flex-wrap gap-1">
                                  {sub.aiAnalysis.strengths.map((str, i) => (
                                    <span key={i} className="px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700 text-[10px] font-bold">
                                      {str}
                                    </span>
                                  ))}
                                </div>
                              </div>
                            )}

                            {sub.aiAnalysis.weaknesses && sub.aiAnalysis.weaknesses.length > 0 && (
                              <div className="space-y-1">
                                <span className="text-[10px] font-bold text-rose-700 uppercase">Areas to Practice:</span>
                                <div className="flex flex-wrap gap-1">
                                  {sub.aiAnalysis.weaknesses.map((w, i) => (
                                    <span key={i} className="px-2 py-0.5 rounded-md bg-rose-50 text-rose-700 text-[10px] font-bold">
                                      {w}
                                    </span>
                                  ))}
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination Controls */}
        {filteredSubmissions.length > pageSize && (
          <div className="px-5 py-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500 bg-slate-50/40">
            <div>
              Showing <span className="font-bold text-slate-800">{(currentPage - 1) * pageSize + 1}</span> to{' '}
              <span className="font-bold text-slate-800">
                {Math.min(currentPage * pageSize, filteredSubmissions.length)}
              </span>{' '}
              of <span className="font-bold text-slate-800">{filteredSubmissions.length}</span>
            </div>

            <div className="flex items-center space-x-1.5">
              <button
                type="button"
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition disabled:opacity-40 disabled:hover:bg-white"
                title="Previous page"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <span className="px-2.5 py-1 font-bold text-slate-700">
                Page {currentPage} of {totalPages}
              </span>

              <button
                type="button"
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="p-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition disabled:opacity-40 disabled:hover:bg-white"
                title="Next page"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Grade, AI Feedback, and Teacher Comment Override Modal */}
      {editingSub && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-md p-4 animate-in fade-in duration-150"
          onClick={() => setEditingSub(null)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="override-modal-title"
        >
          <div
            className="relative overflow-hidden bg-white/95 backdrop-blur-xl rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl shadow-slate-950/25 border border-slate-200/80 ring-1 ring-slate-900/5 space-y-4 animate-in zoom-in-95 duration-150"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="absolute inset-x-0 top-0 h-1.5 bg-gradient-to-r from-indigo-500 via-violet-500 to-sky-500" />

            <button
              type="button"
              onClick={() => setEditingSub(null)}
              className="absolute top-4 right-4 p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100/80 transition"
              aria-label="Close"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center ring-4 ring-indigo-500/10 border border-indigo-200/60 shrink-0">
                <Edit3 className="w-5 h-5" />
              </div>
              <div className="min-w-0 pr-6">
                <span className="inline-block text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200/60 mb-0.5">
                  Teacher Assessment
                </span>
                <h3 id="override-modal-title" className="text-lg font-black text-slate-900 tracking-tight leading-snug">
                  Override Grade & Feedback
                </h3>
              </div>
            </div>

            <div className="bg-slate-50/90 rounded-2xl p-3 border border-slate-200/70 flex items-center justify-between text-xs">
              <span className="text-slate-600 truncate max-w-[240px]">
                {editingSub.grammarTopic || 'Grammar Practice'}
              </span>
              <span className="font-mono font-bold text-slate-500">
                Original Score: <span className="text-primary font-black">{editingSub.score} / {editingSub.totalPoints ?? 100}</span>
              </span>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5 tracking-wider">
                New Score (0 - {editingSub.totalPoints ?? 100})
              </label>
              <input
                type="number"
                min="0"
                max={editingSub.totalPoints ?? 100}
                value={newScore}
                onChange={(e) => setNewScore(Number(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200/90 bg-white text-sm font-mono font-bold focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5 tracking-wider">
                AI Diagnostic Evaluation Comment
              </label>
              <textarea
                rows={3}
                value={aiFeedbackDraft}
                onChange={(e) => setAiFeedbackDraft(e.target.value)}
                placeholder="Edit or refine the AI's diagnostic comment..."
                className="w-full p-3 rounded-xl border border-slate-200/90 bg-white text-xs text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5 tracking-wider">
                Teacher Advice & Commentary for Student
              </label>
              <textarea
                rows={3}
                value={teacherComment}
                onChange={(e) => setTeacherComment(e.target.value)}
                placeholder="Write specific advice and suggestions to guide the student's progress..."
                className="w-full p-3 rounded-xl border border-slate-200/90 bg-white text-xs text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setEditingSub(null)}
                className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveOverride}
                disabled={isSubmittingOverride}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white transition shadow-md shadow-indigo-500/20 disabled:opacity-50"
              >
                {isSubmittingOverride ? 'Saving...' : 'Apply Changes'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Student Account Rename Modal */}
      {renamingStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <UserIcon className="w-4 h-4 text-primary" />
                <span>Rename Student Account</span>
              </h3>
              <button
                type="button"
                onClick={() => setRenamingStudent(null)}
                className="p-1 rounded-xl text-slate-400 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-500 leading-relaxed">
              Set a recognizable display name for this student. This name will appear across your group rosters, submissions queue, and printed PDF/CSV reports.
            </p>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                Student Full Name / Alias
              </label>
              <input
                type="text"
                value={renameInput}
                onChange={(e) => setRenameInput(e.target.value)}
                placeholder="e.g. John Doe / Иванов Иван"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                autoFocus
              />
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setRenamingStudent(null)}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveRename}
                disabled={isSavingName || !renameInput.trim()}
                className="px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
              >
                {isSavingName ? 'Saving...' : 'Save Name'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
