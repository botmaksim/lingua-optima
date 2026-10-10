/**
 * @file StudentGroups.tsx
 * @brief Educator student group management view allowing group creation, enrollment, member soft-deletion, search, hide toggles, and pagination.
 */

import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  Trash2,
  UserPlus,
  Mail,
  Clock,
  Edit3,
  X,
  User as UserIcon,
  Search,
  Eye,
  EyeOff,
  ChevronLeft,
  ChevronRight,
  Filter,
  RotateCcw,
  FileText,
} from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { userApi } from '../../api/userApi';
import { Group } from '../../types/group';
import { User, CefrLevel } from '../../types/user';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { useNotificationStore } from '../../store/notificationStore';

const CEFR_ORDER: Record<string, number> = { A1: 1, A2: 2, B1: 3, B2: 4, C1: 5, C2: 6 };

/**
 * @brief Cohort management panel for educators to organize groups, add students, monitor enrollment, filter, and archive members.
 * @return JSX cohort management view.
 */
export const StudentGroups: React.FC = () => {
  const { addToast } = useNotificationStore();
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Cohort creation
  const [newGroupName, setNewGroupName] = useState('');
  const [isCreatingGroup, setIsCreatingGroup] = useState(false);

  // Cohort search
  const [cohortSearch, setCohortSearch] = useState('');

  // Add student
  const [studentEmail, setStudentEmail] = useState('');
  const [isAddingStudent, setIsAddingStudent] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Student renaming state
  const [renamingStudent, setRenamingStudent] = useState<User | null>(null);
  const [renameInput, setRenameInput] = useState('');
  const [isSavingName, setIsSavingName] = useState(false);

  // Hidden students state (localStorage persistence)
  const [hiddenStudentIds, setHiddenStudentIds] = useState<Set<string>>(() => {
    try {
      const raw = localStorage.getItem('lingua_hidden_student_ids');
      return raw ? new Set(JSON.parse(raw)) : new Set();
    } catch {
      return new Set();
    }
  });

  // Enrolled students tab, filters & pagination
  const [studentTab, setStudentTab] = useState<'active' | 'hidden'>('active');
  const [studentSearch, setStudentSearch] = useState('');
  const [studentCefrFilter, setStudentCefrFilter] = useState<'ALL' | CefrLevel>('ALL');
  const [studentSortBy, setStudentSortBy] = useState<'name_asc' | 'name_desc' | 'level'>('name_asc');
  const [studentPage, setStudentPage] = useState(1);
  const [studentPageSize, setStudentPageSize] = useState(10);

  // Confirm dialog state
  const [confirmConfig, setConfirmConfig] = useState<{
    isOpen: boolean;
    title: string;
    message: string;
    confirmText?: string;
    isDestructive?: boolean;
    onConfirm: () => void;
  }>({
    isOpen: false,
    title: '',
    message: '',
    onConfirm: () => {},
  });

  /**
   * @brief Loads all educator cohorts.
   */
  const loadGroups = async () => {
    try {
      const data = await groupApi.getGroups();
      setGroups(data);
      if (data.length > 0 && !selectedGroup) {
        loadGroupDetails(data[0].id);
      }
    } catch (err) {
      console.error('Failed to load groups:', err);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Loads comprehensive details for a single cohort including rosters.
   * @param id Cohort identifier.
   */
  const loadGroupDetails = async (id: string) => {
    try {
      const details = await groupApi.getGroupDetails(id);
      setSelectedGroup(details);
    } catch (err) {
      console.error('Failed to load group details:', err);
    }
  };

  useEffect(() => {
    loadGroups();
  }, []);

  /**
   * @brief Toggles hidden status of a student in the roster with localStorage sync.
   * @param studentId Unique identifier of the student.
   */
  const handleToggleHideStudent = (studentId: string) => {
    setHiddenStudentIds((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) {
        next.delete(studentId);
      } else {
        next.add(studentId);
      }
      try {
        localStorage.setItem('lingua_hidden_student_ids', JSON.stringify([...next]));
      } catch (err) {
        console.warn('Failed to save hidden student IDs:', err);
      }
      return next;
    });
  };

  /**
   * @brief Restores all hidden students back to the active roster view.
   */
  const handleUnhideAllStudents = () => {
    setHiddenStudentIds(new Set());
    try {
      localStorage.removeItem('lingua_hidden_student_ids');
    } catch (err) {
      console.warn('Failed to clear hidden student IDs:', err);
    }
    addToast({
      type: 'info',
      title: 'Students Restored',
      message: 'All hidden students restored to the active roster.',
    });
  };

  /**
   * @brief Creates a new study group cohort.
   * @param e Form submit event.
   */
  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGroupName.trim()) return;
    setIsCreatingGroup(true);
    try {
      const created = await groupApi.createGroup({ name: newGroupName.trim() });
      addToast({
        type: 'success',
        title: 'Cohort Created',
        message: `Group "${created.name}" created successfully.`,
      });
      setNewGroupName('');
      await loadGroups();
      await loadGroupDetails(created.id);
    } catch (err) {
      console.error('Failed to create group:', err);
      addToast({
        type: 'error',
        title: 'Creation Failed',
        message: 'Could not create cohort group.',
      });
    } finally {
      setIsCreatingGroup(false);
    }
  };

  /**
   * @brief Dispatches an invitation email to enroll a student in the selected cohort.
   * @param e Form submit event.
   */
  const handleAddStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedGroup || !studentEmail.trim()) return;
    setIsAddingStudent(true);
    setActionMessage(null);

    try {
      await groupApi.addStudent(selectedGroup.id, studentEmail.trim());
      addToast({
        type: 'success',
        title: 'Invitation Sent',
        message: `Invitation sent to ${studentEmail.trim()} for group "${selectedGroup.name}". Waiting for student to accept.`,
      });
      setStudentEmail('');
      setActionMessage('Invitation dispatched! Student must accept to become an active cohort member.');
      setTimeout(() => setActionMessage(null), 4000);
      await loadGroupDetails(selectedGroup.id);
      await loadGroups();
    } catch (err: any) {
      console.error('Failed to add student:', err);
      const errMsg = err.response?.data?.message || 'Failed to add student.';
      setActionMessage(errMsg);
      addToast({
        type: 'error',
        title: 'Enrollment Failed',
        message: errMsg,
      });
    } finally {
      setIsAddingStudent(false);
    }
  };

  /**
   * @brief Opens rename modal for an enrolled student account.
   * @param student Target student user entity.
   */
  const handleOpenRename = (student: User) => {
    setRenamingStudent(student);
    setRenameInput(student.fullName);
  };

  /**
   * @brief Saves updated student name across submissions and class roster.
   */
  const handleSaveRename = async () => {
    if (!renamingStudent || !renameInput.trim()) return;
    setIsSavingName(true);
    try {
      const updatedName = renameInput.trim();
      await userApi.updateStudentName(renamingStudent.id, updatedName);

      setSelectedGroup((prev) =>
        prev
          ? {
              ...prev,
              students: prev.students?.map((s) =>
                s.id === renamingStudent.id ? { ...s, fullName: updatedName } : s
              ) || [],
            }
          : null
      );

      setGroups((prev) =>
        prev.map((g) => ({
          ...g,
          students: g.students?.map((s) =>
            s.id === renamingStudent.id ? { ...s, fullName: updatedName } : s
          ) || [],
        }))
      );

      addToast({
        type: 'success',
        title: 'Student Renamed',
        message: `Student account name updated to "${updatedName}".`,
      });
      setRenamingStudent(null);
    } catch (err: any) {
      console.error('Failed to rename student:', err);
      addToast({
        type: 'error',
        title: 'Rename Failed',
        message: err.response?.data?.message || 'Could not update student name.',
      });
    } finally {
      setIsSavingName(false);
    }
  };

  /**
   * @brief Confirms and executes removal of a student from a cohort.
   * @param studentId Target student identifier.
   */
  const handleRemoveStudent = (studentId: string) => {
    if (!selectedGroup) return;
    setConfirmConfig({
      isOpen: true,
      title: 'Remove Student from Cohort',
      message: 'Their historical records will remain safe and restored if re-added.',
      confirmText: 'Remove Student',
      isDestructive: true,
      onConfirm: async () => {
        setConfirmConfig((prev) => ({ ...prev, isOpen: false }));
        try {
          await groupApi.removeStudent(selectedGroup.id, studentId);
          addToast({
            type: 'info',
            title: 'Student Removed',
            message: 'Student has been removed from this cohort.',
          });
          await loadGroupDetails(selectedGroup.id);
          await loadGroups();
        } catch (err) {
          console.error('Failed to remove student:', err);
          addToast({
            type: 'error',
            title: 'Action Failed',
            message: 'Failed to remove student from cohort.',
          });
        }
      },
    });
  };

  /**
   * @brief Confirms and executes cohort deletion.
   */
  const handleDeleteGroup = () => {
    if (!selectedGroup) return;
    const groupName = selectedGroup.name;
    const groupId = selectedGroup.id;

    setConfirmConfig({
      isOpen: true,
      title: 'Delete Cohort Group',
      message: `Are you sure you want to delete "${groupName}"? Student submission histories will be preserved.`,
      confirmText: 'Delete Group',
      isDestructive: true,
      onConfirm: async () => {
        setConfirmConfig((prev) => ({ ...prev, isOpen: false }));
        try {
          await groupApi.deleteGroup(groupId);
          addToast({
            type: 'info',
            title: 'Group Deleted',
            message: `Cohort "${groupName}" has been deleted.`,
          });
          setSelectedGroup(null);
          await loadGroups();
        } catch (err) {
          console.error('Failed to delete group:', err);
          addToast({
            type: 'error',
            title: 'Action Failed',
            message: 'Failed to delete cohort group.',
          });
        }
      },
    });
  };

  // Filtered cohorts for sidebar
  const filteredCohorts = useMemo(() => {
    if (!cohortSearch.trim()) return groups;
    const q = cohortSearch.toLowerCase().trim();
    return groups.filter((g) => g.name.toLowerCase().includes(q));
  }, [groups, cohortSearch]);

  // Enrolled students filtering, tabs, and sorting
  const allStudents = selectedGroup?.students || [];
  const activeStudentsCount = useMemo(() => allStudents.filter((s) => !hiddenStudentIds.has(s.id)).length, [allStudents, hiddenStudentIds]);
  const hiddenStudentsCount = useMemo(() => allStudents.filter((s) => hiddenStudentIds.has(s.id)).length, [allStudents, hiddenStudentIds]);

  const filteredStudents = useMemo(() => {
    return allStudents
      .filter((student) => {
        const isHidden = hiddenStudentIds.has(student.id);
        if (studentTab === 'active' && isHidden) return false;
        if (studentTab === 'hidden' && !isHidden) return false;

        // CEFR filter
        if (studentCefrFilter !== 'ALL' && student.cefrLevel !== studentCefrFilter) {
          return false;
        }

        // Search query
        if (studentSearch.trim()) {
          const q = studentSearch.toLowerCase().trim();
          const matchName = student.fullName.toLowerCase().includes(q);
          const matchEmail = student.email.toLowerCase().includes(q);
          if (!matchName && !matchEmail) return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (studentSortBy === 'name_asc') {
          return a.fullName.localeCompare(b.fullName);
        }
        if (studentSortBy === 'name_desc') {
          return b.fullName.localeCompare(a.fullName);
        }
        if (studentSortBy === 'level') {
          const levA = a.cefrLevel ? CEFR_ORDER[a.cefrLevel] || 0 : 0;
          const levB = b.cefrLevel ? CEFR_ORDER[b.cefrLevel] || 0 : 0;
          return levB - levA;
        }
        return 0;
      });
  }, [allStudents, hiddenStudentIds, studentTab, studentCefrFilter, studentSearch, studentSortBy]);

  // Reset student page on filter change
  useEffect(() => {
    setStudentPage(1);
  }, [studentSearch, studentCefrFilter, studentSortBy, studentTab, studentPageSize, selectedGroup?.id]);

  const totalStudentPages = Math.max(1, Math.ceil(filteredStudents.length / studentPageSize));
  const paginatedStudents = useMemo(() => {
    const start = (studentPage - 1) * studentPageSize;
    return filteredStudents.slice(start, start + studentPageSize);
  }, [filteredStudents, studentPage, studentPageSize]);

  const hasStudentFilters = studentSearch.trim() !== '' || studentCefrFilter !== 'ALL' || studentSortBy !== 'name_asc';

  const resetStudentFilters = () => {
    setStudentSearch('');
    setStudentCefrFilter('ALL');
    setStudentSortBy('name_asc');
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading your class groups..." />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Student Cohorts</h1>
        <p className="text-sm text-slate-500 mt-1">
          Organize your students into groups. Search, filter, archive student profiles, and maintain group rosters.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Cohorts Sidebar */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
              Create New Group
            </h2>
            <form onSubmit={handleCreateGroup} className="flex gap-2">
              <input
                type="text"
                placeholder="e.g. Advanced B2 Evening"
                value={newGroupName}
                onChange={(e) => setNewGroupName(e.target.value)}
                className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
              <button
                type="submit"
                disabled={isCreatingGroup || !newGroupName.trim()}
                className="p-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white transition disabled:opacity-50"
                title="Create Group"
              >
                <Plus className="w-5 h-5" />
              </button>
            </form>
          </div>

          <div className="bg-white rounded-3xl p-4 border border-slate-100 shadow-sm space-y-3">
            <div className="flex items-center justify-between px-2 pt-1">
              <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Cohorts ({groups.length})
              </h2>
            </div>

            {/* Cohorts search if more than 3 groups */}
            {groups.length > 3 && (
              <div className="relative px-1">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Filter cohorts..."
                  value={cohortSearch}
                  onChange={(e) => setCohortSearch(e.target.value)}
                  className="w-full pl-8 pr-3 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                />
              </div>
            )}

            <div className="space-y-1">
              {groups.length === 0 ? (
                <p className="text-xs text-slate-400 p-4 text-center">No groups created yet</p>
              ) : filteredCohorts.length === 0 ? (
                <p className="text-xs text-slate-400 p-4 text-center">No cohorts match "{cohortSearch}"</p>
              ) : (
                filteredCohorts.map((g) => {
                  const isSelected = selectedGroup?.id === g.id;
                  return (
                    <button
                      key={g.id}
                      onClick={() => loadGroupDetails(g.id)}
                      className={`w-full text-left p-3.5 rounded-2xl transition flex items-center justify-between ${
                        isSelected
                          ? 'bg-indigo-50/80 text-primary font-bold shadow-sm'
                          : 'hover:bg-slate-50 text-slate-700 font-medium'
                      }`}
                    >
                      <span className="text-sm truncate">{g.name}</span>
                      <span className="text-xs px-2 py-0.5 rounded-full bg-slate-200/60 text-slate-600 font-mono">
                        {g.studentCount}
                      </span>
                    </button>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {/* Right Cohort Details & Enrolled Students */}
        <div className="lg:col-span-2 space-y-6">
          {selectedGroup ? (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                <div>
                  <h2 className="text-xl font-bold text-slate-900">{selectedGroup.name}</h2>
                  <p className="text-xs text-slate-400 mt-0.5">
                    {selectedGroup.studentCount} Students ·{' '}
                    {selectedGroup.avgScore != null ? `Average Score: ${selectedGroup.avgScore}%` : 'No submissions yet'}
                  </p>
                </div>

                <div className="flex items-center space-x-2">
                  <Link
                    to="/teacher/export"
                    className="flex items-center space-x-1.5 text-xs font-semibold text-primary hover:text-primary-hover p-2 rounded-xl hover:bg-indigo-50 transition"
                  >
                    <FileText className="w-4 h-4" />
                    <span>Export Report</span>
                  </Link>
                  <button
                    type="button"
                    onClick={handleDeleteGroup}
                    className="flex items-center space-x-1.5 text-xs font-semibold text-rose-600 hover:text-rose-700 p-2 rounded-xl hover:bg-rose-50 transition"
                  >
                    <Trash2 className="w-4 h-4" />
                    <span>Delete Group</span>
                  </button>
                </div>
              </div>

              {/* Add Student by Email */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-3">
                <div className="flex items-center space-x-2 text-xs font-bold text-slate-700">
                  <UserPlus className="w-4 h-4 text-primary" />
                  <span>Add Student by Email (Re-activation restores progress)</span>
                </div>
                <form onSubmit={handleAddStudent} className="flex gap-2">
                  <div className="relative flex-1">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="email"
                      placeholder="student@example.com"
                      value={studentEmail}
                      onChange={(e) => setStudentEmail(e.target.value)}
                      className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-white border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={isAddingStudent || !studentEmail.trim()}
                    className="px-4 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition disabled:opacity-50"
                  >
                    Add
                  </button>
                </form>

                {actionMessage && (
                  <p className="text-xs font-medium text-indigo-700">{actionMessage}</p>
                )}
              </div>

              {/* Pending Invitations Section */}
              {selectedGroup.pendingStudents && selectedGroup.pendingStudents.length > 0 && (
                <div className="space-y-3 p-4 rounded-2xl bg-amber-50/60 border border-amber-200/70">
                  <div className="flex items-center justify-between">
                    <h3 className="text-xs font-bold text-amber-900 uppercase tracking-wider flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-amber-600" />
                      <span>Pending Invitations ({selectedGroup.pendingStudents.length})</span>
                    </h3>
                  </div>

                  <div className="divide-y divide-amber-100">
                    {selectedGroup.pendingStudents.map((student) => (
                      <div
                        key={student.id}
                        className="py-2.5 flex items-center justify-between px-1"
                      >
                        <div className="space-y-0.5">
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-bold text-slate-800">{student.fullName}</span>
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300/60">
                              Invitation Pending
                            </span>
                          </div>
                          <span className="text-xs text-slate-500">{student.email}</span>
                        </div>

                        <button
                          type="button"
                          onClick={() => handleRemoveStudent(student.id)}
                          className="text-xs text-rose-600 hover:text-rose-700 font-semibold transition px-2.5 py-1 rounded-lg hover:bg-rose-50"
                          title="Revoke invitation"
                        >
                          Revoke
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Enrolled Students Section */}
              <div className="space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                    Enrolled Students ({allStudents.length})
                  </h3>

                  {/* Active vs Hidden Roster Tabs */}
                  <div className="flex items-center space-x-1.5 p-1 bg-slate-100 rounded-2xl shrink-0 self-start sm:self-center">
                    <button
                      type="button"
                      onClick={() => setStudentTab('active')}
                      className={`flex items-center space-x-1.5 px-3 py-1 rounded-xl text-xs font-bold transition ${
                        studentTab === 'active'
                          ? 'bg-white text-slate-900 shadow-sm'
                          : 'text-slate-500 hover:text-slate-800'
                      }`}
                    >
                      <span>Active</span>
                      <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                        studentTab === 'active' ? 'bg-indigo-50 text-primary' : 'bg-slate-200/60 text-slate-600'
                      }`}>
                        {activeStudentsCount}
                      </span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setStudentTab('hidden')}
                      className={`flex items-center space-x-1.5 px-3 py-1 rounded-xl text-xs font-bold transition ${
                        studentTab === 'hidden'
                          ? 'bg-white text-slate-900 shadow-sm'
                          : 'text-slate-500 hover:text-slate-800'
                      }`}
                    >
                      <EyeOff className="w-3.5 h-3.5" />
                      <span>Hidden</span>
                      <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                        studentTab === 'hidden' ? 'bg-amber-100 text-amber-800' : 'bg-slate-200/60 text-slate-600'
                      }`}>
                        {hiddenStudentsCount}
                      </span>
                    </button>
                  </div>
                </div>

                {/* Search & Filter Toolbar for Enrolled Students */}
                {allStudents.length > 0 && (
                  <div className="p-3 bg-slate-50/80 rounded-2xl border border-slate-100 space-y-2.5">
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                      {/* Name / Email Search */}
                      <div className="relative">
                        <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                        <input
                          type="text"
                          value={studentSearch}
                          onChange={(e) => setStudentSearch(e.target.value)}
                          placeholder="Search student name or email..."
                          className="w-full pl-8 pr-7 py-1.5 rounded-xl bg-white border border-slate-200 text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                        />
                        {studentSearch && (
                          <button
                            type="button"
                            onClick={() => setStudentSearch('')}
                            className="absolute right-2 top-1/2 -translate-y-1/2 p-0.5 text-slate-400 hover:text-slate-600"
                          >
                            <X className="w-3 h-3" />
                          </button>
                        )}
                      </div>

                      {/* CEFR Level Filter */}
                      <select
                        value={studentCefrFilter}
                        onChange={(e) => setStudentCefrFilter(e.target.value as any)}
                        className="px-2.5 py-1.5 rounded-xl bg-white border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                      >
                        <option value="ALL">All CEFR Levels</option>
                        <option value="A1">A1 Beginner</option>
                        <option value="A2">A2 Elementary</option>
                        <option value="B1">B1 Intermediate</option>
                        <option value="B2">B2 Upper Intermediate</option>
                        <option value="C1">C1 Advanced</option>
                        <option value="C2">C2 Proficiency</option>
                      </select>

                      {/* Sort Selector */}
                      <select
                        value={studentSortBy}
                        onChange={(e) => setStudentSortBy(e.target.value as any)}
                        className="px-2.5 py-1.5 rounded-xl bg-white border border-slate-200 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                      >
                        <option value="name_asc">Sort: Name (A-Z)</option>
                        <option value="name_desc">Sort: Name (Z-A)</option>
                        <option value="level">Sort: Highest CEFR</option>
                      </select>
                    </div>

                    {/* Filter info and reset */}
                    <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
                      <div className="flex items-center gap-2">
                        <span>Showing {filteredStudents.length} student{filteredStudents.length === 1 ? '' : 's'}</span>
                        {hasStudentFilters && (
                          <button
                            type="button"
                            onClick={resetStudentFilters}
                            className="inline-flex items-center space-x-1 text-primary hover:text-primary-hover font-bold"
                          >
                            <RotateCcw className="w-3 h-3" />
                            <span>Reset</span>
                          </button>
                        )}
                      </div>

                      {studentTab === 'hidden' && hiddenStudentsCount > 0 && (
                        <button
                          type="button"
                          onClick={handleUnhideAllStudents}
                          className="inline-flex items-center space-x-1 text-amber-700 hover:text-amber-800 font-bold"
                        >
                          <Eye className="w-3 h-3" />
                          <span>Restore All Hidden</span>
                        </button>
                      )}
                    </div>
                  </div>
                )}

                {allStudents.length === 0 ? (
                  <p className="text-sm text-slate-500 py-6 text-center">
                    No active students in this cohort. Invite students via email above.
                  </p>
                ) : filteredStudents.length === 0 ? (
                  <div className="p-8 text-center text-slate-400 text-xs space-y-2">
                    {studentTab === 'hidden' ? (
                      <>
                        <EyeOff className="w-6 h-6 mx-auto text-slate-300 mb-1" />
                        <p className="font-semibold text-slate-600">No hidden students.</p>
                        <p>Click "Hide" on any student to archive them from the active list.</p>
                      </>
                    ) : (
                      <>
                        <Filter className="w-6 h-6 mx-auto text-slate-300 mb-1" />
                        <p className="font-semibold text-slate-600">No students match your filter criteria.</p>
                        <button
                          type="button"
                          onClick={resetStudentFilters}
                          className="text-primary font-bold hover:underline"
                        >
                          Clear filters
                        </button>
                      </>
                    )}
                  </div>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {paginatedStudents.map((student) => {
                      const isHidden = hiddenStudentIds.has(student.id);

                      return (
                        <div
                          key={student.id}
                          className="py-3 flex items-center justify-between hover:bg-slate-50/50 px-2 rounded-xl transition"
                        >
                          <div className="space-y-0.5">
                            <div className="flex items-center space-x-2">
                              <span className="text-sm font-bold text-slate-800">{student.fullName}</span>
                              <button
                                type="button"
                                onClick={() => handleOpenRename(student)}
                                className="p-1 rounded-lg text-slate-400 hover:text-primary hover:bg-slate-100 transition"
                                title="Rename student for reports & review"
                              >
                                <Edit3 className="w-3.5 h-3.5" />
                              </button>
                              {student.cefrLevel && <CefrBadge level={student.cefrLevel} size="sm" />}
                            </div>
                            <span className="text-xs text-slate-400">{student.email}</span>
                          </div>

                          <div className="flex items-center space-x-1">
                            {/* Hide / Unhide Action */}
                            <button
                              type="button"
                              onClick={() => handleToggleHideStudent(student.id)}
                              className={`p-1.5 rounded-lg text-xs font-semibold transition ${
                                isHidden
                                  ? 'text-amber-700 bg-amber-50 hover:bg-amber-100'
                                  : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100'
                              }`}
                              title={isHidden ? 'Unhide student (restore to active)' : 'Hide student from active list'}
                            >
                              {isHidden ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                            </button>

                            {/* Remove from cohort */}
                            <button
                              type="button"
                              onClick={() => handleRemoveStudent(student.id)}
                              className="text-slate-400 hover:text-rose-600 transition p-1.5 rounded-lg hover:bg-rose-50"
                              title="Remove student (soft-delete)"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Pagination Controls for Enrolled Students */}
                {filteredStudents.length > studentPageSize && (
                  <div className="pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
                    <div>
                      Showing <span className="font-bold text-slate-800">{(studentPage - 1) * studentPageSize + 1}</span> to{' '}
                      <span className="font-bold text-slate-800">
                        {Math.min(studentPage * studentPageSize, filteredStudents.length)}
                      </span>{' '}
                      of <span className="font-bold text-slate-800">{filteredStudents.length}</span>
                    </div>

                    <div className="flex items-center space-x-3">
                      <div className="flex items-center space-x-1.5">
                        <span>Per page:</span>
                        <select
                          value={studentPageSize}
                          onChange={(e) => setStudentPageSize(Number(e.target.value))}
                          className="px-2 py-0.5 rounded-lg bg-slate-100 border border-slate-200 text-xs font-bold text-slate-700 focus:outline-none"
                        >
                          <option value={5}>5</option>
                          <option value={10}>10</option>
                          <option value={20}>20</option>
                        </select>
                      </div>

                      <div className="flex items-center space-x-1.5">
                        <button
                          type="button"
                          onClick={() => setStudentPage((p) => Math.max(1, p - 1))}
                          disabled={studentPage === 1}
                          className="p-1 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition disabled:opacity-40"
                          title="Previous page"
                        >
                          <ChevronLeft className="w-3.5 h-3.5" />
                        </button>

                        <span className="px-2 font-bold text-slate-700">
                          {studentPage} / {totalStudentPages}
                        </span>

                        <button
                          type="button"
                          onClick={() => setStudentPage((p) => Math.min(totalStudentPages, p + 1))}
                          disabled={studentPage === totalStudentPages}
                          className="p-1 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-100 transition disabled:opacity-40"
                          title="Next page"
                        >
                          <ChevronRight className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="bg-white rounded-3xl p-12 text-center text-slate-500 border border-slate-100">
              Select or create a cohort to view enrolled students.
            </div>
          )}
        </div>
      </div>

      {/* Student Account Rename Modal */}
      {renamingStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <UserIcon className="w-4 h-4 text-primary" />
                <span>Rename Student Account</span>
              </h3>
              <button
                type="button"
                onClick={() => setRenamingStudent(null)}
                className="p-1 rounded-xl text-slate-400 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-500 leading-relaxed">
              Set a recognizable display name for this student ({renamingStudent.email}). This custom name will appear across your group rosters, submissions queue, and printed PDF/CSV reports.
            </p>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1.5">
                Student Full Name / Alias
              </label>
              <input
                type="text"
                value={renameInput}
                onChange={(e) => setRenameInput(e.target.value)}
                placeholder="e.g. John Doe / Иванов Иван"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                autoFocus
              />
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setRenamingStudent(null)}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveRename}
                disabled={isSavingName || !renameInput.trim()}
                className="px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
              >
                {isSavingName ? 'Saving...' : 'Save Name'}
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        isOpen={confirmConfig.isOpen}
        title={confirmConfig.title}
        message={confirmConfig.message}
        confirmText={confirmConfig.confirmText}
        isDestructive={confirmConfig.isDestructive}
        onConfirm={confirmConfig.onConfirm}
        onCancel={() => setConfirmConfig((prev) => ({ ...prev, isOpen: false }))}
      />
    </div>
  );
};
