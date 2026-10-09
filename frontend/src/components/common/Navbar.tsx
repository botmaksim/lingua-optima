/**
 * @file Navbar.tsx
 * @brief Primary application navigation header featuring responsive mobile menu, notifications dropdown, and role links.
 */

import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import {
  Bell,
  Sparkles,
  LogOut,
  User as UserIcon,
  BookOpen,
  LayoutDashboard,
  CheckCircle,
  BarChart3,
  Users,
  FileText,
  CreditCard,
  Camera,
  Zap,
  GraduationCap,
  Menu,
  X,
  ChevronDown,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/authApi';
import { useUsage } from '../../hooks/useUsage';
import { useNotificationStore } from '../../store/notificationStore';
import { useUIStore } from '../../store/uiStore';
import { CefrBadge } from './CefrBadge';
import { OfflineBanner } from './OfflineBanner';
import { TranslatorDropdown } from './TranslatorDropdown';
import { formatDate } from '../../utils/formatDate';

/**
 * @brief Global navigation bar component.
 * @return React component element.
 */
export const Navbar: React.FC = () => {
  const { user, isStudent, isTeacher, logout } = useAuth();
  const { setUser } = useAuthStore();
  const { remainingEvaluations, isQuotaExceeded } = useUsage();
  const { notifications, unreadCount, fetchNotifications, markAsRead } = useNotificationStore();
  const { openUpgradeWall } = useUIStore();
  const navigate = useNavigate();
  const location = useLocation();

  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSwitchingRole, setIsSwitchingRole] = useState(false);
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);

  const moreMenuRef = useRef<HTMLDivElement>(null);
  const notifMenuRef = useRef<HTMLDivElement>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (user) {
      fetchNotifications();
    }
  }, [user, fetchNotifications]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      if (moreMenuRef.current && !moreMenuRef.current.contains(target)) {
        setIsMoreMenuOpen(false);
      }
      if (notifMenuRef.current && !notifMenuRef.current.contains(target)) {
        setIsNotifOpen(false);
      }
      if (userMenuRef.current && !userMenuRef.current.contains(target)) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    setIsMoreMenuOpen(false);
    setIsNotifOpen(false);
    setIsUserMenuOpen(false);
    setIsMobileMenuOpen(false);
  }, [location.pathname]);

  /**
   * @brief Event handler or helper executing handle logout.
   */
  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  /**
   * @brief Fast role toggle directly from navbar user menu.
   */
  const handleQuickRoleToggle = async () => {
    if (!user || isSwitchingRole) return;
    setIsSwitchingRole(true);
    const nextRole = user.role === 'TEACHER' ? 'STUDENT' : 'TEACHER';
    try {
      const updated = await authApi.updateProfile({ role: nextRole });
      setUser(updated);
      setIsUserMenuOpen(false);
      navigate(nextRole === 'TEACHER' ? '/teacher' : '/student');
    } catch (err) {
      console.error('Failed to switch role:', err);
    } finally {
      setIsSwitchingRole(false);
    }
  };

  const studentPrimaryLinks = [
    { to: '/student', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/student/generate', label: 'New Task', icon: Sparkles },
    { to: '/student/progress', label: 'Progress', icon: BarChart3 },
  ];

  const studentMoreLinks = [
    { to: '/student/essay', label: 'Essay Studio', icon: FileText, description: 'AI writing & feedback' },
    { to: '/student/ocr', label: 'Photo OCR', icon: Camera, description: 'Scan handwritten tasks' },
    { to: '/student/session', label: 'Adaptive CAT', icon: Zap, description: 'Personalized testing' },
    { to: '/student/my-units', label: 'My Units', icon: BookOpen, description: 'Curriculum & vocabulary' },
    { to: '/student/leaderboard', label: 'Leaderboard', icon: Users, description: 'Rankings & points' },
  ];

  const teacherPrimaryLinks = [
    { to: '/teacher', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/teacher/groups', label: 'Student Groups', icon: Users },
    { to: '/teacher/submissions', label: 'Submissions', icon: CheckCircle },
  ];

  const teacherMoreLinks = [
    { to: '/teacher/configure', label: 'Create Task', icon: Sparkles, description: 'Assignment generator' },
    { to: '/teacher/export', label: 'Reports', icon: FileText, description: 'Export analytics & grades' },
  ];

  const primaryLinks = isTeacher ? teacherPrimaryLinks : isStudent ? studentPrimaryLinks : [];
  const moreLinks = isTeacher ? teacherMoreLinks : isStudent ? studentMoreLinks : [];
  const isMoreActive = moreLinks.some(
    (link) => location.pathname === link.to || location.pathname.startsWith(link.to + '/')
  );

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <OfflineBanner />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center space-x-6">
            <Link to={user ? (isTeacher ? '/teacher' : '/student') : '/'} className="flex items-center space-x-2">
              <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-black text-lg shadow-md shadow-indigo-100">
                LO
              </div>
              <span className="font-bold text-lg text-slate-900 tracking-tight">Lingua Optima</span>
            </Link>

            {user && (
              <nav className="hidden md:flex items-center space-x-1">
                {primaryLinks.map((link) => {
                  const Icon = link.icon;
                  const isActive = location.pathname === link.to;
                  return (
                    <Link
                      key={link.to}
                      to={link.to}
                      className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                        isActive
                          ? 'bg-indigo-50 text-primary font-semibold'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                    >
                      <Icon className="w-4 h-4" />
                      <span>{link.label}</span>
                    </Link>
                  );
                })}

                {moreLinks.length > 0 && (
                  <div className="relative" ref={moreMenuRef}>
                    <button
                      type="button"
                      onClick={() => setIsMoreMenuOpen((prev) => !prev)}
                      className={`flex items-center space-x-1 px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                        isMoreActive || isMoreMenuOpen
                          ? 'bg-indigo-50 text-primary font-semibold'
                          : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                      }`}
                      aria-expanded={isMoreMenuOpen}
                    >
                      <span>More</span>
                      <ChevronDown
                        className={`w-3.5 h-3.5 transition-transform duration-200 ${
                          isMoreMenuOpen ? 'rotate-180' : ''
                        }`}
                      />
                    </button>

                    {isMoreMenuOpen && (
                      <div className="absolute left-0 mt-2 w-64 bg-white rounded-2xl shadow-xl border border-slate-100 p-2 animate-in fade-in zoom-in-95 duration-150 z-50">
                        <div className="px-2.5 py-1 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                          {isTeacher ? 'Teacher Tools' : 'Learning Tools'}
                        </div>
                        <div className="space-y-0.5 mt-1">
                          {moreLinks.map((link) => {
                            const Icon = link.icon;
                            const isActive = location.pathname === link.to;
                            return (
                              <Link
                                key={link.to}
                                to={link.to}
                                onClick={() => setIsMoreMenuOpen(false)}
                                className={`flex items-center space-x-3 px-2.5 py-2 rounded-xl text-sm transition ${
                                  isActive
                                    ? 'bg-indigo-50 text-primary font-semibold'
                                    : 'text-slate-700 hover:text-slate-900 hover:bg-slate-50'
                                }`}
                              >
                                <div
                                  className={`p-1.5 rounded-lg shrink-0 ${
                                    isActive
                                      ? 'bg-indigo-100 text-primary'
                                      : 'bg-slate-100 text-slate-600'
                                  }`}
                                >
                                  <Icon className="w-4 h-4" />
                                </div>
                                <div className="flex flex-col min-w-0">
                                  <span className="truncate leading-snug">{link.label}</span>
                                  {link.description && (
                                    <span className="text-[11px] text-slate-400 font-normal truncate leading-snug">
                                      {link.description}
                                    </span>
                                  )}
                                </div>
                              </Link>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </nav>
            )}
          </div>

          {user ? (
            <div className="flex items-center space-x-2 sm:space-x-3">
              <TranslatorDropdown />
              <button
                onClick={() => isQuotaExceeded && openUpgradeWall()}
                className={`hidden sm:flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-semibold border transition ${
                  isQuotaExceeded
                    ? 'bg-rose-50 text-rose-700 border-rose-200 hover:bg-rose-100'
                    : 'bg-indigo-50 text-primary border-indigo-200 hover:bg-indigo-100'
                }`}
                title={isQuotaExceeded ? 'Daily limit reached. Click to upgrade.' : 'Remaining daily evaluations'}
              >
                <Sparkles className="w-3.5 h-3.5" />
                <span>{remainingEvaluations} evaluations left</span>
              </button>

              <div className="relative" ref={notifMenuRef}>
                <button
                  onClick={() => setIsNotifOpen(!isNotifOpen)}
                  className="p-2 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-100 relative transition"
                  aria-label="Notifications"
                >
                  <Bell className="w-5 h-5" />
                  {unreadCount > 0 && (
                    <span className="absolute top-1 right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </button>

                {isNotifOpen && (
                  <div className="absolute right-0 mt-2 w-80 bg-white rounded-2xl shadow-xl border border-slate-100 p-3 animate-in fade-in zoom-in-95 duration-150">
                    <div className="flex items-center justify-between pb-2 border-b border-slate-100 px-2">
                      <span className="text-sm font-bold text-slate-900">Notifications</span>
                      <span className="text-xs text-slate-500 font-medium">{unreadCount} unread</span>
                    </div>
                    <div className="max-h-72 overflow-y-auto divide-y divide-slate-50 py-1">
                      {notifications.length === 0 ? (
                        <p className="text-xs text-slate-500 text-center py-6">No notifications yet</p>
                      ) : (
                        notifications.slice(0, 10).map((n) => (
                          <div
                            key={n.id}
                            onClick={() => !n.isRead && markAsRead(n.id)}
                            className={`p-2.5 rounded-lg text-xs cursor-pointer transition ${
                              n.isRead ? 'text-slate-500 hover:bg-slate-50' : 'bg-indigo-50/50 text-slate-800 font-medium'
                            }`}
                          >
                            <p>{n.message}</p>
                            <span className="text-[10px] text-slate-400 mt-1 block">{formatDate(n.createdAt)}</span>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>

              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center space-x-2 p-1.5 rounded-xl hover:bg-slate-100 transition"
                >
                  <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white font-bold flex items-center justify-center text-sm">
                    {user.fullName.charAt(0).toUpperCase()}
                  </div>
                  {user.cefrLevel && <CefrBadge level={user.cefrLevel} size="sm" />}
                </button>

                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-64 bg-white rounded-2xl shadow-xl border border-slate-100 p-2 animate-in fade-in zoom-in-95 duration-150">
                    <div className="px-3 py-2 border-b border-slate-100">
                      <div className="flex items-center justify-between gap-2">
                        <p className="text-sm font-bold text-slate-900 truncate">{user.fullName}</p>
                        <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-indigo-50 text-primary uppercase">
                          {user.role}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 truncate mt-0.5">{user.email}</p>
                    </div>

                    <div className="py-1">
                      <button
                        onClick={handleQuickRoleToggle}
                        disabled={isSwitchingRole}
                        className="w-full flex items-center justify-between px-3 py-2 rounded-lg text-sm text-indigo-700 hover:bg-indigo-50/70 font-semibold transition"
                      >
                        <div className="flex items-center space-x-2">
                          <GraduationCap className="w-4 h-4 text-primary" />
                          <span>{user.role === 'TEACHER' ? 'Switch to Student' : 'Switch to Educator'}</span>
                        </div>
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-indigo-100 text-indigo-700 font-bold">1-Click</span>
                      </button>

                      <Link
                        to="/profile"
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center space-x-2 px-3 py-2 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition"
                      >
                        <UserIcon className="w-4 h-4 text-slate-400" />
                        <span>Profile & API Keys</span>
                      </Link>

                      <Link
                        to="/subscription"
                        onClick={() => setIsUserMenuOpen(false)}
                        className="flex items-center space-x-2 px-3 py-2 rounded-lg text-sm text-slate-700 hover:bg-slate-50 transition"
                      >
                        <CreditCard className="w-4 h-4 text-slate-400" />
                        <span>Subscription & Limits</span>
                      </Link>
                    </div>

                    <div className="pt-1 border-t border-slate-100">
                      <button
                        onClick={handleLogout}
                        className="w-full flex items-center space-x-2 px-3 py-2 rounded-lg text-sm text-rose-600 hover:bg-rose-50 transition"
                      >
                        <LogOut className="w-4 h-4 text-rose-400" />
                        <span>Sign Out</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>

              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className="md:hidden p-2 rounded-xl text-slate-600 hover:bg-slate-100"
              >
                {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          ) : (
            <div className="flex items-center space-x-2 sm:space-x-3">
              <TranslatorDropdown />
              <Link
                to="/login"
                className="text-sm font-semibold text-slate-700 hover:text-primary transition px-3 py-2"
              >
                Sign In
              </Link>
              <Link
                to="/login?register=true"
                className="text-sm font-semibold text-white bg-primary hover:bg-primary-hover px-4 py-2 rounded-xl transition shadow-sm"
              >
                Get Started
              </Link>
            </div>
          )}
        </div>

        {isMobileMenuOpen && user && (
          <div className="md:hidden py-3 border-t border-slate-100 space-y-2">
            <div className="space-y-1">
              {primaryLinks.map((link) => {
                const Icon = link.icon;
                const isActive = location.pathname === link.to;
                return (
                  <Link
                    key={link.to}
                    to={link.to}
                    onClick={() => setIsMobileMenuOpen(false)}
                    className={`flex items-center space-x-2.5 px-3 py-2 rounded-lg text-sm font-medium transition ${
                      isActive
                        ? 'bg-indigo-50 text-primary font-semibold'
                        : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    <Icon className="w-4 h-4 text-slate-500" />
                    <span>{link.label}</span>
                  </Link>
                );
              })}
            </div>

            {moreLinks.length > 0 && (
              <div className="pt-2 border-t border-slate-100">
                <div className="px-3 pb-1.5 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  {isTeacher ? 'Teacher Tools' : 'More Tools'}
                </div>
                <div className="space-y-1">
                  {moreLinks.map((link) => {
                    const Icon = link.icon;
                    const isActive = location.pathname === link.to;
                    return (
                      <Link
                        key={link.to}
                        to={link.to}
                        onClick={() => setIsMobileMenuOpen(false)}
                        className={`flex items-center space-x-2.5 px-3 py-2 rounded-lg text-sm font-medium transition ${
                          isActive
                            ? 'bg-indigo-50 text-primary font-semibold'
                            : 'text-slate-700 hover:bg-slate-50'
                        }`}
                      >
                        <Icon className="w-4 h-4 text-slate-500" />
                        <span>{link.label}</span>
                      </Link>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
};
