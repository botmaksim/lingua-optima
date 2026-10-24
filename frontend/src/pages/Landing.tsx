/**
 * @file Landing.tsx
 * @brief Public landing page highlighting product capabilities, CEFR progression, and zero-retention OCR.
 */

import React from 'react';
import { Link } from 'react-router-dom';
import {
  Camera,
  Award,
  Zap,
  ShieldCheck,
  Users,
  ArrowRight,
  CheckCircle2,
  Cpu,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

/**
 * @brief Public marketing landing page introducing LinguaOptima features and CTA links.
 * @return JSX landing page element.
 */
export const Landing: React.FC = () => {
  const { user } = useAuth();

  const features = [
    {
      icon: Zap,
      title: 'Computerized Adaptive Testing (CAT)',
      description:
        'Dynamic difficulty adjustment that responds to your answers in real time, rapidly pinpointing your CEFR boundaries.',
    },
    {
      icon: Camera,
      title: 'Zero-Retention OCR',
      description:
        'Photograph handwritten homework and essays. Images are processed in RAM only and purged immediately to protect biometrics.',
    },
    {
      icon: Cpu,
      title: 'High-Throughput Diagnostic Engine',
      description:
        'Sub-second task analysis and grammar evaluation with automated multi-provider failover for uninterrupted practice.',
    },
    {
      icon: Award,
      title: 'Rigorous CEFR Rubrics',
      description:
        'IELTS-standard four-criterion evaluation: Task Achievement, Coherence & Cohesion, Lexical Resource, and Grammatical Accuracy.',
    },
    {
      icon: Users,
      title: 'Cohort-Isolated Leaderboards',
      description:
        'No toxic global leaderboards. Compete exclusively with your own classmates in teacher-supervised study groups.',
    },
    {
      icon: ShieldCheck,
      title: 'Bring Your Own API Key (BYOK)',
      description:
        'Supply your DeepSeek, Qwen, Gemini, GPT, Claude, or Groq key with client-side AES-256-GCM encryption.',
    },
  ];

  return (
    <div className="space-y-16 py-8 sm:py-14">
      <section className="text-center max-w-4xl mx-auto px-4 space-y-6">
        <div className="inline-flex items-center space-x-2 py-1.5 px-3.5 rounded-full bg-slate-100 border border-slate-200/90 text-slate-700 text-xs font-semibold shadow-xs">
          <ShieldCheck className="w-3.5 h-3.5 text-primary" />
          <span>Academic English Platform · CEFR Framework · Zero-Retention OCR</span>
        </div>

        <h1 className="text-4xl sm:text-6xl font-black text-slate-900 tracking-tight leading-tight">
          Master English with{' '}
          <span className="text-primary underline decoration-primary/30 decoration-wavy underline-offset-4">
            Adaptive Precision
          </span>{' '}
          and Privacy-First OCR
        </h1>

        <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto leading-relaxed">
          From handwritten essay corrections to adaptive computerized testing: Lingua Optima provides precise diagnostics, automated grading, and teacher cohort workflows.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3.5 pt-2">
          <Link
            to={user ? (user.role === 'TEACHER' ? '/teacher' : '/student') : '/login?register=true'}
            className="flex items-center space-x-2 py-3.5 px-7 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm transition shadow-xs"
          >
            <span>{user ? 'Open Dashboard' : 'Get Started'}</span>
            <ArrowRight className="w-4 h-4" />
          </Link>

          {!user && (
            <Link
              to="/login"
              className="py-3.5 px-7 rounded-xl bg-white hover:bg-slate-50 text-slate-800 font-semibold text-sm border border-slate-300 transition shadow-xs"
            >
              Sign In
            </Link>
          )}

          <a
            href="/presentation.html"
            target="_blank"
            rel="noopener noreferrer"
            className="py-3.5 px-7 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-sm border border-slate-200 transition"
          >
            Product Overview
          </a>
        </div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10">
        <div className="text-center space-y-1.5">
          <h2 className="text-2xl sm:text-3xl font-bold text-slate-900 tracking-tight">
            Engineered for Academic Rigor & Privacy
          </h2>
          <p className="text-sm text-slate-500 max-w-xl mx-auto">
            Strictly adhering to GDPR guidelines and CEFR language acquisition frameworks.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feat, idx) => {
            const Icon = feat.icon;
            return (
              <div
                key={idx}
                className="bg-white rounded-2xl p-6 sm:p-7 border border-slate-200/80 shadow-xs hover:border-slate-300 hover:shadow-sm transition space-y-3.5"
              >
                <div className="w-10 h-10 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center">
                  <Icon className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-slate-900 tracking-tight">{feat.title}</h3>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">{feat.description}</p>
              </div>
            );
          })}
        </div>
      </section>

      <section className="max-w-5xl mx-auto px-4">
        <div className="bg-slate-900 rounded-3xl p-8 sm:p-12 text-white shadow-2xl space-y-8">
          <div className="text-center space-y-2">
            <h2 className="text-2xl sm:text-3xl font-black tracking-tight">
              A Clear Path from B1 to C1 Fluency
            </h2>
            <p className="text-slate-400 text-sm">
              Our automated level-up engine suggests advancement once you master 85% of your level's curriculum.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <div className="bg-slate-800/80 p-6 rounded-2xl border border-slate-700/60 space-y-3">
              <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-emerald-950 text-emerald-400 border border-emerald-800">
                B1 Intermediate
              </span>
              <h3 className="text-base font-bold">Core Foundations</h3>
              <ul className="text-xs text-slate-400 space-y-1.5">
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /><span>Present Perfect vs Past</span></li>
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /><span>First & Second Conditionals</span></li>
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /><span>Basic Passive Voice</span></li>
              </ul>
            </div>

            <div className="bg-slate-800/80 p-6 rounded-2xl border border-slate-700/60 space-y-3">
              <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-sky-950 text-sky-400 border border-sky-800">
                B2 Upper Intermediate
              </span>
              <h3 className="text-base font-bold">Complex Structures</h3>
              <ul className="text-xs text-slate-400 space-y-1.5">
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-sky-400" /><span>Third & Mixed Conditionals</span></li>
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-sky-400" /><span>Inversion for Emphasis</span></li>
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-sky-400" /><span>Subjunctive & Wishes</span></li>
              </ul>
            </div>

            <div className="bg-slate-800/80 p-6 rounded-2xl border border-slate-700/60 space-y-3">
              <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-purple-950 text-purple-400 border border-purple-800">
                C1 Advanced
              </span>
              <h3 className="text-base font-bold">Nuance & Precision</h3>
              <ul className="text-xs text-slate-400 space-y-1.5">
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-400" /><span>Cleft Sentences</span></li>
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-400" /><span>Negative Adverbial Inversion</span></li>
                <li className="flex items-center space-x-1.5"><CheckCircle2 className="w-3.5 h-3.5 text-purple-400" /><span>Discourse & Nuance</span></li>
              </ul>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
