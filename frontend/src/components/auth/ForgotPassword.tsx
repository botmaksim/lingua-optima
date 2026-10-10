/**
 * @file ForgotPassword.tsx
 * @brief Self-service password recovery component with 6-digit email verification and password reset.
 */

import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowLeft, CheckCircle2, Lock, KeyRound, Eye, EyeOff, ShieldCheck, AlertCircle } from 'lucide-react';
import { authApi } from '../../api/authApi';
import { LoadingSpinner } from '../common/LoadingSpinner';

type ResetStep = 'REQUEST_CODE' | 'VERIFY_RESET' | 'SUCCESS';

/**
 * @brief ForgotPassword page component providing a 2-step verification code password reset flow.
 * @return React component element.
 */
export const ForgotPassword: React.FC = () => {
  const [step, setStep] = useState<ResetStep>('REQUEST_CODE');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  /**
   * @brief Dispatches a 6-digit recovery code to the provided email.
   * @param e React form submission event.
   */
  const handleRequestCode = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanEmail = email.toLowerCase().trim();
    if (!cleanEmail) {
      setError('Please enter your email address.');
      return;
    }

    setError(null);
    setSuccessMsg(null);
    setIsLoading(true);

    try {
      await authApi.forgotPassword(cleanEmail);
      setStep('VERIFY_RESET');
      setResendCooldown(60);
      setSuccessMsg(`We sent a 6-digit recovery code to ${cleanEmail}`);
    } catch (err: any) {
      console.error('Password reset request error:', err);
      // Even if user not found, backend handles safely, but display error if rate-limited
      const message = err.response?.data?.message;
      if (err.response?.status === 429) {
        setError(message || 'Too many requests. Please wait before trying again.');
      } else {
        // Safe transition: avoid leaking user existence, still let them proceed to code entry
        setStep('VERIFY_RESET');
        setResendCooldown(60);
        setSuccessMsg(`If an account exists for ${cleanEmail}, a recovery code has been sent.`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Resends the 6-digit recovery code.
   */
  const handleResendCode = async () => {
    if (resendCooldown > 0 || isResending) return;
    const cleanEmail = email.toLowerCase().trim();
    if (!cleanEmail) return;

    setError(null);
    setSuccessMsg(null);
    setIsResending(true);

    try {
      await authApi.forgotPassword(cleanEmail);
      setResendCooldown(60);
      setSuccessMsg(`New recovery code dispatched to ${cleanEmail}`);
    } catch (err: any) {
      console.error('Resend recovery code error:', err);
      setError(err.response?.data?.message || 'Failed to resend code. Please try again later.');
    } finally {
      setIsResending(false);
    }
  };

  /**
   * @brief Submits the verification code and new password to reset the account credentials.
   * @param e React form submission event.
   */
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);

    const cleanEmail = email.toLowerCase().trim();
    const cleanCode = code.trim();

    if (!cleanCode || cleanCode.length !== 6) {
      setError('Please enter the full 6-digit verification code.');
      return;
    }

    if (!newPassword || newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match. Please re-enter.');
      return;
    }

    setIsLoading(true);

    try {
      await authApi.resetPassword({
        email: cleanEmail,
        code: cleanCode,
        newPassword,
      });
      setStep('SUCCESS');
    } catch (err: any) {
      console.error('Password reset completion error:', err);
      setError(
        err.response?.data?.message ||
          'Invalid or expired recovery code. Please check your code or request a new one.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-6 bg-white p-8 sm:p-10 rounded-3xl shadow-xl shadow-slate-100 border border-slate-100">
        <div>
          {step !== 'SUCCESS' && (
            <Link
              to="/login"
              className="inline-flex items-center space-x-1.5 text-xs font-bold text-slate-500 hover:text-slate-800 transition mb-6"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Sign In</span>
            </Link>
          )}

          {step === 'REQUEST_CODE' && (
            <>
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center mb-4">
                <KeyRound className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-black text-slate-900 tracking-tight">Forgot Password?</h2>
              <p className="text-xs text-slate-500 mt-1">
                Enter your account email and we'll send you a 6-digit recovery code to reset your password.
              </p>
            </>
          )}

          {step === 'VERIFY_RESET' && (
            <>
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center mb-4">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h2 className="text-2xl font-black text-slate-900 tracking-tight">Set New Password</h2>
              <p className="text-xs text-slate-500 mt-1">
                Enter the 6-digit code sent to <span className="font-semibold text-slate-800">{email}</span> and pick a new password.
              </p>
            </>
          )}
        </div>

        {error && (
          <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-2xl flex items-start space-x-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
            <span className="font-medium">{error}</span>
          </div>
        )}

        {successMsg && step !== 'SUCCESS' && (
          <div className="p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-2xl flex items-start space-x-2">
            <CheckCircle2 className="w-4 h-4 flex-shrink-0 mt-0.5 text-emerald-600" />
            <span className="font-medium">{successMsg}</span>
          </div>
        )}

        {step === 'REQUEST_CODE' && (
          <form onSubmit={handleRequestCode} className="space-y-4">
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

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3.5 px-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" className="p-0 text-white" />
                  <span>Sending recovery code...</span>
                </>
              ) : (
                <span>Send Recovery Code</span>
              )}
            </button>
          </form>
        )}

        {step === 'VERIFY_RESET' && (
          <form onSubmit={handleResetPassword} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                6-Digit Recovery Code
              </label>
              <div className="relative">
                <KeyRound className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  maxLength={6}
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                  placeholder="123456"
                  className="w-full pl-10 pr-4 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm font-mono tracking-widest focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                New Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  className="w-full pl-10 pr-10 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                Confirm New Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Repeat new password"
                  className="w-full pl-10 pr-10 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                  tabIndex={-1}
                >
                  {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3.5 px-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100 disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" className="p-0 text-white" />
                  <span>Updating password...</span>
                </>
              ) : (
                <span>Reset Password & Continue</span>
              )}
            </button>

            <div className="flex items-center justify-between pt-2 text-xs">
              <button
                type="button"
                onClick={() => {
                  setStep('REQUEST_CODE');
                  setError(null);
                  setSuccessMsg(null);
                }}
                className="text-slate-500 hover:text-slate-800 font-medium flex items-center space-x-1"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Change email</span>
              </button>

              <button
                type="button"
                disabled={resendCooldown > 0 || isResending}
                onClick={handleResendCode}
                className="text-primary hover:text-primary-hover font-bold disabled:text-slate-400 disabled:cursor-not-allowed"
              >
                {resendCooldown > 0 ? (
                  <span>Resend in {resendCooldown}s</span>
                ) : isResending ? (
                  <span>Sending...</span>
                ) : (
                  <span>Resend code</span>
                )}
              </button>
            </div>
          </form>
        )}

        {step === 'SUCCESS' && (
          <div className="text-center space-y-4 py-4">
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-inner">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div>
              <h3 className="text-xl font-black text-slate-900">Password Reset Successful!</h3>
              <p className="text-xs text-slate-600 mt-2 leading-relaxed">
                Your password has been securely updated. You can now log into your account using your new credentials.
              </p>
            </div>

            <div className="pt-4">
              <Link
                to="/login"
                className="w-full inline-flex items-center justify-center py-3.5 px-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100"
              >
                Proceed to Sign In
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
