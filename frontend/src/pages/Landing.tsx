/**
 * @file Landing.tsx
 * @brief Public landing page highlighting product capabilities, CEFR progression, and zero-retention OCR.
 */

import React from 'react';
import { Link } from 'react-router-dom';
import {
  Sparkles,
  Camera,
  Award,
  Zap,
  ShieldCheck,
  Users,
  ArrowRight,
  CheckCircle2,
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
      icon: Sparkles,
      title: 'AI Fallback Chain',
      description:
        'Ultra-fast generation powered by Groq Llama 3.1 with seamless automatic fallback to Gemini 1.5 Flash.',
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
        'Optionally supply your OpenAI, Anthropic, Gemini, or Groq API key, encrypted safely at rest with AES-256-GCM.',
    },
  ];

  return (
    <div className="space-y-20 py-10 sm:py-16">
      <section className="text-center max-w-4xl mx-auto px-4 space-y-6">
        <div className="inline-flex items-center space-x-2 py-1.5 px-3.5 rounded-full bg-indigo-50 border border-indigo-200 text-primary text-xs font-bold shadow-sm">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Next-Generation Adaptive English Platform</span>
        </div>

        <h1 className="text-4xl sm:text-6xl font-black text-slate-900 tracking-tight leading-tight">
          Master English with{' '}
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary to-accent">
            Intelligent AI
          </span>{' '}
          and Privacy-First OCR
        </h1>

        <p className="text-base sm:text-lg text-slate-600 max-w-2xl mx-auto leading-relaxed">
          From handwritten essay corrections to adaptive computerized testing: Lingua Optima provides precise diagnostics, automated grading, and teacher cohort workflows.
        </p>

        <div className="flex flex-wrap items-center justify-center gap-4 pt-4">
          <Link
            to={user ? (user.role === 'TEACHER' ? '/teacher' : '/student') : '/login?register=true'}
            className="flex items-center space-x-2 py-4 px-8 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-lg shadow-indigo-200"
          >
            <span>{user ? 'Open Dashboard' : 'Get Started Free'}</span>
            <ArrowRight className="w-4 h-4" />
          </Link>

          {!user && (
            <Link
              to="/login"
              className="py-4 px-8 rounded-2xl bg-white hover:bg-slate-50 text-slate-800 font-bold text-sm border border-slate-200 transition shadow-sm"
            >
              Sign In
            </Link>
          )}

          <a
            href="/presentation.html"
            target="_blank"
            rel="noopener noreferrer"
            className="py-4 px-8 rounded-2xl bg-indigo-50 hover:bg-indigo-100 text-primary font-bold text-sm border border-indigo-200 transition shadow-sm"
          >
            View Presentation
          </a>
        </div>
      </section>

      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center space-y-2">
          <h2 className="text-3xl font-black text-slate-900 tracking-tight">
            Engineered for Academic Rigor & Privacy
          </h2>
          <p className="text-sm text-slate-500 max-w-xl mx-auto">
            Strictly adhering to GDPR guidelines and CEFR language acquisition frameworks.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {features.map((feat, idx) => {
            const Icon = feat.icon;
            return (
              <div
                key={idx}
                className="bg-white rounded-3xl p-8 border border-slate-100 shadow-sm hover:shadow-md transition space-y-4"
              >
                <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center">
                  <Icon className="w-6 h-6" />
                </div>
                <h3 className="text-lg font-bold text-slate-900">{feat.title}</h3>
                <p className="text-sm text-slate-600 leading-relaxed">{feat.description}</p>
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
