/**
 * @file MobileBottomNav.tsx
 * @brief Persistent bottom navigation bar for mobile viewports providing one-thumb access to core workspaces.
 */

import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Sparkles,
  BarChart3,
  BookOpen,
  User,
  Users,
  CheckCircle,
  FileText,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

/**
 * @brief Mobile bottom navigation bar rendered on screen widths < 768px.
 * @return JSX mobile navigation bar or null if unauthenticated.
 */
export const MobileBottomNav: React.FC = () => {
  const { user, isStudent, isTeacher } = useAuth();
  const location = useLocation();

  if (!user) return null;

  const studentNavItems = [
    { to: '/student', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/student/generate', label: 'New Task', icon: Sparkles },
    { to: '/student/progress', label: 'Progress', icon: BarChart3 },
    { to: '/student/my-units', label: 'My Units', icon: BookOpen },
    { to: '/profile', label: 'Profile', icon: User },
  ];

  const teacherNavItems = [
    { to: '/teacher', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/teacher/groups', label: 'Cohorts', icon: Users },
    { to: '/teacher/configure', label: 'Create', icon: Sparkles },
    { to: '/teacher/submissions', label: 'Grading', icon: CheckCircle },
    { to: '/teacher/export', label: 'Reports', icon: FileText },
  ];

  const items = isTeacher ? teacherNavItems : isStudent ? studentNavItems : [];

  if (items.length === 0) return null;

  return (
    <nav
      aria-label="Mobile Navigation"
      className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white/95 backdrop-blur-xl border-t border-slate-200/90 shadow-[0_-4px_24px_rgba(0,0,0,0.06)] pb-[env(safe-area-inset-bottom)] no-print"
    >
      <div className="grid grid-cols-5 h-16 max-w-lg mx-auto px-1">
        {items.map((item) => {
          const Icon = item.icon;
          const isActive =
            location.pathname === item.to ||
            (item.to !== '/student' && item.to !== '/teacher' && location.pathname.startsWith(item.to));

          return (
            <Link
              key={item.to}
              to={item.to}
              className={`flex flex-col items-center justify-center py-1 transition-all select-none group relative ${
                isActive ? 'text-primary' : 'text-slate-400 hover:text-slate-700'
              }`}
            >
              <div
                className={`p-1 rounded-xl transition-transform active:scale-90 ${
                  isActive ? 'bg-indigo-50 text-primary scale-105' : 'group-hover:bg-slate-50'
                }`}
              >
                <Icon className="w-5 h-5 stroke-[2.2]" />
              </div>
              <span
                className={`text-[10px] tracking-tight mt-0.5 truncate max-w-[64px] ${
                  isActive ? 'font-bold text-primary' : 'font-medium'
                }`}
              >
                {item.label}
              </span>
              {isActive && (
                <span className="w-1.5 h-1.5 rounded-full bg-primary mt-0.5 animate-in fade-in zoom-in-75 duration-150" />
              )}
            </Link>
          );
        })}
      </div>
    </nav>
  );
};
