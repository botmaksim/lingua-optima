/**
 * @file ProfilePage.tsx
 * @brief User account profile, CEFR status, and BYOK AES-256 encrypted API key management page.
 */

import React, { useState, useEffect } from 'react';
import { Key, ShieldCheck, Trash2, CheckCircle2, Cpu } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import { apiKeyApi, ApiKeyItem } from '../api/apiKeyApi';
import { CefrBadge } from '../components/common/CefrBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  getModelsForProvider,
} from '../constants/aiModels';

/**
 * @brief User profile page component.
 * @return React component element.
 */
export const ProfilePage: React.FC = () => {
  const { user } = useAuthStore();
  const [keys, setKeys] = useState<ApiKeyItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [selectedProvider, setSelectedProvider] = useState<AIProviderType>('DEEPSEEK');
  const [selectedModel, setSelectedModel] = useState<string>(getDefaultModelForProvider('DEEPSEEK'));
  const [rawKey, setRawKey] = useState('');
  const [isSavingKey, setIsSavingKey] = useState(false);
  const [keyMessage, setKeyMessage] = useState<string | null>(null);

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
      setKeyMessage(`Custom ${selectedProvider} key (${selectedModel}) encrypted with AES-256-GCM and saved.`);
      setTimeout(() => setKeyMessage(null), 3500);
      await loadKeys();
    } catch (err: any) {
      console.error('Failed to save API key:', err);
      setKeyMessage(err.response?.data?.message || 'Failed to save API key.');
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
          <div className="p-3.5 bg-indigo-50 border border-indigo-200 text-indigo-900 rounded-xl text-xs flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4 text-primary" />
            <span>{keyMessage}</span>
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
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
                AI Model
              </label>
              <select
                value={selectedModel}
                onChange={(e) => setSelectedModel(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-medium bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                {getModelsForProvider(selectedProvider).map((m) => (
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
