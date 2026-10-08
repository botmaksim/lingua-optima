import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Dashboard } from '../components/student/Dashboard';
import { GenerateTask } from '../components/student/GenerateTask';
import { TaskView } from '../components/student/TaskView';
import { AdaptiveSession } from '../components/student/AdaptiveSession';
import { OcrSubmit } from '../components/student/OcrSubmit';
import { EssayEditor } from '../components/student/EssayEditor';
import { AIReview } from '../components/student/AIReview';
import { MyUnits } from '../components/student/MyUnits';
import { Progress } from '../components/student/Progress';
import { GroupLeaderboard } from '../components/student/GroupLeaderboard';

export const StudentApp: React.FC = () => {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/generate" element={<GenerateTask />} />
        <Route path="/task/:taskId" element={<TaskView />} />
        <Route path="/session/:assignmentId?" element={<AdaptiveSession />} />
        <Route path="/ocr" element={<OcrSubmit />} />
        <Route path="/essay/:taskId?" element={<EssayEditor />} />
        <Route path="/review/:submissionId" element={<AIReview />} />
        <Route path="/my-units" element={<MyUnits />} />
        <Route path="/progress" element={<Progress />} />
        <Route path="/leaderboard" element={<GroupLeaderboard />} />
        <Route path="*" element={<Navigate to="/student" replace />} />
      </Routes>
    </div>
  );
};
