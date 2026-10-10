/**
 * @file HelpCenter.tsx
 * @brief Public Help Center and FAQ page featuring categorized accordions, interactive search, and educator support guidance.
 */

import React, { useState, useMemo } from 'react';
import {
  HelpCircle,
  Search,
  BookOpen,
  Users,
  Camera,
  ChevronDown,
  ChevronUp,
  CreditCard,
  Mail,
  ArrowLeft,
} from 'lucide-react';
import { Link } from 'react-router-dom';

interface FaqItem {
  id: string;
  category: 'students' | 'teachers' | 'billing' | 'general';
  question: string;
  answer: string;
  badge: string;
}

const FAQ_DATA: FaqItem[] = [
  {
    id: 'ocr-zero-retention',
    category: 'students',
    badge: 'OCR & Photos',
    question: 'How does Zero-Retention OCR work when I submit homework photos?',
    answer:
      'When you take a picture of your handwritten homework, the image is loaded into temporary RAM only. Our optical recognition engine extracts the written sentences and passes the text to the grading pipeline. The original photo binary is immediately discarded from memory and is never written to disk or database storage.',
  },
  {
    id: 'cefr-level-promotion',
    category: 'students',
    badge: 'Proficiency',
    question: 'How is my CEFR language level evaluated and promoted?',
    answer:
      'Your CEFR level (A1 to C2) is continuously calibrated by an adaptive computer-based algorithm (CAT). Each completed exercise measures accuracy across specific grammatical modules. Consistently achieving high accuracy on tasks at your current benchmark triggers a Level-Up notification and unlocks higher-tier content.',
  },
  {
    id: 'teacher-create-groups',
    category: 'teachers',
    badge: 'Cohorts',
    question: 'How do educators create cohort groups and invite students?',
    answer:
      'From the Teacher Dashboard, open "Student Cohorts" and create a named group (e.g. "Evening B2 Grammar"). You can add students instantly by their registered email address. If the student has not signed up yet, a pending invitation is saved and automatically connected when they register.',
  },
  {
    id: 'teacher-override-grades',
    category: 'teachers',
    badge: 'Grading',
    question: 'Can teachers adjust AI scores, edit AI feedback, and write custom guidance?',
    answer:
      'Yes! In the Submissions Review panel, teachers have complete control over evaluations. You can click "Override" to enter a custom numerical score, refine the AI diagnostic evaluation text, or write personalized pedagogical advice. Teachers can also edit feedback inline directly inside any expanded submission card.',
  },
  {
    id: 'export-reports',
    category: 'teachers',
    badge: 'Reporting',
    question: 'How do I download PDF or CSV group performance reports?',
    answer:
      'Navigate to the "Export Academic Reports" panel (or click "Export Report" inside any cohort group card). Select your target cohort and choose PDF or CSV format. Reports feature a complete breakdown of all assigned homeworks, student submission timestamps, scores, attempt counts, and teacher comments.',
  },
  {
    id: 'eco-mode-tasks',
    category: 'teachers',
    badge: 'Task Creation',
    question: 'What is Eco Mode when creating educational tasks?',
    answer:
      'Eco Mode optimizes AI model token generation parameters, allowing educators and students to generate concise, high-impact grammar exercises faster while consuming significantly fewer daily quota units.',
  },
  {
    id: 'free-vs-pro',
    category: 'billing',
    badge: 'Subscription',
    question: 'What is the difference between Free and Pro plans?',
    answer:
      'Free accounts receive daily exercise generation and evaluation allowances suitable for regular independent study. Pro subscribers enjoy significantly expanded daily limits, unlimited cohort sizes for teachers, advanced essay grading rubrics, and high-priority OCR processing.',
  },
  {
    id: 'google-oauth',
    category: 'general',
    badge: 'Authentication',
    question: 'Can I sign in using my Google account?',
    answer:
      'Yes. Lingua Optima provides seamless Google OAuth2 integration. Click "Continue with Google" on the Sign In page to securely access your student or teacher account without remembering a separate password.',
  },
];

/**
 * @brief Interactive Help Center page featuring searchable FAQs and categorized pedagogical advice.
 * @return JSX help center view.
 */
