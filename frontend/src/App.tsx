import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { UpgradeWall } from './components/common/UpgradeWall';
import { ErrorBoundary } from './components/common/ErrorBoundary';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { Toast } from './components/common/Toast';
import { useNotificationStore } from './store/notificationStore';
import { useAuthStore } from './store/authStore';
import { useSSE } from './hooks/useSSE';

import { Landing } from './pages/Landing';
import { LoginPage } from './components/auth/LoginPage';
import { ForgotPassword } from './components/auth/ForgotPassword';
import { StudentApp } from './pages/StudentApp';
import { TeacherApp } from './pages/TeacherApp';
import { ProfilePage } from './pages/ProfilePage';
import { SubscriptionPage } from './pages/SubscriptionPage';
import { NotFound } from './pages/NotFound';

const AppContent: React.FC = () => {
  useSSE();
  const { toasts, removeToast } = useNotificationStore();

  return (
    <div className="flex flex-col min-h-screen bg-surface">
      <Navbar />

      {/* Floating Toast Notification Container */}
      <div className="fixed top-20 right-4 z-50 flex flex-col space-y-2 max-w-sm w-full pointer-events-none">
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto">
            <Toast
              id={toast.id}
              type={toast.type}
              message={toast.message}
              onClose={removeToast}
            />
          </div>
        ))}
      </div>

      <UpgradeWall />

      <main className="flex-1">
        <ErrorBoundary>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />

            {/* Student Portal Routes */}
            <Route
              path="/student/*"
              element={
                <ProtectedRoute allowedRoles={['STUDENT', 'ADMIN']}>
                  <StudentApp />
                </ProtectedRoute>
              }
            />

            {/* Teacher Portal Routes */}
            <Route
              path="/teacher/*"
              element={
                <ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']}>
                  <TeacherApp />
                </ProtectedRoute>
              }
            />

            {/* Profile & BYOK */}
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />

            {/* Subscription & Pricing */}
            <Route
              path="/subscription"
              element={
                <ProtectedRoute>
                  <SubscriptionPage />
                </ProtectedRoute>
              }
            />

            {/* 404 Catch-All */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </ErrorBoundary>
      </main>

      <Footer />
    </div>
  );
};

export const App: React.FC = () => {
  const { initAuth } = useAuthStore();

  useEffect(() => {
    initAuth();
  }, [initAuth]);

  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
};

export default App;
