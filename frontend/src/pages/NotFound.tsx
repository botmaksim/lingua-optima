/**
 * @file NotFound.tsx
 * @brief 404 error fallback page with contextual redirection to role dashboard.
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Compass, Home, ArrowLeft } from 'lucide-react';
import { useAuthStore } from '../store/authStore';

/**
 * @brief 404 HTTP fallback component shown when requested route does not exist.
 * @return JSX 404 element.
 */
export const NotFound: React.FC = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  const handleGoHome = () => {
    if (!isAuthenticated) {
      navigate('/');
    } else if (user?.role === 'TEACHER') {
      navigate('/teacher');
    } else {
      navigate('/student');
    }
  };

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 text-center">
      <div className="w-20 h-20 bg-indigo-50 text-primary rounded-3xl flex items-center justify-center mb-6 shadow-sm border border-indigo-100">
        <Compass className="w-10 h-10 animate-pulse" />
      </div>

      <h1 className="text-6xl font-black text-slate-900 tracking-tight mb-2">404</h1>
      <h2 className="text-2xl font-bold text-slate-800 mb-3">Page Not Found</h2>
      <p className="text-slate-600 max-w-md mb-8 text-sm">
        The page you are looking for doesn't exist, has been removed, or you don't have permission to access it.
      </p>

      <div className="flex flex-col sm:flex-row items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl border border-slate-200 text-slate-700 bg-white hover:bg-slate-50 font-medium text-sm transition shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Go Back</span>
        </button>
        <button
          onClick={handleGoHome}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-primary text-white hover:bg-primary-hover font-medium text-sm transition shadow-sm shadow-indigo-100"
        >
          <Home className="w-4 h-4" />
          <span>Return to Dashboard</span>
        </button>
      </div>
    </div>
  );
};
