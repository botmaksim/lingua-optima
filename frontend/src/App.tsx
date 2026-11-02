/**
 * @file App.tsx
 * @brief Top-level application routing layout, authentication initialization, and modal provider.
 */

import React, { useEffect, Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Navbar } from './components/common/Navbar';
import { Footer } from './components/common/Footer';
import { MobileBottomNav } from './components/common/MobileBottomNav';
import { UpgradeWall } from './components/common/UpgradeWall';
import { ErrorBoundary } from './components/common/ErrorBoundary';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { Toast } from './components/common/Toast';
import { LoadingSpinner } from './components/common/LoadingSpinner';
import { useNotificationStore } from './store/notificationStore';
import { useAuthStore } from './store/authStore';
import { useSSE } from './hooks/useSSE';

const Landing = lazy(() => import('./pages/Landing').then((m) => ({ default: m.Landing })));
const LoginPage = lazy(() => import('./components/auth/LoginPage').then((m) => ({ default: m.LoginPage })));
const ForgotPassword = lazy(() => import('./components/auth/ForgotPassword').then((m) => ({ default: m.ForgotPassword })));
const StudentApp = lazy(() => import('./pages/StudentApp').then((m) => ({ default: m.StudentApp })));
const TeacherApp = lazy(() => import('./pages/TeacherApp').then((m) => ({ default: m.TeacherApp })));
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((m) => ({ default: m.ProfilePage })));
const SubscriptionPage = lazy(() => import('./pages/SubscriptionPage').then((m) => ({ default: m.SubscriptionPage })));
const PrivacyPolicy = lazy(() => import('./pages/PrivacyPolicy').then((m) => ({ default: m.PrivacyPolicy })));
const TermsOfService = lazy(() => import('./pages/TermsOfService').then((m) => ({ default: m.TermsOfService })));
const HelpCenter = lazy(() => import('./pages/HelpCenter').then((m) => ({ default: m.HelpCenter })));
const NotFound = lazy(() => import('./pages/NotFound').then((m) => ({ default: m.NotFound })));

/**
 * @brief Inner shell managing navigation routes, toasts, upgrade wall modals, and SSE events.
 * @return JSX main layout.
 */
const AppContent: React.FC = () => {
  useSSE();
  const { toasts, removeToast } = useNotificationStore();

  return (
    <div className="flex flex-col min-h-screen bg-surface">
      <Navbar />

      <div className="fixed top-20 right-4 sm:right-6 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none">
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto">
            <Toast
              id={toast.id}
              type={toast.type}
              title={toast.title}
              message={toast.message}
              onClose={removeToast}
            />
          </div>
        ))}
      </div>

      <UpgradeWall />

      <main className="flex-1 pb-20 md:pb-0">
        <ErrorBoundary>
          <Suspense fallback={<div className="flex items-center justify-center min-h-[50vh]"><LoadingSpinner size="lg" message="Loading..." /></div>}>
            <Routes>
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/privacy" element={<PrivacyPolicy />} />
              <Route path="/terms" element={<TermsOfService />} />
              <Route path="/help" element={<HelpCenter />} />

              <Route
                path="/student/*"
                element={
                  <ProtectedRoute allowedRoles={['STUDENT', 'ADMIN']}>
                    <StudentApp />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/teacher/*"
                element={
                  <ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']}>
                    <TeacherApp />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/subscription"
                element={
                  <ProtectedRoute>
                    <SubscriptionPage />
                  </ProtectedRoute>
                }
              />

              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
        </ErrorBoundary>
      </main>

      <Footer />
      <MobileBottomNav />
    </div>
  );
};

/**
 * @brief Application root wrapped in browser history router and auth session bootstrap.
 * @return JSX top-level application element.
 */
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
