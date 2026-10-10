/**
 * @file LoginPage.tsx
 * @brief Authentication page supporting email/password and Google OAuth2 sign-in and registration.
 */

import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import {
  Mail,
  Lock,
  User,
  AlertCircle,
  Sparkles,
  GraduationCap,
  ArrowLeft,
  CheckCircle2,
} from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/authApi';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief User login and account registration view with email/password, email verification, and Google OAuth2 support.
 * @return JSX authentication element.
 */
export const LoginPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { login, register, googleLogin, isLoading } = useAuthStore();

  const [isRegister, setIsRegister] = useState<boolean>(
    searchParams.get('register') === 'true'
  );
  const [regStep, setRegStep] = useState<'DETAILS' | 'VERIFY_CODE'>('DETAILS');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<'STUDENT' | 'TEACHER'>('STUDENT');
  const [verificationCode, setVerificationCode] = useState('');
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [gisReady, setGisReady] = useState<boolean>(false);
  const googleBtnRef = React.useRef<HTMLDivElement>(null);

  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  /**
   * @brief Initializes Google Identity Services and renders the official, mobile-compatible Sign-In button.
   */
  useEffect(() => {
    if (!googleClientId || typeof document === 'undefined') return;

    const renderGoogleBtn = () => {
      if (!window.google?.accounts?.id || !googleBtnRef.current) return;
      try {
        window.google.accounts.id.initialize({
          client_id: googleClientId,
          callback: async (response) => {
            if (!response?.credential) return;
            try {
              setError(null);
              await googleLogin(response.credential, role);
              redirectAuthenticatedUser();
            } catch (err: any) {
              console.error('Google OAuth failure:', err);
              setError(err.response?.data?.message || 'Google authentication failed.');
            }
          },
          auto_select: false,
          cancel_on_tap_outside: true,
        });

        const container = googleBtnRef.current;
        const targetWidth = Math.min(380, Math.max(250, container.clientWidth || 320));
        container.innerHTML = '';
        window.google.accounts.id.renderButton(container, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          logo_alignment: 'left',
          width: targetWidth,
        });
        setGisReady(true);
      } catch (err) {
        console.warn('GIS render error:', err);
      }
    };

    if (window.google?.accounts?.id) {
      renderGoogleBtn();
      return;
    }

    const checkInterval = setInterval(() => {
      if (window.google?.accounts?.id) {
        renderGoogleBtn();
        clearInterval(checkInterval);
      }
    }, 150);

    const timeout = setTimeout(() => {
      clearInterval(checkInterval);
    }, 4000);

    return () => {
      clearInterval(checkInterval);
      clearTimeout(timeout);
    };
  }, [googleClientId, role, isRegister]);

  /**
   * @brief Redirects the authenticated user to the appropriate Student or Teacher dashboard.
   */
  const redirectAuthenticatedUser = () => {
    const currentUser = useAuthStore.getState().user;
    if (currentUser?.role === 'TEACHER') {
      navigate('/teacher');
    } else {
      navigate('/student');
    }
  };

  /**
   * @brief Dispatches email verification code for new user registration.
   * @param e React form submission event.
   */
  const handleSendVerificationCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    const cleanEmail = email.toLowerCase().trim();
    if (!cleanEmail || !password || !fullName.trim()) {
      setError('Please fill in all required fields.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setIsSendingCode(true);
    try {
      await authApi.sendVerificationCode(cleanEmail);
      setRegStep('VERIFY_CODE');
      setResendCooldown(60);
      setSuccessMsg(`A 6-digit confirmation code was sent to ${cleanEmail}`);
    } catch (err: any) {
      console.error('Send verification code failure:', err);
      setError(
        err.response?.data?.message ||
          'Failed to send verification code. Please check your email or try again.'
      );
    } finally {
      setIsSendingCode(false);
    }
  };

  /**
   * @brief Resends verification code with cooldown restriction.
   */
  const handleResendCode = async () => {
    if (resendCooldown > 0 || isSendingCode) return;
    setError(null);
    setSuccessMsg(null);
    setIsSendingCode(true);
    try {
      await authApi.sendVerificationCode(email.toLowerCase().trim());
      setResendCooldown(60);
      setSuccessMsg(`New code sent to ${email.toLowerCase().trim()}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to resend verification code.');
    } finally {
      setIsSendingCode(false);
    }
  };

  /**
   * @brief Completes registration after verifying confirmation code.
   * @param e React form submission event.
   */
  const handleCompleteRegistration = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanCode = verificationCode.trim();
    if (cleanCode.length !== 6) {
      setError('Please enter the 6-digit verification code.');
      return;
    }

    try {
      await register(email.toLowerCase().trim(), password, fullName.trim(), role, cleanCode);
      redirectAuthenticatedUser();
    } catch (err: any) {
      console.error('Registration failure:', err);
      setError(
        err.response?.data?.message ||
          'Invalid or expired verification code. Please try again.'
      );
    }
  };

  /**
   * @brief Handles standard email and password sign-in form submission.
   * @param e React form submission event.
   */
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    try {
      await login(email.toLowerCase().trim(), password);
      redirectAuthenticatedUser();
    } catch (err: any) {
      console.error('Login failure:', err);
      setError(err.response?.data?.message || 'Invalid email or password.');
    }
  };

  /**
   * @brief Initiates Google OAuth2 sign-in using Google Identity Services and exchanges the ID token with the backend.
   */
  const handleGoogleSignIn = () => {
    setError(null);
    if (!googleClientId) {
      setError('Google OAuth Client ID (VITE_GOOGLE_CLIENT_ID) is not configured in .env');
      return;
    }

    if (!window.google?.accounts?.id) {
      setError('Google Identity Services is still loading. Please try again in a moment.');
      return;
    }

    document.cookie = 'g_state=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    window.google.accounts.id.initialize({
      client_id: googleClientId,
      callback: async (response) => {
        try {
          await googleLogin(response.credential, role);
          redirectAuthenticatedUser();
        } catch (err: any) {
          console.error('Google OAuth failure:', err);
          setError(err.response?.data?.message || 'Google authentication failed.');
        }
      },
    });
    window.google.accounts.id.prompt();
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-6 bg-white p-6 sm:p-8 rounded-3xl shadow-xl shadow-slate-200/60 border border-slate-100">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-indigo-600 to-indigo-500 text-white font-black text-xl flex items-center justify-center mx-auto shadow-md shadow-indigo-100">
            LO
          </div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">
            {isRegister
              ? regStep === 'VERIFY_CODE'
                ? 'Confirm your email'
                : 'Create your account'
              : 'Welcome back'}
          </h2>
          <p className="text-xs text-slate-500">
            {isRegister
              ? regStep === 'VERIFY_CODE'
                ? 'Enter the 6-digit confirmation code sent to your email address'
                : 'Start mastering English with adaptive AI and zero-retention OCR'
              : 'Sign in to continue your personalized learning journey'}
          </p>
        </div>

        <div className="flex rounded-2xl bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => {
              setIsRegister(false);
              setRegStep('DETAILS');
              setError(null);
              setSuccessMsg(null);
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
              setRegStep('DETAILS');
              setError(null);
              setSuccessMsg(null);
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

        {successMsg && !error && (
          <div className="p-3.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        {isRegister && regStep === 'VERIFY_CODE' ? (
          <form onSubmit={handleCompleteRegistration} className="space-y-4">
            <div className="text-center p-4 rounded-2xl bg-indigo-50/70 border border-indigo-100">
              <div className="w-10 h-10 rounded-xl bg-indigo-100 text-primary flex items-center justify-center mx-auto mb-2">
                <Mail className="w-5 h-5" />
              </div>
              <p className="text-xs text-slate-600">
                Code sent from <span className="font-semibold text-slate-900">mrartissite@gmail.com</span> to:
              </p>
              <p className="text-xs font-bold text-primary mt-0.5 truncate">{email}</p>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2 text-center">
                6-Digit Confirmation Code
              </label>
              <input
                type="text"
                required
                maxLength={6}
                autoFocus
                value={verificationCode}
                onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ''))}
                placeholder="123456"
                className="w-full text-center tracking-[0.4em] font-mono text-2xl py-3 rounded-2xl bg-slate-50 border border-slate-200 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-bold"
              />
            </div>

            <button
              type="submit"
              disabled={isLoading || verificationCode.length !== 6}
              className="w-full py-3.5 px-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100 disabled:opacity-50 mt-2 flex items-center justify-center space-x-2"
            >
              {isLoading ? (
                <LoadingSpinner size="sm" className="p-0 text-white" />
              ) : (
                <span>Confirm & Complete Registration</span>
              )}
            </button>

            <div className="flex items-center justify-between pt-2 text-xs">
              <button
                type="button"
                onClick={() => {
                  setRegStep('DETAILS');
                  setError(null);
                  setSuccessMsg(null);
                }}
                className="text-slate-500 hover:text-slate-800 font-medium flex items-center space-x-1"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Edit details</span>
              </button>

              <button
                type="button"
                disabled={resendCooldown > 0 || isSendingCode}
                onClick={handleResendCode}
                className="text-primary hover:text-primary-hover font-bold disabled:text-slate-400 disabled:cursor-not-allowed"
              >
                {resendCooldown > 0 ? (
                  <span>Resend in {resendCooldown}s</span>
                ) : (
                  <span>Resend code</span>
                )}
              </button>
            </div>
          </form>
        ) : (
          <form onSubmit={isRegister ? handleSendVerificationCode : handleLoginSubmit} className="space-y-4">
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
              disabled={isLoading || isSendingCode}
              className="w-full py-3.5 px-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100 disabled:opacity-50 mt-2 flex items-center justify-center space-x-2"
            >
              {isSendingCode ? (
                <>
                  <LoadingSpinner size="sm" className="p-0 text-white" />
                  <span>Sending confirmation code...</span>
                </>
              ) : isLoading ? (
                <LoadingSpinner size="sm" className="p-0 text-white" />
              ) : isRegister ? (
                <span>Send Verification Code & Continue</span>
              ) : (
                <span>Sign In with Email</span>
              )}
            </button>
          </form>
        )}

        <div className="relative flex items-center justify-center my-2">
          <div className="border-t border-slate-200 w-full" />
          <span className="bg-white px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 whitespace-nowrap">
            Or continue with
          </span>
          <div className="border-t border-slate-200 w-full" />
        </div>

        <div className="w-full flex flex-col items-center justify-center min-h-[44px]">
          {/* Official Google Identity Services button container (renders iframe when GIS loaded) */}
          <div
            ref={googleBtnRef}
            className={`w-full flex justify-center items-center ${gisReady ? 'block' : 'hidden'}`}
          />

          {/* Interactive fallback / initial button (active while GIS is loading or in test environment) */}
          {!gisReady && (
            <button
              type="button"
              onClick={handleGoogleSignIn}
              disabled={isLoading}
              data-testid="google-oauth-button"
              className="w-full py-3 px-4 rounded-full bg-white hover:bg-slate-50 active:bg-slate-100 text-slate-700 font-semibold text-sm border border-slate-300 transition-all shadow-sm flex items-center justify-center space-x-3 disabled:opacity-50"
            >
              <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="#4285F4"
                  d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"
                />
                <path
                  fill="#34A853"
                  d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.11-6.72-4.96H1.29v3.14C3.26 21.3 7.31 24 12 24z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.28 14.24c-.24-.72-.38-1.49-.38-2.24s.14-1.52.38-2.24V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.99-3.14z"
                />
                <path
                  fill="#EA4335"
                  d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.99 3.14c.95-2.85 3.6-4.96 6.72-4.96z"
                />
              </svg>
              <span>Continue with Google</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