export const HelpCenter: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<'all' | 'students' | 'teachers' | 'billing'>('all');
  const [expandedFaqId, setExpandedFaqId] = useState<string | null>('ocr-zero-retention');

  const filteredFaqs = useMemo(() => {
    return FAQ_DATA.filter((item) => {
      const matchesCategory = activeCategory === 'all' || item.category === activeCategory;
      const q = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !q ||
        item.question.toLowerCase().includes(q) ||
        item.answer.toLowerCase().includes(q) ||
        item.badge.toLowerCase().includes(q);
      return matchesCategory && matchesSearch;
    });
  }, [searchQuery, activeCategory]);

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
            <HelpCircle className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-3xl font-black text-slate-900 tracking-tight">Help Center & Knowledge Base</h1>
            <p className="text-xs text-slate-500 mt-0.5">Frequently asked questions, student tutorials, and educator guides</p>
          </div>
        </div>
      </div>

      {/* Search Header Banner */}
      <div className="bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-800 rounded-3xl p-6 sm:p-8 text-white shadow-lg space-y-4">
        <div>
          <h2 className="text-xl font-black">How can we help you today?</h2>
          <p className="text-xs text-indigo-100 mt-1">
            Search our knowledge base for answers regarding OCR submissions, grading, cohorts, and billing.
          </p>
        </div>

        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-4 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search questions by keyword (e.g. OCR, CEFR, cohorts, override)..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-11 pr-4 py-3 rounded-2xl bg-white text-slate-900 placeholder:text-slate-400 text-xs sm:text-sm font-medium focus:outline-none focus:ring-4 focus:ring-indigo-300 shadow-sm"
          />
        </div>
      </div>

      {/* Category Filter Tabs */}
      <div className="flex flex-wrap items-center gap-2">
        {[
          { key: 'all', label: 'All Questions', icon: BookOpen },
          { key: 'students', label: 'For Students', icon: Camera },
          { key: 'teachers', label: 'For Educators', icon: Users },
          { key: 'billing', label: 'Plans & Billing', icon: CreditCard },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeCategory === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveCategory(tab.key as any)}
              className={`flex items-center space-x-1.5 px-4 py-2 rounded-2xl text-xs font-bold transition ${
                isActive
                  ? 'bg-primary text-white shadow-sm'
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* FAQ Accordions List */}
      <div className="space-y-3">
        {filteredFaqs.length === 0 ? (
          <div className="bg-white rounded-3xl p-10 text-center border border-slate-100 text-slate-400 space-y-2">
            <Search className="w-8 h-8 mx-auto text-slate-300" />
            <p className="font-bold text-slate-700 text-sm">No matching help articles found</p>
            <p className="text-xs">Try searching for broader keywords like "group", "grade", or "photo".</p>
          </div>
        ) : (
          filteredFaqs.map((faq) => {
            const isExpanded = expandedFaqId === faq.id;
            return (
              <div
                key={faq.id}
                className="bg-white rounded-2xl border border-slate-200/80 shadow-2xs overflow-hidden transition"
              >
                <button
                  type="button"
                  onClick={() => setExpandedFaqId(isExpanded ? null : faq.id)}
                  className="w-full p-4 sm:p-5 text-left flex items-center justify-between gap-4 hover:bg-slate-50/50 transition"
                >
                  <div className="flex items-center space-x-3 min-w-0">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-700 border border-indigo-200/60 shrink-0">
                      {faq.badge}
                    </span>
                    <h3 className="text-xs sm:text-sm font-bold text-slate-800 truncate">
                      {faq.question}
                    </h3>
                  </div>
                  <div className="text-slate-400 shrink-0">
                    {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                  </div>
                </button>

                {isExpanded && (
                  <div className="px-4 sm:px-5 pb-5 pt-1 text-xs sm:text-sm text-slate-600 leading-relaxed border-t border-slate-100 bg-slate-50/30 animate-in fade-in duration-150">
                    <p>{faq.answer}</p>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Contact Support Footer Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-6">
        <div className="space-y-1 text-center sm:text-left">
          <h3 className="text-base font-bold text-slate-900">Still have questions?</h3>
          <p className="text-xs text-slate-500">
            Our pedagogical engineering team is ready to assist with customized institutional deployments.
          </p>
        </div>

        <a
          href="mailto:support@linguaoptima.com"
          className="inline-flex items-center space-x-2 px-5 py-3 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs sm:text-sm transition shadow-sm"
        >
          <Mail className="w-4 h-4" />
          <span>Contact Support</span>
        </a>
      </div>
    </div>
  );
};
