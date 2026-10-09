/**
 * @file LoginPage.tsx
 * @brief Authentication page supporting user sign-in and registration for students and teachers.
 */

import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { Mail, Lock, User, AlertCircle, Sparkles, GraduationCap } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief User login and account registration view with role selection.
 * @return JSX authentication element.
 */
export const LoginPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login, register, isLoading } = useAuthStore();

  const [isRegister, setIsRegister] = useState<boolean>(
    searchParams.get('register') === 'true'
  );
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>('STUDENT');
  const [error, setError] = useState<string | null>(null);

  /**
   * @brief Event handler or helper executing handle submit.
   */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    try {
      if (isRegister) {
        await register(email, password, fullName, role);
      } else {
        await login(email, password);
      }
      const currentUser = useAuthStore.getState().user;
      if (currentUser?.role === 'TEACHER') {
        navigate('/teacher');
      } else {
        navigate('/student');
      }
    } catch (err: any) {
      console.error('Auth failure:', err);
      setError(
        err.response?.data?.message ||
          (isRegister ? 'Registration failed. Please check your data.' : 'Invalid email or password.')
      );
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-8 sm:p-10 rounded-3xl shadow-xl shadow-slate-100 border border-slate-100">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-primary text-white font-black text-xl flex items-center justify-center mx-auto shadow-md shadow-indigo-100">
            LO
          </div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">
            {isRegister ? 'Create your account' : 'Welcome back'}
          </h2>
          <p className="text-xs text-slate-500">
            {isRegister
              ? 'Start mastering English with adaptive AI and zero-retention OCR'
              : 'Sign in to continue your personalized learning journey'}
          </p>
        </div>

        <div className="flex rounded-2xl bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => {
              setIsRegister(false);
              setError(null);
            }}
            className={`flex-1 py-2 text-xs font-bold rounded-xl transition ${
              !isRegister ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => {
              setIsRegister(true);
              setError(null);
            }}
            className={`flex-1 py-2 text-xs font-bold rounded-xl transition ${
              isRegister ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <>
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Full Name
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Alex Morgan"
                    className="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Account Role
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRole('STUDENT')}
                    className={`p-3 rounded-2xl border text-xs font-bold flex items-center justify-center space-x-1.5 transition ${
                      role === 'STUDENT'
                        ? 'border-primary bg-indigo-50 text-primary'
                        : 'border-slate-200 text-slate-600 hover:border-slate-300'
                    }`}
                  >
                    <GraduationCap className="w-4 h-4" />
                    <span>Student</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setRole('TEACHER')}
                    className={`p-3 rounded-2xl border text-xs font-bold flex items-center justify-center space-x-1.5 transition ${
                      role === 'TEACHER'
                        ? 'border-accent bg-sky-50 text-accent'
                        : 'border-slate-200 text-slate-600 hover:border-slate-300'
                    }`}
                  >
                    <Sparkles className="w-4 h-4" />
                    <span>Teacher</span>
                  </button>
                </div>
              </div>
            </>
          )}

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
              Email Address
            </label>
            <div className="relative">
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider">
                Password
              </label>
              {!isRegister && (
                <Link
                  to="/forgot-password"
                  className="text-xs text-primary hover:text-primary-hover font-semibold"
                >
                  Forgot password?
                </Link>
              )}
            </div>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="password"
                required
                minLength={6}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3.5 px-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100 disabled:opacity-50 mt-2 flex items-center justify-center"
          >
            {isLoading ? (
              <LoadingSpinner size="sm" className="p-0 text-white" />
            ) : isRegister ? (
              'Create Account'
            ) : (
              'Sign In'
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
