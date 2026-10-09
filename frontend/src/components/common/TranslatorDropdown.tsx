/**
 * @file TranslatorDropdown.tsx
 * @brief Website translation dropdown integrating official Google Translate and Yandex Translate widgets.
 */

import React, { useState, useEffect, useRef } from 'react';
import {
  Globe,
  ChevronDown,
  ExternalLink,
  RotateCcw,
  Check,
  Languages,
} from 'lucide-react';

const SUPPORTED_LANGUAGES = [
  { code: 'ru', name: 'Russian (Русский)', flag: '🇷🇺' },
  { code: 'es', name: 'Spanish (Español)', flag: '🇪🇸' },
  { code: 'de', name: 'German (Deutsch)', flag: '🇩🇪' },
  { code: 'fr', name: 'French (Français)', flag: '🇫🇷' },
  { code: 'zh-CN', name: 'Chinese (中文)', flag: '🇨🇳' },
  { code: 'ar', name: 'Arabic (العربية)', flag: '🇸🇦' },
  { code: 'tr', name: 'Turkish (Türkçe)', flag: '🇹🇷' },
];

/**
 * @brief Component providing Google Translate and Yandex Translate page translation controls.
 * @return React component element.
 */
export const TranslatorDropdown: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [activeEngine, setActiveEngine] = useState<'none' | 'google' | 'yandex'>('none');
  const [currentLang, setCurrentLang] = useState<string>('en');
  const [isMoreLangsOpen, setIsMoreLangsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  /**
   * @brief Detects active translation cookies on mount.
   */
  useEffect(() => {
    const cookies = document.cookie;
    const match = cookies.match(/googtrans=\/en\/([a-zA-Z-]+)/);
    if (match && match[1] && match[1] !== 'en') {
      setActiveEngine('google');
      setCurrentLang(match[1]);
    } else if (localStorage.getItem('lingua_translate_engine') === 'yandex') {
      setActiveEngine('yandex');
      setCurrentLang('ru');
    }
  }, []);

  /**
   * @brief Handles outside click to close popover.
   */
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        setIsMoreLangsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  /**
   * @brief Sets Google Translate cookie across all possible domain scopes.
   * @param lang Target language code.
   */
  const setGoogleTranslateCookie = (lang: string) => {
    const host = window.location.hostname;
    document.cookie = `googtrans=/en/${lang}; path=/;`;
    document.cookie = `googtrans=/en/${lang}; path=/; domain=${host};`;
    if (host.includes('.')) {
      document.cookie = `googtrans=/en/${lang}; path=/; domain=.${host};`;
    }
  };

  /**
   * @brief Clears Google Translate cookie.
   */
  const clearGoogleTranslateCookie = () => {
    const host = window.location.hostname;
    document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = `googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=${host};`;
    if (host.includes('.')) {
      document.cookie = `googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=.${host};`;
    }
  };

  /**
   * @brief Initializes and triggers Google Translate for a specific language.
   * @param lang Target language code.
   */
  const handleTranslateGoogle = (lang = 'ru') => {
    setGoogleTranslateCookie(lang);
    localStorage.setItem('lingua_translate_engine', 'google');
    setActiveEngine('google');
    setCurrentLang(lang);
    setIsOpen(false);

    // If script not yet injected
    if (!document.getElementById('google-translate-script')) {
      window.googleTranslateElementInit = () => {
        if (window.google?.translate?.TranslateElement) {
          new window.google.translate.TranslateElement(
            {
              pageLanguage: 'en',
              includedLanguages: 'ru,es,de,fr,zh-CN,ar,tr,en',
              autoDisplay: false,
              layout: window.google.translate.TranslateElement.InlineLayout?.SIMPLE,
            },
            'google_translate_element'
          );
        }
      };
      const script = document.createElement('script');
      script.id = 'google-translate-script';
      script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
      script.async = true;
      document.body.appendChild(script);
    } else {
      // Script already present; reload so Google Translate evaluates the new cookie cleanly
      window.location.reload();
    }
  };

  /**
   * @brief Initializes and triggers Yandex Translate widget.
   */
  const handleTranslateYandex = () => {
    localStorage.setItem('lingua_translate_engine', 'yandex');
    setActiveEngine('yandex');
    setCurrentLang('ru');
    setIsOpen(false);

    if (!document.getElementById('yandex-translate-script')) {
      const script = document.createElement('script');
      script.id = 'yandex-translate-script';
      script.src = 'https://translate.yandex.net/website-widget/v1/widget.js?widgetId=ytWidget&pageLang=en&widgetTheme=light&autoMode=false';
      script.async = true;
      document.body.appendChild(script);
    } else if (window.ya?.translate?.changeLang) {
      window.ya.translate.changeLang('ru');
    } else {
      window.location.reload();
    }
  };

  /**
   * @brief Reverts the webpage back to original English.
   */
  const handleRevertOriginal = () => {
    clearGoogleTranslateCookie();
    localStorage.removeItem('lingua_translate_engine');
    setActiveEngine('none');
    setCurrentLang('en');
    setIsOpen(false);
    window.location.reload();
  };

  /**
   * @brief Opens Google Web Translate for current page in new tab.
   */
  const handleOpenGoogleWeb = () => {
    const currentUrl = encodeURIComponent(window.location.href);
    window.open(`https://translate.google.com/translate?sl=en&tl=ru&u=${currentUrl}`, '_blank');
  };

  /**
   * @brief Opens Yandex Web Translate for current page in new tab.
   */
  const handleOpenYandexWeb = () => {
    const currentUrl = encodeURIComponent(window.location.href);
    window.open(`https://translate.yandex.com/translate?view=compact&url=${currentUrl}&lang=en-ru`, '_blank');
  };

  return (
    <div className="relative notranslate" translate="no" ref={dropdownRef}>
      {/* Hidden containers required by Google and Yandex widget scripts */}
      <div id="google_translate_element" className="hidden" />
      <div id="ytWidget" className="hidden" />

      {/* Main Navbar Button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center space-x-1.5 px-2.5 py-1.5 rounded-xl text-xs font-semibold border transition shadow-sm ${
          activeEngine !== 'none'
            ? 'bg-amber-50 text-amber-900 border-amber-300 hover:bg-amber-100 ring-2 ring-amber-200'
            : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50 hover:text-slate-900'
        }`}
        title="Translate webpage with Google or Yandex"
        aria-label="Translate page"
      >
        <Globe className={`w-3.5 h-3.5 ${activeEngine !== 'none' ? 'text-amber-600' : 'text-indigo-600'}`} />
        <span className="hidden sm:inline">
          {activeEngine === 'google'
            ? `Google: ${currentLang.toUpperCase()}`
            : activeEngine === 'yandex'
            ? 'Yandex: RU'
            : 'Translate'}
        </span>
        {activeEngine !== 'none' && (
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
        )}
        <ChevronDown className="w-3 h-3 text-slate-400" />
      </button>

      {/* Popover Dropdown Menu */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 overflow-hidden bg-white/95 backdrop-blur-xl rounded-2xl shadow-2xl shadow-indigo-950/10 border border-slate-200/80 ring-1 ring-slate-900/5 p-3.5 pt-4 z-50 animate-in fade-in zoom-in-95 duration-150 space-y-3">
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-indigo-500 via-sky-500 to-emerald-500" />
          {/* Header */}
          <div className="flex items-center justify-between pb-2 border-b border-slate-100">
            <div className="flex items-center space-x-2">
              <Languages className="w-4 h-4 text-primary" />
              <span className="text-xs font-bold text-slate-900">Translate Webpage</span>
            </div>
            {activeEngine !== 'none' && (
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                Active: {activeEngine === 'google' ? 'Google' : 'Yandex'}
              </span>
            )}
          </div>

          <p className="text-[11px] text-slate-500 leading-snug">
            Translate the interface and exercises into Russian or other languages using official automated services:
          </p>

          {/* Service 1: Google Translate */}
          <div className="p-2.5 rounded-xl border border-slate-100 bg-slate-50/60 hover:bg-slate-50 transition space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="w-5 h-5 rounded-md bg-blue-600 text-white font-black text-[11px] flex items-center justify-center">
                  G
                </span>
                <span className="text-xs font-bold text-slate-900">Google Translate</span>
              </div>
              <span className="text-[10px] font-semibold text-slate-400">Global</span>
            </div>

            <div className="grid grid-cols-2 gap-1.5 pt-1">
              <button
                type="button"
                onClick={() => handleTranslateGoogle('ru')}
                className="py-1.5 px-2 bg-white hover:bg-blue-50 text-slate-700 hover:text-blue-700 text-[11px] font-semibold rounded-lg border border-slate-200 hover:border-blue-200 transition text-left flex items-center justify-between"
              >
                <span>🇷🇺 Russian (RU)</span>
                {activeEngine === 'google' && currentLang === 'ru' && (
                  <Check className="w-3 h-3 text-blue-600" />
                )}
              </button>

              <button
                type="button"
                onClick={handleOpenGoogleWeb}
                className="py-1.5 px-2 bg-white hover:bg-slate-100 text-slate-600 text-[11px] font-semibold rounded-lg border border-slate-200 transition flex items-center justify-between"
                title="Open via Google Web Translate proxy"
              >
                <span>Google Web</span>
                <ExternalLink className="w-3 h-3 text-slate-400" />
              </button>
            </div>

            {/* Expandable more languages */}
            <div>
              <button
                type="button"
                onClick={() => setIsMoreLangsOpen(!isMoreLangsOpen)}
                className="text-[11px] font-bold text-primary hover:underline flex items-center space-x-1"
              >
                <span>{isMoreLangsOpen ? 'Hide other languages' : 'More languages (ES, DE, FR, ZH)...'}</span>
                <ChevronDown className={`w-3 h-3 transition-transform ${isMoreLangsOpen ? 'rotate-180' : ''}`} />
              </button>

              {isMoreLangsOpen && (
                <div className="grid grid-cols-2 gap-1 mt-2 pt-2 border-t border-slate-200">
                  {SUPPORTED_LANGUAGES.map((l) => (
                    <button
                      key={l.code}
                      type="button"
                      onClick={() => handleTranslateGoogle(l.code)}
                      className={`py-1 px-2 text-[10px] font-medium rounded-md text-left transition flex items-center justify-between ${
                        activeEngine === 'google' && currentLang === l.code
                          ? 'bg-blue-100 text-blue-800 font-bold'
                          : 'bg-white hover:bg-slate-100 text-slate-700'
                      }`}
                    >
                      <span className="truncate">{l.flag} {l.name.split(' ')[0]}</span>
                      {activeEngine === 'google' && currentLang === l.code && (
                        <Check className="w-2.5 h-2.5 text-blue-600 flex-shrink-0" />
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Service 2: Yandex Translate */}
          <div className="p-2.5 rounded-xl border border-slate-100 bg-slate-50/60 hover:bg-slate-50 transition space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="w-5 h-5 rounded-md bg-red-600 text-white font-black text-[11px] flex items-center justify-center">
                  Я
                </span>
                <span className="text-xs font-bold text-slate-900">Yandex Translate</span>
              </div>
              <span className="text-[10px] font-semibold text-slate-400">RU / CIS</span>
            </div>

            <div className="grid grid-cols-2 gap-1.5 pt-1">
              <button
                type="button"
                onClick={handleTranslateYandex}
                className="py-1.5 px-2 bg-white hover:bg-red-50 text-slate-700 hover:text-red-700 text-[11px] font-semibold rounded-lg border border-slate-200 hover:border-red-200 transition text-left flex items-center justify-between"
              >
                <span>🇷🇺 Russian (RU)</span>
                {activeEngine === 'yandex' && (
                  <Check className="w-3 h-3 text-red-600" />
                )}
              </button>

              <button
                type="button"
                onClick={handleOpenYandexWeb}
                className="py-1.5 px-2 bg-white hover:bg-slate-100 text-slate-600 text-[11px] font-semibold rounded-lg border border-slate-200 transition flex items-center justify-between"
                title="Open via Yandex Web Translate proxy"
              >
                <span>Yandex Web</span>
                <ExternalLink className="w-3 h-3 text-slate-400" />
              </button>
            </div>
          </div>

          {/* Revert / Original English Button */}
          {activeEngine !== 'none' && (
            <button
              type="button"
              onClick={handleRevertOriginal}
              className="w-full py-2 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold transition flex items-center justify-center space-x-1.5 border border-slate-200"
            >
              <RotateCcw className="w-3.5 h-3.5 text-slate-600" />
              <span>Show Original (English)</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};
