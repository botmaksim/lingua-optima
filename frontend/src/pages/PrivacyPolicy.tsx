/**
 * @file PrivacyPolicy.tsx
 * @brief Public Privacy Policy page highlighting Zero-Retention OCR architecture, AI evaluation practices, and GDPR compliance.
 */

import React from 'react';
import { ShieldCheck, EyeOff, Cpu, Lock, Database, UserCheck, ArrowLeft, Mail } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * @brief Privacy Policy page component detailing data security and zero-retention policies.
 * @return JSX privacy view.
 */
export const PrivacyPolicy: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Top Breadcrumb & Title */}
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
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-3xl font-black text-slate-900 tracking-tight">Privacy Policy</h1>
            <p className="text-xs text-slate-500 mt-0.5">Last updated: October 10, 2026 · Effective immediately</p>
          </div>
        </div>
      </div>

      {/* Zero-Retention Banner Callout */}
      <div className="p-6 rounded-3xl bg-gradient-to-br from-indigo-500/10 via-purple-500/5 to-white border border-indigo-200/80 shadow-sm space-y-2.5">
        <div className="flex items-center space-x-2 text-primary font-bold text-sm">
          <EyeOff className="w-5 h-5 text-indigo-600 shrink-0" />
          <span>Zero-Retention OCR Architecture Guarantee</span>
        </div>
        <p className="text-xs sm:text-sm text-slate-700 leading-relaxed">
          Lingua Optima enforces a strict zero-retention policy for student handwriting submissions.
          Uploaded homework images are processed entirely in ephemeral volatile memory (RAM) by our optical character recognition
          engine and are <strong>instantly purged</strong> from our servers after text extraction. No image binaries, camera metadata,
          or biometric handwriting photos are ever stored on disk or databases.
        </p>
      </div>

      {/* Main Sections */}
      <div className="bg-white rounded-3xl p-6 sm:p-10 border border-slate-100 shadow-sm space-y-8 text-sm text-slate-600 leading-relaxed">
        {/* 1. Information Collection */}
        <section className="space-y-3">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Database className="w-4 h-4 text-primary" />
            <span>1. Information We Collect</span>
          </h2>
          <p>
            When you register and use Lingua Optima, we collect the minimum necessary data to deliver personalized adaptive English instruction:
          </p>
          <ul className="list-disc pl-5 space-y-1.5 text-xs sm:text-sm text-slate-700">
            <li>
              <strong>Account Information:</strong> Name, email address, password hash (salted via BCrypt), and optional Google OAuth profile metadata.
            </li>
            <li>
              <strong>Learning & Evaluation Data:</strong> Student answers to exercises, grammar error logs, estimated CEFR proficiency benchmarks, and diagnostic progress metrics.
            </li>
            <li>
              <strong>Educator Records:</strong> Cohort group structures, assignments, manual grade overrides, and custom instructional feedback.
            </li>
            <li>
              <strong>Technical Telemetry:</strong> Anonymized session logs and browser environment details required for secure JWT session authentication.
            </li>
          </ul>
        </section>

        {/* 2. Artificial Intelligence Processing */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Cpu className="w-4 h-4 text-primary" />
            <span>2. Artificial Intelligence & Diagnostic Processing</span>
          </h2>
          <p>
            Lingua Optima employs state-of-the-art language models (Groq Llama 3 and Google Gemini) to generate contextual pedagogical exercises,
            perform automated essay evaluation, and synthesize granular grammatical recommendations.
          </p>
          <p className="text-xs sm:text-sm text-slate-700 bg-slate-50 p-4 rounded-2xl border border-slate-100">
            <strong>Privacy Assurance:</strong> Text submitted for automated grading is transmitted via secure TLS-encrypted API tunnels.
            We do not share your private student essays or personal identity with public AI training corpuses.
          </p>
        </section>

        {/* 3. Data Protection and Security */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Lock className="w-4 h-4 text-primary" />
            <span>3. Data Storage & Cryptographic Security</span>
          </h2>
          <p>
            All persistent platform records are hosted within secured PostgreSQL databases protected by role-based access controls,
            firewalls, and encrypted connections. Authentication sessions utilize tamper-proof JSON Web Tokens (JWT) signed with high-entropy secrets.
          </p>
        </section>

        {/* 4. Student & Educator Rights */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <UserCheck className="w-4 h-4 text-primary" />
            <span>4. Your Data Rights (GDPR & CCPA Compliance)</span>
          </h2>
          <p>
            Regardless of your geographic location, you retain full ownership of your educational data. You have the right to:
          </p>
          <ul className="list-disc pl-5 space-y-1.5 text-xs sm:text-sm text-slate-700">
            <li>Request a full export of your learning history and assignment submissions in machine-readable format (CSV/PDF).</li>
            <li>Modify or update your personal account details, including your displayed roster name.</li>
            <li>Request complete deletion of your account and associated historical submission records.</li>
          </ul>
        </section>

        {/* 5. Contact Information */}
        <section className="space-y-3 pt-6 border-t border-slate-100">
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Mail className="w-4 h-4 text-primary" />
            <span>5. Contact Our Privacy Team</span>
          </h2>
          <p>
            For privacy inquiries, data subject access requests, or security vulnerability disclosures, please contact our data governance team at:
          </p>
          <p className="text-primary font-mono font-bold text-xs">
            privacy@linguaoptima.com
          </p>
        </section>
      </div>
    </div>
  );
};
