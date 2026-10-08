import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { TeacherDashboard } from '../components/teacher/TeacherDashboard';
import { StudentGroups } from '../components/teacher/StudentGroups';
import { ConfigureTask } from '../components/teacher/ConfigureTask';
import { SubmissionsReview } from '../components/teacher/SubmissionsReview';
import { ExportReports } from '../components/teacher/ExportReports';

export const TeacherApp: React.FC = () => {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Routes>
        <Route path="/" element={<TeacherDashboard />} />
        <Route path="/groups" element={<StudentGroups />} />
        <Route path="/configure" element={<ConfigureTask />} />
        <Route path="/submissions" element={<SubmissionsReview />} />
        <Route path="/export" element={<ExportReports />} />
        <Route path="*" element={<Navigate to="/teacher" replace />} />
      </Routes>
    </div>
  );
};
