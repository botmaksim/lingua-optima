/**
 * @file ExportReports.tsx
 * @brief Educator reporting interface for downloading student cohort performance in PDF or CSV formats.
 */

import React, { useState, useEffect } from 'react';
import { FileText, Download, FileSpreadsheet, CheckCircle2 } from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { exportApi } from '../../api/exportApi';
import { Group } from '../../types/group';
import { CustomSelect } from '../common/CustomSelect';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Panel allowing educators to export cohort progress reports in PDF and CSV format.
 * @return JSX report export view.
 */
export const ExportReports: React.FC = () => {
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState<string>('');
  const [format, setFormat] = useState<'csv' | 'pdf'>('pdf');
  const [isLoading, setIsLoading] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);

  useEffect(() => {
    groupApi
      .getGroups()
      .then((data) => {
        setGroups(data);
        if (data.length > 0) setSelectedGroupId(data[0].id);
      })
      .catch((err) => console.error('Failed to load groups:', err));
  }, []);

  /**
   * @brief Event handler or helper executing handle download.
   */
  const handleDownload = async () => {
    if (!selectedGroupId) return;
    setIsLoading(true);
    setDownloadSuccess(false);

    try {
      const blob = await exportApi.downloadGroupReport(selectedGroupId, format);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `group_report_${selectedGroupId}.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to download report:', err);
      alert('Failed to generate report.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Export Academic Reports</h1>
        <p className="text-sm text-slate-500 mt-1">
          Generate comprehensive diagnostic and grading reports for individual cohorts in PDF or CSV format.
        </p>
      </div>

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
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
              className={`p-4 rounded-2xl border text-left transition flex items-center space-x-3 ${
                format === 'pdf'
                  ? 'border-primary bg-indigo-50/60 text-primary font-bold shadow-sm'
                  : 'border-slate-200 text-slate-600 hover:border-slate-300'
              }`}
            >
              <FileText className="w-5 h-5 text-rose-500" />
              <div>
                <p className="text-sm">Formal PDF Document</p>
                <p className="text-[11px] text-slate-400 font-normal">Ready for printing or sharing</p>
              </div>
            </button>

            <button
              type="button"
              onClick={() => setFormat('csv')}
              className={`p-4 rounded-2xl border text-left transition flex items-center space-x-3 ${
                format === 'csv'
                  ? 'border-primary bg-indigo-50/60 text-primary font-bold shadow-sm'
                  : 'border-slate-200 text-slate-600 hover:border-slate-300'
              }`}
            >
              <FileSpreadsheet className="w-5 h-5 text-emerald-600" />
              <div>
                <p className="text-sm">Raw CSV Spreadsheet</p>
                <p className="text-[11px] text-slate-400 font-normal">Excel, Google Sheets compatible</p>
              </div>
            </button>
          </div>
        </div>

        {downloadSuccess && (
          <div className="p-3 bg-emerald-50 text-emerald-800 rounded-xl text-xs flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>Report downloaded successfully!</span>
          </div>
        )}

        <button
          type="button"
          onClick={handleDownload}
          disabled={isLoading || !selectedGroupId}
          className="w-full flex items-center justify-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
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
  );
};
