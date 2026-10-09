/**
 * @file ProfilePage.tsx
 * @brief User account profile, CEFR status, and BYOK AES-256 encrypted API key management page.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Key,
  ShieldCheck,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Cpu,
  GraduationCap,
  School,
  BookOpen,
  Sparkles,
  HelpCircle,
  ArrowRight,
  Edit2,
  Check,
  X,
} from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { apiKeyApi, ApiKeyItem } from '../api/apiKeyApi';
import { CefrBadge } from '../components/common/CefrBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { CefrLevel, Role } from '../types/user';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  useProviderModels,
} from '../constants/aiModels';

const CEFR_LEVEL_METADATA: Record<CefrLevel, { title: string; desc: string }> = {
  A1: { title: 'Beginner', desc: 'Basic vocabulary, simple present structures, and fundamental everyday expressions.' },
  A2: { title: 'Elementary', desc: 'Past simple, basic future plans, everyday social exchanges and routines.' },
  B1: { title: 'Intermediate', desc: 'Present perfect, conditionals, narrative tenses, and standard communication.' },
  B2: { title: 'Upper-Intermediate', desc: 'Complex conditionals, passive nuances, argumentative fluency, and deduction.' },
  C1: { title: 'Advanced', desc: 'Inversion, cleft clauses, stylistic subtleties, and sophisticated discourse.' },
  C2: { title: 'Mastery', desc: 'Native-equivalent idiomatic precision, rhetorical fronting, and effortless subtlety.' },
};

/**
 * @brief User profile page component.
 * @return React component element.
 */
