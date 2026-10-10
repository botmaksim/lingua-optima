/**
 * @file TranslatorDropdown.tsx
 * @brief Website translation dropdown integrating official Google Translate in-page widget with responsive mobile layout.
 */

import React, { useState, useEffect, useRef } from 'react';
import {
  Globe,
  ChevronDown,
  RotateCcw,
  Check,
  Languages,
} from 'lucide-react';

interface LanguageOption {
  code: string;
  name: string;
  flag: string;
}

const SUPPORTED_LANGUAGES: LanguageOption[] = [
  { code: 'ru', name: 'Russian (Русский)', flag: '🇷🇺' },
  { code: 'es', name: 'Spanish (Español)', flag: '🇪🇸' },
  { code: 'de', name: 'German (Deutsch)', flag: '🇩🇪' },
  { code: 'fr', name: 'French (Français)', flag: '🇫🇷' },
  { code: 'zh-CN', name: 'Chinese (中文)', flag: '🇨🇳' },
  { code: 'ar', name: 'Arabic (العربية)', flag: '🇸🇦' },
  { code: 'tr', name: 'Turkish (Türkçe)', flag: '🇹🇷' },
];

/**
 * @brief Component providing Google Translate in-page translation controls.
 * @return React component element.
 */
export const TranslatorDropdown: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [activeEngine, setActiveEngine] = useState<'none' | 'google'>('none');
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
    }
  }, []);

  /**
   * @brief Watches for and neutralizes any intrusive banner iframes or top body shifts injected by translation widgets.
   */
  useEffect(() => {
    const neutralizeTranslateOverlays = () => {
      // Force body and html top to 0px and remove relative shift
      if (document.body.style.top && document.body.style.top !== '0px') {
        document.body.style.setProperty('top', '0px', 'important');
      }
      if (document.body.style.position === 'relative') {
        document.body.style.removeProperty('position');
      }
      if (document.documentElement.style.top && document.documentElement.style.top !== '0px') {
        document.documentElement.style.setProperty('top', '0px', 'important');
      }

      // Hide all Google Translate banner iframes, tooltips, and floating gadgets
      const bannerFrames = document.querySelectorAll<HTMLElement>(
        'iframe.skiptranslate, iframe.goog-te-banner-frame, iframe[id*=":1.container"], iframe[id*=":2.container"], iframe[id*=":3.container"], .VIpgJd-ZVi9od-aZ2wEe-wOHMyf, .VIpgJd-ZVi9od-ORHb-Oxf5ab, .VIpgJd-ZVi9od-xl07Ob-Oxf5ab, #goog-gt-tt, .goog-te-banner-frame'
      );
      bannerFrames.forEach((frame) => {
        frame.style.setProperty('display', 'none', 'important');
        frame.style.setProperty('height', '0px', 'important');
        frame.style.setProperty('visibility', 'hidden', 'important');
        frame.style.setProperty('opacity', '0', 'important');
        frame.style.setProperty('pointer-events', 'none', 'important');
      });
    };

    neutralizeTranslateOverlays();

    const observer = new MutationObserver(() => {
      neutralizeTranslateOverlays();
    });

    observer.observe(document.body, {
      attributes: true,
      attributeFilter: ['style', 'class'],
      childList: true,
    });

    return () => observer.disconnect();
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
      const domainParts = host.split('.');
      if (domainParts.length >= 2) {
        const rootDomain = '.' + domainParts.slice(-2).join('.');
        document.cookie = `googtrans=/en/${lang}; path=/; domain=${rootDomain};`;
      }
    }
  };

  /**
   * @brief Clears Google Translate cookie across all domain scopes.
   */
  const clearGoogleTranslateCookie = () => {
    const host = window.location.hostname;
    const expireStr = '; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
    document.cookie = 'googtrans=' + expireStr;
    document.cookie = `googtrans=${expireStr} domain=${host};`;
    if (host.includes('.')) {
      document.cookie = `googtrans=${expireStr} domain=.${host};`;
      const domainParts = host.split('.');
      if (domainParts.length >= 2) {
        const rootDomain = '.' + domainParts.slice(-2).join('.');
        document.cookie = `googtrans=${expireStr} domain=${rootDomain};`;
      }
    }
    document.cookie = 'googtrans=/en/en; path=/;';
  };

  /**
   * @brief Initializes and triggers Google Translate for a specific language.
   * @param lang Target language code.
   */
  const handleTranslateGoogle = (lang = 'ru') => {
    localStorage.removeItem('lingua_translate_engine');
    setGoogleTranslateCookie(lang);
    localStorage.setItem('lingua_translate_engine', 'google');
    setActiveEngine('google');
    setCurrentLang(lang);
    setIsOpen(false);

    if (!document.getElementById('google-translate-script')) {
      (window as any).googleTranslateElementInit = () => {
        if ((window as any).google?.translate?.TranslateElement) {
          new (window as any).google.translate.TranslateElement(
            {
              pageLanguage: 'en',
              includedLanguages: 'ru,es,de,fr,zh-CN,ar,tr,en',
              autoDisplay: false,
              layout: (window as any).google.translate.TranslateElement.InlineLayout?.SIMPLE,
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

  return (
    <div className="relative notranslate" translate="no" ref={dropdownRef}>
      {/* Hidden container required by Google Translate script */}
      <div id="google_translate_element" className="hidden" />

      {/* Main Navbar Button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center space-x-1.5 px-2.5 py-1.5 rounded-xl text-xs font-semibold border transition shadow-sm ${
          activeEngine !== 'none'
            ? 'bg-emerald-50 text-emerald-900 border-emerald-300 hover:bg-emerald-100 ring-2 ring-emerald-200'
            : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50 hover:text-slate-900'
        }`}
        title="Translate webpage with Google Translate"
        aria-label="Translate page"
      >
        <Globe className={`w-3.5 h-3.5 ${activeEngine !== 'none' ? 'text-emerald-600' : 'text-indigo-600'}`} />
        <span className="hidden sm:inline">
          {activeEngine === 'google' ? `Google: ${currentLang.toUpperCase()}` : 'Translate'}
        </span>
        {activeEngine !== 'none' && (
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
        )}
        <ChevronDown className="w-3 h-3 text-slate-400" />
      </button>

      {/* Popover Dropdown Menu */}
      {isOpen && (
        <>
          {/* Mobile backdrop */}
          <div
            className="fixed inset-0 z-40 bg-black/20 backdrop-blur-2xs sm:hidden"
            onClick={() => setIsOpen(false)}
          />

          <div className="fixed inset-x-3 top-16 sm:absolute sm:inset-x-auto sm:right-0 sm:top-full sm:mt-2 sm:w-80 max-w-[calc(100vw-1.5rem)] overflow-hidden bg-white/95 backdrop-blur-xl rounded-2xl shadow-2xl shadow-indigo-950/10 border border-slate-200/80 ring-1 ring-slate-900/5 p-3.5 pt-4 z-50 animate-in fade-in zoom-in-95 duration-150 space-y-3">
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-indigo-500 via-sky-500 to-emerald-500" />
            {/* Header */}
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <div className="flex items-center space-x-2">
                <Languages className="w-4 h-4 text-primary" />
                <span className="text-xs font-bold text-slate-900">Translate Webpage</span>
              </div>
              {activeEngine !== 'none' && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                  Active: Google
                </span>
              )}
            </div>

            <p className="text-[11px] text-slate-500 leading-snug">
              Translate the entire application interface and exercises in-place using Google Translate:
            </p>

            {/* Google Translate Options */}
            <div className="p-2.5 rounded-xl border border-slate-100 bg-slate-50/60 hover:bg-slate-50 transition space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <span className="w-5 h-5 rounded-md bg-blue-600 text-white font-black text-[11px] flex items-center justify-center">
                    G
                  </span>
                  <span className="text-xs font-bold text-slate-900">Google Translate</span>
                </div>
                <span className="text-[10px] font-semibold text-slate-400">In-Page Widget</span>
              </div>

              <div className="pt-1">
                <button
                  type="button"
                  onClick={() => handleTranslateGoogle('ru')}
                  className={`w-full py-2 px-3 text-[11px] font-semibold rounded-lg border transition text-left flex items-center justify-between ${
                    activeEngine === 'google' && currentLang === 'ru'
                      ? 'bg-blue-50 border-blue-300 text-blue-700 shadow-2xs font-bold'
                      : 'bg-white hover:bg-blue-50/50 text-slate-700 border-slate-200'
                  }`}
                >
                  <span className="flex items-center gap-1.5">
                    <span>🇷🇺</span>
                    <span>Russian (Русский)</span>
                  </span>
                  {activeEngine === 'google' && currentLang === 'ru' ? (
                    <Check className="w-3.5 h-3.5 text-blue-600" />
                  ) : (
                    <span className="text-[10px] text-slate-400 font-normal">Translate</span>
                  )}
                </button>
              </div>

              {/* Expandable more languages */}
              <div>
                <button
                  type="button"
                  onClick={() => setIsMoreLangsOpen(!isMoreLangsOpen)}
                  className="text-[11px] font-bold text-primary hover:underline flex items-center space-x-1 pt-1"
                >
                  <span>{isMoreLangsOpen ? 'Hide other languages' : 'Other languages (ES, DE, FR, ZH, AR, TR)...'}</span>
                  <ChevronDown className={`w-3 h-3 transition-transform ${isMoreLangsOpen ? 'rotate-180' : ''}`} />
                </button>

                {isMoreLangsOpen && (
                  <div className="grid grid-cols-2 gap-1.5 mt-2 pt-2 border-t border-slate-200">
                    {SUPPORTED_LANGUAGES.filter((l) => l.code !== 'ru').map((l) => (
                      <button
                        key={l.code}
                        type="button"
                        onClick={() => handleTranslateGoogle(l.code)}
                        className={`py-1.5 px-2 text-[10px] font-medium rounded-md text-left transition flex items-center justify-between ${
                          activeEngine === 'google' && currentLang === l.code
                            ? 'bg-blue-100 text-blue-800 font-bold'
                            : 'bg-white hover:bg-slate-100 text-slate-700 border border-slate-100'
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

            {/* Revert / Original English Button */}
            {activeEngine !== 'none' && (
              <button
                type="button"
                onClick={handleRevertOriginal}
                className="w-full py-2 px-3 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold transition flex items-center justify-center space-x-1.5 shadow-sm"
              >
                <RotateCcw className="w-3.5 h-3.5 text-slate-200" />
                <span>Show Original (English)</span>
              </button>
            )}
          </div>
        </>
      )}
    </div>
  );
};
