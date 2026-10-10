/**
 * @file ExportReports.tsx
 * @brief Educator reporting interface for inspecting and downloading cohort homework performance in PDF or CSV formats.
 */

import React, { useState, useEffect } from 'react';
import {
  FileText,
  Download,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  BookOpen,
  Users,
  CheckCircle,
  Clock,
  Sparkles,
  RefreshCw,
  Award,
  Calendar,
  Printer,
} from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { exportApi } from '../../api/exportApi';
import { Group } from '../../types/group';
import { GroupReportResponse } from '../../types/export';
import { CustomSelect } from '../common/CustomSelect';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useNotificationStore } from '../../store/notificationStore';

type PeriodType = 'all' | '7d' | '30d' | 'month' | 'custom';

/**
 * @brief Panel allowing educators to inspect and export cohort progress reports in PDF and CSV format.
 * @return JSX report export view.
 */
export const ExportReports: React.FC = () => {
  const { addToast } = useNotificationStore();
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<string>('');
  const [format, setFormat] = useState<'csv' | 'pdf'>('pdf');
  const [periodType, setPeriodType] = useState<PeriodType>('all');
  const [customFrom, setCustomFrom] = useState<string>('');
  const [customTo, setCustomTo] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Live preview state
  const [previewData, setPreviewData] = useState<GroupReportResponse | null>(null);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [activeTab, setActiveTab] = useState<'homeworks' | 'roster'>('homeworks');

  useEffect(() => {
    groupApi
      .getGroups()
      .then((data) => {
        setGroups(data);
        if (data.length > 0) setSelectedGroupId(data[0].id);
      })
      .catch((err) => console.error('Failed to load groups:', err));
  }, []);

  const computeRangeIso = (
    type: PeriodType,
    fromStr: string,
    toStr: string
  ): { fromIso?: string; toIso?: string } => {
    if (type === '7d') {
      return { fromIso: new Date(Date.now() - 7 * 86400000).toISOString() };
    }
    if (type === '30d') {
      return { fromIso: new Date(Date.now() - 30 * 86400000).toISOString() };
    }
    if (type === 'month') {
      const now = new Date();
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
      return { fromIso: startOfMonth.toISOString() };
    }
    if (type === 'custom') {
      const fromIso = fromStr ? new Date(`${fromStr}T00:00:00`).toISOString() : undefined;
      const toIso = toStr ? new Date(`${toStr}T23:59:59`).toISOString() : undefined;
      return { fromIso, toIso };
    }
    return {};
  };

  const loadPreview = (
    groupId: string,
    pType: PeriodType = periodType,
    cFrom: string = customFrom,
    cTo: string = customTo
  ) => {
    if (!groupId) {
      setPreviewData(null);
      return;
    }
    setIsLoadingPreview(true);
    const { fromIso, toIso } = computeRangeIso(pType, cFrom, cTo);
    const promise = fromIso || toIso
      ? exportApi.getGroupReportPreview(groupId, fromIso, toIso)
      : exportApi.getGroupReportPreview(groupId);
    promise
      .then((data) => setPreviewData(data))
      .catch((err) => {
        console.error('Failed to load report preview:', err);
        setPreviewData(null);
      })
      .finally(() => setIsLoadingPreview(false));
  };

  useEffect(() => {
    if (selectedGroupId) {
      loadPreview(selectedGroupId, periodType, customFrom, customTo);
    }
  }, [selectedGroupId, periodType, customFrom, customTo]);

  /**
   * @brief Event handler downloading cohort report in selected format.
   */
  const handleDownload = async () => {
    if (!selectedGroupId) return;
    setIsLoading(true);
    setDownloadSuccess(false);
    setErrorMessage(null);

    try {
      const { fromIso, toIso } = computeRangeIso(periodType, customFrom, customTo);
      const blob = fromIso || toIso
        ? await exportApi.downloadGroupReport(selectedGroupId, format, fromIso, toIso)
        : await exportApi.downloadGroupReport(selectedGroupId, format);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `group_report_${selectedGroupId}.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      setDownloadSuccess(true);
      addToast({
        type: 'success',
        title: 'Report Exported',
        message: `Your ${format.toUpperCase()} cohort report has been downloaded.`,
      });
      setTimeout(() => setDownloadSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to download report:', err);
      setErrorMessage('Failed to generate report. Please try again shortly.');
      addToast({
        type: 'error',
        title: 'Export Failed',
        message: 'Could not generate the cohort report. Please try again.',
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <div className="no-print">
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Export Academic Reports</h1>
        <p className="text-sm text-slate-500 mt-1">
          Generate comprehensive diagnostic and grading reports for individual cohorts in PDF or CSV format.
        </p>
      </div>

      {/* Export Configuration Controls Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6 no-print export-controls-card">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Select Cohort Group
            </label>
            <CustomSelect
              size="lg"
              value={selectedGroupId}
              onChange={(val) => setSelectedGroupId(val)}
              options={groups.map((g) => ({
                value: g.id,
                label: g.name,
                badge: `${g.studentCount} students`,
              }))}
              placeholder="Select a cohort group..."
              ariaLabel="Select Cohort Group"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Report Format
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setFormat('pdf')}
                className={`p-3.5 rounded-2xl border text-left transition flex items-center space-x-3 ${
                  format === 'pdf'
                    ? 'border-primary bg-indigo-50/60 text-primary font-bold shadow-sm'
                    : 'border-slate-200 text-slate-600 hover:border-slate-300'
                }`}
              >
                <FileText className="w-5 h-5 text-rose-500 shrink-0" />
                <div>
                  <p className="text-xs sm:text-sm">PDF Document</p>
                  <p className="text-[10px] text-slate-400 font-normal">Formatted for print & sharing</p>
                </div>
              </button>

              <button
                type="button"
                onClick={() => setFormat('csv')}
                className={`p-3.5 rounded-2xl border text-left transition flex items-center space-x-3 ${
                  format === 'csv'
                    ? 'border-primary bg-indigo-50/60 text-primary font-bold shadow-sm'
                    : 'border-slate-200 text-slate-600 hover:border-slate-300'
                }`}
              >
                <FileSpreadsheet className="w-5 h-5 text-emerald-600 shrink-0" />
                <div>
                  <p className="text-xs sm:text-sm">CSV Spreadsheet</p>
                  <p className="text-[10px] text-slate-400 font-normal">Excel / Sheets compatible</p>
                </div>
              </button>
            </div>
          </div>
        </div>

        {/* Timeframe / Period Filter */}
        <div className="pt-4 border-t border-slate-100">
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5 text-indigo-500" />
            <span>Reporting Timeframe / Period</span>
          </label>
          <div className="flex flex-wrap items-center gap-2">
            {[
              { id: 'all', label: 'All Time' },
              { id: '7d', label: 'Last 7 Days' },
              { id: '30d', label: 'Last 30 Days' },
              { id: 'month', label: 'This Month' },
              { id: 'custom', label: 'Custom Range' },
            ].map((preset) => (
              <button
                key={preset.id}
                type="button"
                onClick={() => setPeriodType(preset.id as PeriodType)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 ${
                  periodType === preset.id
                    ? 'bg-primary text-white shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                }`}
              >
                <span>{preset.label}</span>
              </button>
            ))}
          </div>

          {periodType === 'custom' && (
            <div className="flex flex-wrap items-center gap-3 mt-3 p-3 bg-slate-50/80 rounded-2xl border border-slate-100 animate-in fade-in duration-150">
              <div className="flex items-center space-x-2">
                <span className="text-xs font-medium text-slate-500">From:</span>
                <input
                  type="date"
                  lang="en-US"
                  value={customFrom}
                  onChange={(e) => setCustomFrom(e.target.value)}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div className="flex items-center space-x-2">
                <span className="text-xs font-medium text-slate-500">To:</span>
                <input
                  type="date"
                  lang="en-US"
                  value={customTo}
                  onChange={(e) => setCustomTo(e.target.value)}
                  className="px-3 py-1.5 rounded-xl border border-slate-200 text-xs text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
          )}
        </div>

        {downloadSuccess && (
          <div className="p-3 bg-emerald-50 text-emerald-800 rounded-xl text-xs flex items-center space-x-2 border border-emerald-200/80 animate-in fade-in duration-150">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Report downloaded successfully!</span>
          </div>
        )}

        {errorMessage && (
          <div className="p-3 bg-rose-50 text-rose-800 rounded-xl text-xs flex items-center space-x-2 border border-rose-200/80 animate-in fade-in duration-150">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2 border-t border-slate-100">
          <div className="text-xs text-slate-400">
            Includes overall student statistics and detailed question/score breakdown for each assigned homework.
          </div>
          <button
            type="button"
            onClick={handleDownload}
            disabled={isLoading || !selectedGroupId}
            className="w-full sm:w-auto flex items-center justify-center space-x-2 py-3 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50 text-xs sm:text-sm"
          >
            {isLoading ? (
              <LoadingSpinner size="sm" className="p-0 text-white" />
            ) : (
              <>
                <Download className="w-4 h-4" />
                <span>Download Report ({format.toUpperCase()})</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Cohort Performance Overview & Homework Breakdown Preview */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6 printable-report">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-100 no-print">
          <div>
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-primary" />
              <span>Cohort Performance Preview</span>
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Live inspection of student submissions and completion status across all homework assignments.
            </p>
          </div>

          <div className="flex items-center space-x-2">
            <button
              type="button"
              onClick={() => window.print()}
              disabled={isLoadingPreview || !previewData}
              className="flex items-center space-x-1.5 py-2 px-3 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold transition shadow-2xs disabled:opacity-50"
              title="Print academic report"
            >
              <Printer className="w-4 h-4 text-slate-600" />
              <span>Print Report</span>
            </button>

            <button
              type="button"
              onClick={() => loadPreview(selectedGroupId, periodType, customFrom, customTo)}
              disabled={isLoadingPreview || !selectedGroupId}
              className="p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
              title="Refresh Preview"
            >
              <RefreshCw className={`w-4 h-4 ${isLoadingPreview ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {isLoadingPreview ? (
          <div className="py-12 flex justify-center">
            <LoadingSpinner size="md" message="Loading cohort performance data..." />
          </div>
        ) : previewData ? (
          <div className="space-y-6">
            {/* Print Only Branded Header */}
            <div className="print-only mb-6 pb-4 border-b border-slate-300">
              <div className="flex justify-between items-start">
                <div>
                  <h1 className="text-xl font-black text-slate-900 tracking-tight">
                    Lingua Optima — Academic Performance Report
                  </h1>
                  <p className="text-xs text-slate-600 mt-1">
                    Cohort: <strong>{previewData.groupName}</strong> | Educator: <strong>{previewData.teacherName}</strong>
                  </p>
                </div>
                <div className="text-right text-xs text-slate-600">
                  <p>Period: <strong className="text-indigo-700">{previewData.periodLabel || 'All Time'}</strong></p>
                  <p>Generated: {new Date(previewData.generatedAt).toLocaleDateString()}</p>
                </div>
              </div>
            </div>

            {/* Timeframe indicator badge in interactive preview */}
            {previewData.periodLabel && (
              <div className="flex items-center space-x-2 text-xs no-print">
                <span className="text-slate-400 font-medium">Filtered Period:</span>
                <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-indigo-50 border border-indigo-100 text-primary font-bold text-[11px]">
                  <Calendar className="w-3 h-3 text-primary" />
                  <span>{previewData.periodLabel}</span>
                </span>
              </div>
            )}

            {/* Quick Metrics Bar */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-100 space-y-1">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-indigo-500" />
                  <span>Enrolled</span>
                </span>
                <p className="text-xl font-black text-slate-800 font-mono">
                  {previewData.activeStudentCount}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-100 space-y-1">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <BookOpen className="w-3.5 h-3.5 text-primary" />
                  <span>Homeworks</span>
                </span>
                <p className="text-xl font-black text-slate-800 font-mono">
                  {previewData.assignedHomeworkCount}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-100 space-y-1">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-500" />
                  <span>Submissions</span>
                </span>
                <p className="text-xl font-black text-slate-800 font-mono">
                  {previewData.totalSubmissionsCount}
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-100 space-y-1">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Award className="w-3.5 h-3.5 text-amber-500" />
                  <span>Avg Score</span>
                </span>
                <p className="text-xl font-black text-primary font-mono">
                  {previewData.groupAverageScore != null ? `${previewData.groupAverageScore}%` : '—'}
                </p>
              </div>
            </div>

            {/* Navigation Tabs */}
            <div className="flex border-b border-slate-100 space-x-4 no-print">
              <button
                type="button"
                onClick={() => setActiveTab('homeworks')}
                className={`pb-3 text-xs font-bold transition flex items-center space-x-2 border-b-2 ${
                  activeTab === 'homeworks'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-slate-400 hover:text-slate-700'
                }`}
              >
                <BookOpen className="w-4 h-4" />
                <span>Homework Assignments Breakdown ({previewData.homeworkReports.length})</span>
              </button>

              <button
                type="button"
                onClick={() => setActiveTab('roster')}
                className={`pb-3 text-xs font-bold transition flex items-center space-x-2 border-b-2 ${
                  activeTab === 'roster'
                    ? 'border-primary text-primary'
                    : 'border-transparent text-slate-400 hover:text-slate-700'
                }`}
              >
                <Users className="w-4 h-4" />
                <span>Student Roster Overview ({previewData.studentSummaries.length})</span>
              </button>
            </div>

            {/* Tab 1: Detailed Homework Assignments Breakdown */}
            {activeTab === 'homeworks' && (
              <div className="space-y-6">
                {previewData.homeworkReports.length === 0 ? (
                  <div className="p-8 text-center rounded-2xl bg-slate-50/60 border border-dashed border-slate-200 text-xs text-slate-400 space-y-2">
                    <BookOpen className="w-8 h-8 text-slate-300 mx-auto" />
                    <p className="font-semibold text-slate-600">No homework assignments yet</p>
                    <p>Assign tasks to this cohort from the "Configure Task" panel to track student completion.</p>
                  </div>
                ) : (
                  previewData.homeworkReports.map((hw, idx) => (
                    <div
                      key={hw.taskId || `hw-${idx}`}
                      className="rounded-2xl border border-slate-200/90 bg-white overflow-hidden shadow-2xs space-y-3"
                    >
                      {/* Homework Header */}
                      <div className="p-4 bg-slate-50/80 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center space-x-3">
                          <span className="w-7 h-7 rounded-xl bg-primary/10 text-primary text-xs font-black flex items-center justify-center font-mono">
                            {idx + 1}
                          </span>
                          <div>
                            <div className="flex items-center space-x-2">
                              <h3 className="text-sm font-bold text-slate-800">
                                {hw.grammarTopic}
                              </h3>
                              <span className="px-2 py-0.5 rounded-md bg-slate-200/60 text-slate-700 text-[10px] font-bold">
                                {hw.taskType}
                              </span>
                              <CefrBadge level={hw.cefrLevel} size="sm" />
                            </div>
                            <p className="text-[11px] text-slate-400 mt-0.5 flex items-center gap-2">
                              <span>Max Points: <strong className="text-slate-600">{hw.totalPoints} pts</strong></span>
                              <span>·</span>
                              <span className="flex items-center gap-1">
                                <Clock className="w-3 h-3 text-slate-400" />
                                <span>{hw.dueDate ? `Due: ${new Date(hw.dueDate).toLocaleString()}` : 'No deadline'}</span>
                              </span>
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* Student Completion Table */}
                      <div className="overflow-x-auto p-4 pt-1">
                        <table className="w-full text-left text-xs">
                          <thead>
                            <tr className="border-b border-slate-100 text-slate-400 text-[10px] uppercase tracking-wider">
                              <th className="py-2.5 px-3 font-bold">Student Name</th>
                              <th className="py-2.5 px-3 font-bold">Status</th>
                              <th className="py-2.5 px-3 font-bold">Score</th>
                              <th className="py-2.5 px-3 font-bold">Attempts</th>
                              <th className="py-2.5 px-3 font-bold">Submitted At</th>
                              <th className="py-2.5 px-3 font-bold">Teacher Advice / AI Verdict</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-50">
                            {hw.studentResults.map((res) => {
                              let statusBadge = (
                                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600">
                                  {res.status}
                                </span>
                              );
                              if (res.status === 'GRADED') {
                                statusBadge = (
                                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                                    Graded
                                  </span>
                                );
                              } else if (res.status === 'SUBMITTED') {
                                statusBadge = (
                                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
                                    Submitted
                                  </span>
                                );
                              } else if (res.status === 'OVERDUE') {
                                statusBadge = (
                                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                                    Overdue
                                  </span>
                                );
                              } else if (res.status === 'PENDING') {
                                statusBadge = (
                                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
                                    Pending
                                  </span>
                                );
                              }

                              return (
                                <tr key={res.studentId} className="hover:bg-slate-50/60 transition">
                                  <td className="py-2.5 px-3">
                                    <div className="font-bold text-slate-800">{res.studentName}</div>
                                    <div className="text-[10px] text-slate-400 font-mono">{res.email}</div>
                                  </td>
                                  <td className="py-2.5 px-3">{statusBadge}</td>
                                  <td className="py-2.5 px-3 font-mono">
                                    {res.score != null ? (
                                      <span className="font-bold text-primary">
                                        {Math.round(res.score)} / {res.totalPoints}{' '}
                                        <span className="text-[10px] text-slate-400 font-normal">
                                          ({res.percentage}%)
                                        </span>
                                      </span>
                                    ) : (
                                      <span className="text-slate-400">—</span>
                                    )}
                                  </td>
                                  <td className="py-2.5 px-3 font-mono text-slate-600">
                                    {res.attemptsUsed} / {res.maxAttempts || '∞'}
                                  </td>
                                  <td className="py-2.5 px-3 text-slate-500 font-mono text-[11px]">
                                    {res.submittedAt ? new Date(res.submittedAt).toLocaleString() : '—'}
                                  </td>
                                  <td className="py-2.5 px-3 text-slate-600 max-w-xs truncate text-[11px]">
                                    {res.teacherComment && res.teacherComment !== '—' ? (
                                      <span className="text-amber-800 font-semibold" title={res.teacherComment}>
                                        {res.teacherComment}
                                      </span>
                                    ) : res.aiFeedback && res.aiFeedback !== '—' ? (
                                      <span className="text-slate-500" title={res.aiFeedback}>
                                        {res.aiFeedback}
                                      </span>
                                    ) : (
                                      <span className="text-slate-400">—</span>
                                    )}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {/* Tab 2: Roster Summary */}
            {activeTab === 'roster' && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-100 text-slate-400 text-[10px] uppercase tracking-wider">
                      <th className="py-3 px-3.5 font-bold">Student Name</th>
                      <th className="py-3 px-3.5 font-bold">Email</th>
                      <th className="py-3 px-3.5 font-bold">CEFR</th>
                      <th className="py-3 px-3.5 font-bold">Tasks Done</th>
                      <th className="py-3 px-3.5 font-bold">Average Score</th>
                      <th className="py-3 px-3.5 font-bold">Submissions</th>
                      <th className="py-3 px-3.5 font-bold">Joined Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {previewData.studentSummaries.map((s) => (
                      <tr key={s.studentId} className="hover:bg-slate-50/60 transition">
                        <td className="py-3 px-3.5 font-bold text-slate-800">{s.studentName}</td>
                        <td className="py-3 px-3.5 text-slate-500 font-mono text-xs">{s.email}</td>
                        <td className="py-3 px-3.5">
                          <CefrBadge level={s.cefrLevel as any} size="sm" />
                        </td>
                        <td className="py-3 px-3.5 font-mono text-slate-700">
                          {s.totalTasks > 0 ? `${s.completedTasks} / ${s.totalTasks}` : '0 / 0'}
                        </td>
                        <td className="py-3 px-3.5 font-mono font-bold text-primary">
                          {s.averageScore != null ? `${s.averageScore}%` : 'N/A'}
                        </td>
                        <td className="py-3 px-3.5 font-mono text-slate-600">{s.submissionCount}</td>
                        <td className="py-3 px-3.5 text-slate-400 text-[11px] font-mono">{s.joinedDate}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        ) : (
          <div className="p-8 text-center text-xs text-slate-400">
            Select a cohort group above to view performance metrics.
          </div>
        )}
      </div>
    </div>
  );
};