export const ProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const { user, setUser } = useAuthStore();
  const [keys, setKeys] = useState<ApiKeyItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [selectedProvider, setSelectedProvider] = useState<AIProviderType>('DEEPSEEK');
  const [selectedModel, setSelectedModel] = useState<string>(getDefaultModelForProvider('DEEPSEEK'));
  const { models: providerModels, isLiveSynced } = useProviderModels(selectedProvider);
  const [rawKey, setRawKey] = useState('');
  const [isSavingKey, setIsSavingKey] = useState(false);
  const [keyMessage, setKeyMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [isEditingName, setIsEditingName] = useState(false);
  const [nameInput, setNameInput] = useState('');

  /**
   * @brief Updates the user's full name.
   */
  const handleSaveName = async () => {
    if (!nameInput.trim() || !user) return;
    setIsUpdatingProfile(true);
    setProfileMessage(null);
    try {
      const updated = await authApi.updateProfile({ fullName: nameInput.trim() });
      setUser(updated);
      setIsEditingName(false);
      setProfileMessage({ text: 'Full name successfully updated.', type: 'success' });
      setTimeout(() => setProfileMessage(null), 3000);
    } catch (err: any) {
      setProfileMessage({ text: err.response?.data?.message || 'Failed to update name.', type: 'error' });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  /**
   * @brief Switches the authenticated user's role between STUDENT and TEACHER.
   */
  const handleToggleRole = async () => {
    if (!user) return;
    const nextRole: Role = user.role === 'TEACHER' ? 'STUDENT' : 'TEACHER';
    setIsUpdatingProfile(true);
    setProfileMessage(null);
    try {
      const updated = await authApi.updateProfile({ role: nextRole });
      setUser(updated);
      setProfileMessage({
        text: `Switched role to ${nextRole === 'TEACHER' ? 'Educator / Teacher' : 'Student'} mode.`,
        type: 'success',
      });
      setTimeout(() => setProfileMessage(null), 4000);
    } catch (err: any) {
      setProfileMessage({
        text: err.response?.data?.message || 'Failed to switch role.',
        type: 'error',
      });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  /**
   * @brief Updates the user's target CEFR proficiency level across the A1-C2 ladder.
   * @param level Selected CEFR level.
   */
  const handleSelectCefrLevel = async (level: CefrLevel) => {
    if (!user || user.cefrLevel === level) return;
    setIsUpdatingProfile(true);
    setProfileMessage(null);
    try {
      const updated = await authApi.updateProfile({ cefrLevel: level });
      setUser(updated);
      setProfileMessage({
        text: `Proficiency level successfully updated to ${level} (${CEFR_LEVEL_METADATA[level].title}).`,
        type: 'success',
      });
      setTimeout(() => setProfileMessage(null), 3500);
    } catch (err: any) {
      setProfileMessage({
        text: err.response?.data?.message || 'Failed to update CEFR level.',
        type: 'error',
      });
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  /**
   * @brief Event handler updating selected AI provider and its default model.
   * @param provider New AI provider identifier.
   */
  const handleProviderChange = (provider: AIProviderType) => {
    setSelectedProvider(provider);
    setSelectedModel(getDefaultModelForProvider(provider));
  };

  /**
   * @brief Event handler or helper executing load keys.
   */
  const loadKeys = async () => {
    try {
      const data = await apiKeyApi.getKeys();
      setKeys(data);
    } catch (err) {
      console.error('Failed to load keys:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadKeys();
  }, []);

  /**
   * @brief Event handler or helper executing handle save key.
   */
  const handleSaveKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rawKey.trim()) return;

    setIsSavingKey(true);
    setKeyMessage(null);
    try {
      await apiKeyApi.saveKey(selectedProvider, rawKey.trim(), selectedModel);
      setRawKey('');
      setKeyMessage({
        text: `Custom ${selectedProvider} key (${selectedModel}) encrypted with AES-256-GCM and saved.`,
        type: 'success',
      });
      setTimeout(() => setKeyMessage(null), 3500);
      await loadKeys();
    } catch (err: any) {
      console.error('Failed to save API key:', err);
      setKeyMessage({
        text: err.response?.data?.message || 'Failed to save API key.',
        type: 'error',
      });
    } finally {
      setIsSavingKey(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle delete key.
   */
  const handleDeleteKey = async (id: string) => {
    if (!confirm('Remove this custom API key?')) return;
    try {
      await apiKeyApi.deleteKey(id);
      await loadKeys();
    } catch (err) {
      console.error('Failed to delete key:', err);
    }
  };

  if (!user) return null;

  return (
    <div className="max-w-3xl mx-auto space-y-8 py-8 px-4 sm:px-0">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Profile & Security</h1>
        <p className="text-sm text-slate-500 mt-1">
          Manage your account information and Bring-Your-Own-Key (BYOK) AI provider and model settings.
        </p>
      </div>

      {profileMessage && (
        <div
          className={`p-4 rounded-2xl text-xs sm:text-sm font-semibold flex items-center space-x-2 animate-in fade-in duration-200 border ${
            profileMessage.type === 'error'
              ? 'bg-rose-50 border-rose-200 text-rose-900'
              : 'bg-emerald-50 border-emerald-200 text-emerald-900'
          }`}
        >
          {profileMessage.type === 'error' ? (
            <AlertCircle className="w-5 h-5 text-rose-600 flex-shrink-0" />
          ) : (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
          )}
          <span>{profileMessage.text}</span>
        </div>
      )}

      {/* Account Info and Role Switcher Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 rounded-2xl bg-indigo-600 text-white font-black text-2xl flex items-center justify-center shadow-md shadow-indigo-100">
              {user.fullName.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="flex items-center space-x-2">
                {isEditingName ? (
                  <div className="flex items-center space-x-1.5">
                    <input
                      type="text"
                      value={nameInput}
                      onChange={(e) => setNameInput(e.target.value)}
                      className="px-2.5 py-1 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary font-semibold text-slate-900 bg-white"
                      placeholder="Your full name"
                      autoFocus
                    />
                    <button
                      type="button"
                      onClick={handleSaveName}
                      disabled={isUpdatingProfile}
                      className="p-1.5 bg-primary text-white rounded-lg hover:bg-primary-hover transition"
                      title="Save name"
                    >
                      <Check className="w-3.5 h-3.5" />
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setNameInput(user.fullName);
                        setIsEditingName(false);
                      }}
                      className="p-1.5 bg-slate-100 text-slate-600 rounded-lg hover:bg-slate-200 transition"
                      title="Cancel"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ) : (
                  <>
                    <h2 className="text-lg font-bold text-slate-900">{user.fullName}</h2>
                    <button
                      type="button"
                      onClick={() => {
                        setNameInput(user.fullName);
                        setIsEditingName(true);
                      }}
                      className="p-1 text-slate-400 hover:text-slate-600 rounded-md transition"
                      title="Edit full name"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    {user.cefrLevel && <CefrBadge level={user.cefrLevel} size="sm" />}
                  </>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">{user.email}</p>
              <div className="flex items-center space-x-2 mt-2">
                <span className={`text-[10px] font-bold px-2.5 py-1 rounded-md uppercase tracking-wider flex items-center space-x-1 ${
                  user.role === 'TEACHER'
                    ? 'bg-purple-100 text-purple-800 border border-purple-200'
                    : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                }`}>
                  {user.role === 'TEACHER' ? <School className="w-3 h-3 mr-1" /> : <GraduationCap className="w-3 h-3 mr-1" />}
                  Role: {user.role === 'TEACHER' ? 'Educator / Teacher' : 'Student / Learner'}
                </span>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
            <button
              onClick={handleToggleRole}
              disabled={isUpdatingProfile}
              className="py-2.5 px-4 rounded-xl border border-slate-200 hover:border-slate-300 bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-bold transition flex items-center justify-center space-x-2 disabled:opacity-50"
            >
              {user.role === 'TEACHER' ? (
                <>
                  <GraduationCap className="w-4 h-4 text-emerald-600" />
                  <span>Switch to Student Mode</span>
                </>
              ) : (
                <>
                  <School className="w-4 h-4 text-purple-600" />
                  <span>Switch to Teacher Mode</span>
                </>
              )}
            </button>

            <button
              onClick={() => navigate(user.role === 'TEACHER' ? '/teacher' : '/student')}
              className="py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition flex items-center justify-center space-x-1.5 shadow-sm"
            >
              <span>{user.role === 'TEACHER' ? 'Teacher Hub' : 'Practice Tasks'}</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* CEFR Level Ladder Selector */}
        <div className="pt-4 border-t border-slate-100 space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-800 flex items-center space-x-2">
                <BookOpen className="w-4 h-4 text-primary" />
                <span>Target CEFR Proficiency Level</span>
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Current level is <strong>{user.cefrLevel || 'A1'}</strong>. Select any level to update your practice syllabus.
              </p>
            </div>
            {user.cefrLevel && <CefrBadge level={user.cefrLevel} size="md" />}
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2 pt-1">
            {(['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as CefrLevel[]).map((level) => {
              const isSelected = user.cefrLevel === level;
              const meta = CEFR_LEVEL_METADATA[level];
              return (
                <button
                  key={level}
                  type="button"
                  onClick={() => handleSelectCefrLevel(level)}
                  disabled={isUpdatingProfile}
                  className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between space-y-1.5 ${
                    isSelected
                      ? 'border-primary bg-indigo-50/60 ring-2 ring-primary/20 shadow-sm'
                      : 'border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50/50'
                  } disabled:opacity-50`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-base font-black text-slate-900">{level}</span>
                    <CefrBadge level={level} size="sm" />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold text-slate-700 leading-tight">{meta.title}</p>
                    <p className="text-[10px] text-slate-400 line-clamp-2 mt-0.5">{meta.desc}</p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Guide: Roles, Groups, and CEFR */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-5">
        <div className="flex items-center space-x-2">
          <HelpCircle className="w-5 h-5 text-accent" />
          <h2 className="text-lg font-bold text-slate-900">Platform Guide: Roles, Groups, & CEFR</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-slate-600">
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
            <h4 className="font-bold text-slate-900 text-sm flex items-center space-x-1.5">
              <School className="w-4 h-4 text-purple-600" />
              <span>Roles & Student Cohort Management</span>
            </h4>
            <p className="leading-relaxed">
              • <strong>Role Switching:</strong> Any user can toggle between Student and Educator mode with a single click using the switch above.
            </p>
            <p className="leading-relaxed">
              • <strong>Creating Cohorts:</strong> Navigate to <a href="/groups" className="text-primary font-bold hover:underline">Cohorts (/groups)</a>, click "Create Cohort", and enter a class name (e.g. "IELTS 2026 Group A").
            </p>
            <p className="leading-relaxed">
              • <strong>Deploying Tasks:</strong> In the Task Configurator, select your target cohort and click "Deploy Task". All enrolled students will receive the assignment with deadline tracking.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-2">
            <h4 className="font-bold text-slate-900 text-sm flex items-center space-x-1.5">
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>CEFR Ladder & Placement</span>
            </h4>
            <p className="leading-relaxed">
              • <strong>Complete 6-Level Scale:</strong> A1 (Beginner) → A2 (Elementary) → B1 (Intermediate) → B2 (Upper-Intermediate) → C1 (Advanced) → C2 (Mastery).
            </p>
            <p className="leading-relaxed">
              • <strong>Adaptive Curriculum:</strong> The syllabus provides dedicated grammar topics across all proficiencies, from foundational A1 structures to C2 native-level discourse.
            </p>
            <p className="leading-relaxed">
              • <strong>Automated Level-Up:</strong> Reaching &gt;85% mastery across &gt;80% of current syllabus topics triggers an automated promotion prompt. You can also adjust your target level manually at any time.
            </p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div>
          <div className="flex items-center space-x-2">
            <Key className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-bold text-slate-900">Custom AI Provider & Model Keys (BYOK)</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Provide your personal API keys and choose your preferred model to bypass default daily limits. Stored safely with AES-256-GCM encryption.
          </p>
        </div>

        <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start space-x-3">
          <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
          <span>
            Keys are encrypted using 256-bit AES-GCM before database storage. They are decrypted in memory only when completing an AI request on your behalf.
          </span>
        </div>

        {keyMessage && (
          <div
            className={`p-3.5 rounded-xl text-xs font-semibold flex items-center space-x-2 border ${
              keyMessage.type === 'error'
                ? 'bg-rose-50 border-rose-200 text-rose-900'
                : 'bg-emerald-50 border-emerald-200 text-emerald-900'
            }`}
          >
            {keyMessage.type === 'error' ? (
              <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
            )}
            <span>{keyMessage.text}</span>
          </div>
        )}

        <form onSubmit={handleSaveKey} className="space-y-4 pt-2">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
                AI Provider
              </label>
              <select
                value={selectedProvider}
                onChange={(e) => handleProviderChange(e.target.value as AIProviderType)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-medium bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                {AI_PROVIDER_CATALOG.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="flex items-center justify-between text-xs font-bold text-slate-500 uppercase mb-1">
                <span>AI Model</span>
                {isLiveSynced && (
                  <span className="text-[10px] font-semibold text-emerald-600 lowercase">
                    ● live vendor sync
                  </span>
                )}
              </label>
              <select
                value={selectedModel}
                onChange={(e) => setSelectedModel(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-medium bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                {providerModels.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.label} — {m.badge}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
                Raw API Key
              </label>
              <input
                type="password"
                required
                placeholder="sk-... / AIza..."
                value={rawKey}
                onChange={(e) => setRawKey(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSavingKey || !rawKey.trim()}
            className="py-2.5 px-5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-sm disabled:opacity-50"
          >
            {isSavingKey ? 'Encrypting & Saving...' : 'Save API Key & Model'}
          </button>
        </form>

        <div className="pt-4 border-t border-slate-100 space-y-3">
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
            Active Encrypted Keys
          </h3>

          {isLoading ? (
            <LoadingSpinner size="sm" />
          ) : keys.length === 0 ? (
            <p className="text-xs text-slate-400 py-2">No custom keys configured yet.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {keys.map((k) => (
                <div key={k.id} className="py-3 flex items-center justify-between text-xs">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-bold text-slate-800">{k.provider}</span>
                    <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded-md bg-indigo-50 text-primary font-mono text-[11px] font-semibold">
                      <Cpu className="w-3 h-3" />
                      <span>{k.modelName || getDefaultModelForProvider(k.provider)}</span>
                    </span>
                    <span className="font-mono text-slate-400">••••••••••••••••</span>
                    <span className="text-[10px] text-slate-400">
                      Added {new Date(k.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <button
                    onClick={() => handleDeleteKey(k.id)}
                    className="p-1.5 text-slate-400 hover:text-rose-600 transition rounded-lg"
                    title="Delete key"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
