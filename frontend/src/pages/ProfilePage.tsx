/**
 * @file ProfilePage.tsx
 * @brief User account profile, CEFR status, and BYOK AES-256 encrypted API key management page.
 */

import React, { useState, useEffect } from 'react';
import { Key, ShieldCheck, Trash2, CheckCircle2 } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { apiKeyApi, ApiKeyItem } from '../api/apiKeyApi';
import { CefrBadge } from '../components/common/CefrBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

/**
 * @brief User profile page component.
 * @return React component element.
 */
export const ProfilePage: React.FC = () => {
  const { user } = useAuthStore();
  const [keys, setKeys] = useState<ApiKeyItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [selectedProvider, setSelectedProvider] = useState<'GROQ' | 'GEMINI' | 'OPENAI' | 'ANTHROPIC'>('OPENAI');
  const [rawKey, setRawKey] = useState('');
  const [isSavingKey, setIsSavingKey] = useState(false);
  const [keyMessage, setKeyMessage] = useState<string | null>(null);

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

  const handleSaveKey = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rawKey.trim()) return;

    setIsSavingKey(true);
    setKeyMessage(null);
    try {
      await apiKeyApi.saveKey(selectedProvider, rawKey.trim());
      setRawKey('');
      setKeyMessage('Custom API key securely encrypted with AES-256-GCM and saved.');
      setTimeout(() => setKeyMessage(null), 3000);
      await loadKeys();
    } catch (err: any) {
      console.error('Failed to save API key:', err);
      setKeyMessage(err.response?.data?.message || 'Failed to save API key.');
    } finally {
      setIsSavingKey(false);
    }
  };

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
          Manage your account information and Bring-Your-Own-Key (BYOK) AI provider settings.
        </p>
      </div>

      {/* User Info Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div className="flex items-center space-x-4">
          <div className="w-16 h-16 rounded-2xl bg-indigo-600 text-white font-black text-2xl flex items-center justify-center shadow-md shadow-indigo-100">
            {user.fullName.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h2 className="text-lg font-bold text-slate-900">{user.fullName}</h2>
              {user.cefrLevel && <CefrBadge level={user.cefrLevel} size="sm" />}
            </div>
            <p className="text-xs text-slate-400 mt-0.5">{user.email}</p>
            <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 uppercase mt-2 inline-block">
              Role: {user.role}
            </span>
          </div>
        </div>
      </div>

      {/* BYOK (Bring Your Own Key) Section */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div>
          <div className="flex items-center space-x-2">
            <Key className="w-5 h-5 text-primary" />
            <h2 className="text-lg font-bold text-slate-900">Custom AI Provider Keys (BYOK)</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Provide your personal API keys to bypass default daily limits. Stored safely with AES-256-GCM encryption.
          </p>
        </div>

        {/* Security Alert */}
        <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-xs text-slate-600 flex items-start space-x-3">
          <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
          <span>
            Keys are encrypted using 256-bit AES-GCM before database storage. They are decrypted in memory only when completing an AI request on your behalf.
          </span>
        </div>

        {keyMessage && (
          <div className="p-3.5 bg-indigo-50 border border-indigo-200 text-indigo-900 rounded-xl text-xs flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 text-primary" />
            <span>{keyMessage}</span>
          </div>
        )}

        {/* Add Key Form */}
        <form onSubmit={handleSaveKey} className="space-y-4 pt-2">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
                AI Provider
              </label>
              <select
                value={selectedProvider}
                onChange={(e) => setSelectedProvider(e.target.value as any)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-medium bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                <option value="OPENAI">OpenAI (GPT-4o mini)</option>
                <option value="ANTHROPIC">Anthropic (Claude 3.5 Sonnet)</option>
                <option value="GEMINI">Google Gemini 1.5 Flash</option>
                <option value="GROQ">Groq (Llama 3.1 70B)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
                Raw API Key
              </label>
              <input
                type="password"
                required
                placeholder="sk-..."
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
            {isSavingKey ? 'Encrypting & Saving...' : 'Save API Key'}
          </button>
        </form>

        {/* Active Keys List */}
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
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-slate-800">{k.provider}</span>
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
