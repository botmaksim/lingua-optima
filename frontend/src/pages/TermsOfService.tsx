/**
 * @file TermsOfService.tsx
 * @brief Public Terms of Service agreement governing student and educator usage of Lingua Optima.
 */

import React from 'react';
import { Scale, CheckCircle2, AlertCircle, BookOpen, CreditCard, ShieldAlert, ArrowLeft, Mail } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * @brief Terms of Service page component detailing user obligations, licensing, and usage terms.
 * @return JSX terms of service view.
 */
export const TermsOfService: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Top Navigation & Header */}
      <div className="space-y-3">
        <Link
          to="/"
          className="inline-flex items-center space-x-1.5 text-xs font-bold text-slate-500 hover:text-primary transition"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Home</span>
        </Link>
        <div className="flex items-center space-x-3">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-primary">
            <Scale className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-3xl font-black text-slate-900 tracking-tight">Terms of Service</h1>
            <p className="text-xs text-slate-500 mt-0.5">Last revised: October 10, 2026 · Valid for all registered users</p>
          </div>
        </div>
      </div>

      {/* Highlights Card */}
      <div className="p-6 rounded-3xl bg-slate-900 text-white shadow-xl space-y-3">
        <h2 className="text-base font-bold text-white flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 text-emerald-400" />
          <span>Core Terms Summary</span>
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs text-slate-300">
          <div className="p-3 rounded-2xl bg-slate-800/80 border border-slate-700/60 space-y-1">
            <p className="font-bold text-white">Academic Integrity</p>
            <p>Users must submit original student work for diagnostic evaluation and honest learning.</p>
          </div>
          <div className="p-3 rounded-2xl bg-slate-800/80 border border-slate-700/60 space-y-1">
            <p className="font-bold text-white">Educator Authority</p>
            <p>Teachers hold full authority to review, override scores, and manage student enrollment.</p>
          </div>
          <div className="p-3 rounded-2xl bg-slate-800/80 border border-slate-700/60 space-y-1">
            <p className="font-bold text-white">Fair Usage Quotas</p>
            <p>Free and Pro tiers operate under daily evaluation limits to ensure platform stability.</p>
          </div>
        </div>
      </div>

      {/* Main Content Sections */}
      <div className="bg-white rounded-3xl p-6 sm:p-10 border border-slate-100 shadow-sm space-y-8 text-sm text-slate-600 leading-relaxed">
        {/* 1. Acceptance of Terms */}
        <section className="space-y-3">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-primary" />
            <span>1. Acceptance of Agreement</span>
          </h2>
          <p>
            By accessing or creating an account on Lingua Optima ("Platform"), you agree to be bound by these Terms of Service
            and our Privacy Policy. If you do not agree with any provision of these terms, you must refrain from using the platform.
          </p>
        </section>

        {/* 2. Educational Roles & Accounts */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Scale className="w-4 h-4 text-primary" />
            <span>2. Account Roles and Conduct</span>
          </h2>
          <p>
            Lingua Optima provides dedicated experiences for both Learners (Students) and Instructors (Teachers):
          </p>
          <ul className="list-disc pl-5 space-y-1.5 text-xs sm:text-sm text-slate-700">
            <li>
              <strong>Student Responsibilities:</strong> You agree not to automate exercise completions via third-party bots
              or circumvent question timers. Homework submissions should reflect your authentic language learning efforts.
            </li>
            <li>
              <strong>Educator Rights:</strong> Instructors have the right to organize student groups, assign syllabus modules,
              override machine scores, customize pedagogical feedback, and export academic performance reports.
            </li>
            <li>
              <strong>Credential Security:</strong> You are responsible for safeguarding your login credentials and notifying us
              immediately of any unauthorized access to your account.
            </li>
          </ul>
        </section>

        {/* 3. AI Diagnostic Disclaimers */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-primary" />
            <span>3. AI Diagnostic Evaluations & Pedagogical Advice</span>
          </h2>
          <p>
            Lingua Optima uses artificial intelligence models to score exercises, diagnose grammar weaknesses, and estimate CEFR proficiency levels.
            While our models strive for high grammatical accuracy, all AI feedback is provided as an educational aid and diagnostic recommendation.
            Final grading decisions and official certifications remain at the sole discretion of qualified educational institutions and human teachers.
          </p>
        </section>

        {/* 4. Subscriptions, Billing, and Fair Use */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <CreditCard className="w-4 h-4 text-primary" />
            <span>4. Subscriptions, Tier Quotas, and Fair Use</span>
          </h2>
          <p>
            Users on the Free tier receive daily evaluation allowances. Upgrading to a Pro subscription grants increased daily limits,
            advanced AI curriculum generation, and priority OCR processing.
          </p>
          <p className="text-xs sm:text-sm text-slate-700 bg-slate-50 p-4 rounded-2xl border border-slate-100">
            <strong>Cancellation & Renewals:</strong> Pro subscriptions renew automatically according to the chosen billing cycle.
            You may cancel your recurring subscription at any time via the Subscription page; your paid benefits will continue until the end of the active billing period.
          </p>
        </section>

        {/* 5. Intellectual Property */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-primary" />
            <span>5. Intellectual Property Rights</span>
          </h2>
          <p>
            All platform interfaces, proprietary algorithms, CEFR curriculum frameworks, and brand assets are the exclusive intellectual property of Lingua Optima.
            Students and teachers retain ownership over their respective written work and personal instructional notes submitted to the platform.
          </p>
        </section>

        {/* 6. Legal Contact */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Mail className="w-4 h-4 text-primary" />
            <span>6. Legal Inquiries</span>
          </h2>
          <p>
            If you have questions regarding these terms or wish to report a violation, please contact our legal counsel at:
          </p>
          <p className="text-primary font-mono font-bold text-xs">
            legal@linguaoptima.com
          </p>
        </section>
      </div>
    </div>
  );
};
