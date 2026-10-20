/**
 * @file HotkeysModal.tsx
 * @brief Modal cheatsheet presenting all available keyboard shortcuts across exercises and student modes.
 */

import React, { useEffect } from 'react';
import {
  Keyboard,
  X,
  Volume2,
  ArrowLeftRight,
  CornerDownLeft,
  Lightbulb,
  Sparkles,
  Layers,
  CheckCircle2,
} from 'lucide-react';

interface HotkeysModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface ShortcutItem {
  keys: string[];
  description: string;
}

interface ShortcutSection {
  title: string;
  icon: React.ReactNode;
  shortcuts: ShortcutItem[];
}

export const HotkeysModal: React.FC<HotkeysModalProps> = ({ isOpen, onClose }) => {
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const sections: ShortcutSection[] = [
    {
      title: 'Выбор вариантов ответов',
      icon: <CheckCircle2 className="w-4 h-4 text-emerald-600" />,
      shortcuts: [
        {
          keys: ['1', '2', '3', '4'],
          description: 'Мгновенный выбор варианта ответа по номеру',
        },
        {
          keys: ['A', 'B', 'C', 'D'],
          description: 'Выбор варианта по букве (поддерживается и русская раскладка: Ф, И, С, В)',
        },
        {
          keys: ['T', 'F'],
          description: 'Выбор True или False (для вопросов формата True/False; также Е / А)',
        },
      ],
    },
    {
      title: 'Аудио и произношение',
      icon: <Volume2 className="w-4 h-4 text-indigo-600" />,
      shortcuts: [
        {
          keys: ['Пробел'],
          description: 'Прослушать эталонное аудио-произношение вопроса или предложения (нажмите повторно для паузы)',
        },
      ],
    },
    {
      title: 'Навигация между вопросами',
      icon: <ArrowLeftRight className="w-4 h-4 text-sky-600" />,
      shortcuts: [
        {
          keys: ['←', '→'],
          description: 'Перемещение к предыдущей или следующей карточке вопроса (также H / L)',
        },
      ],
    },
    {
      title: 'Переход и подтверждение',
      icon: <CornerDownLeft className="w-4 h-4 text-violet-600" />,
      shortcuts: [
        {
          keys: ['Enter'],
          description: 'Переход к следующей карточке (или отправка ответа из текстового поля ввода)',
        },
        {
          keys: ['Ctrl', 'Enter'],
          description: 'Мгновенная отправка всей работы на проверку преподавателю / ИИ',
        },
      ],
    },
    {
      title: 'ИИ-разбор и подсказки',
      icon: <Sparkles className="w-4 h-4 text-amber-500" />,
      shortcuts: [
        {
          keys: ['E'],
          description: 'Грамматический и лексический разбор предложения с переводом (также У в русской раскладке)',
        },
        {
          keys: ['F'],
          description: 'Включить или выключить режим мгновенной проверки ответов',
        },
        {
          keys: ['R'],
          description: 'Показать или скрыть правильный ответ (в режиме интерактивной презентации)',
        },
      ],
    },
    {
      title: 'Вид и интерфейс',
      icon: <Layers className="w-4 h-4 text-slate-600" />,
      shortcuts: [
        {
          keys: ['V'],
          description: 'Циклическое переключение вида: Карточки → Список → Презентация',
        },
        {
          keys: ['?'],
          description: 'Открыть или закрыть эту справку по горячим клавишам',
        },
        {
          keys: ['Esc'],
          description: 'Закрыть любое модальное окно, окно разбора или снять выделение текста',
        },
      ],
    },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-hidden shadow-2xl border border-slate-100 flex flex-col animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 pb-4 border-b border-slate-100 flex-shrink-0">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-50 border border-indigo-100 text-primary flex items-center justify-center">
              <Keyboard className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-slate-900">
                Горячие клавиши (Hotkeys)
              </h3>
              <p className="text-xs text-slate-500 font-medium">
                Управляйте процессотом решения заданий без мыши
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
            aria-label="Закрыть"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-5">
          {sections.map((section, idx) => (
            <div key={idx} className="space-y-2.5">
              <div className="flex items-center space-x-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
                {section.icon}
                <span>{section.title}</span>
              </div>
              <div className="space-y-1.5 bg-slate-50/70 p-3 rounded-2xl border border-slate-100">
                {section.shortcuts.map((item, sIdx) => (
                  <div
                    key={sIdx}
                    className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 py-1.5 px-2 rounded-xl hover:bg-white/80 transition text-xs"
                  >
                    <span className="text-slate-700 font-medium leading-relaxed">
                      {item.description}
                    </span>
                    <div className="flex items-center space-x-1.5 flex-shrink-0 self-start sm:self-center">
                      {item.keys.map((key, kIdx) => (
                        <React.Fragment key={kIdx}>
                          <kbd className="px-2 py-1 rounded-lg bg-white border border-slate-200 text-slate-800 font-mono font-bold text-[11px] shadow-2xs">
                            {key}
                          </kbd>
                          {kIdx < item.keys.length - 1 && (
                            <span className="text-slate-400 font-mono text-[10px]">+</span>
                          )}
                        </React.Fragment>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500 flex-shrink-0">
          <div className="flex items-center space-x-2">
            <Lightbulb className="w-4 h-4 text-amber-500 flex-shrink-0" />
            <span>Клавиши работают везде в режиме карточек, когда не активен ввод текста.</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-xs"
          >
            Понятно
          </button>
        </div>
      </div>
    </div>
  );
};
