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
  Lock,
  ArrowRightLeft,
  BookOpen,
} from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { userApi } from '../../api/userApi';
import { Group, GroupTask } from '../../types/group';
import { User, CefrLevel } from '../../types/user';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { useNotificationStore } from '../../store/notificationStore';
import { useUIStore } from '../../store/uiStore';

const CEFR_ORDER: Record<string, number> = { A1: 1, A2: 2, B1: 3, B2: 4, C1: 5, C2: 6 };

/**
 * @brief Cohort management panel for educators to organize groups, add students, monitor enrollment, filter, and archive members.
 * @return JSX cohort management view.
 */
export const StudentGroups: React.FC = () => {
  const { addToast } = useNotificationStore();
  const { openUpgradeWall } = useUIStore();
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

  // Cohort primary tab (Roster vs Assigned Tasks)
  const [cohortMainTab, setCohortMainTab] = useState<'roster' | 'tasks'>('roster');
  const [groupTasks, setGroupTasks] = useState<GroupTask[]>([]);
  const [isLoadingTasks, setIsLoadingTasks] = useState<boolean>(false);

  // Transfer / Move task modal state
  const [transferModalTask, setTransferModalTask] = useState<GroupTask | null>(null);
  const [transferTargetGroupId, setTransferTargetGroupId] = useState<string>('');
  const [transferKeepInSource, setTransferKeepInSource] = useState<boolean>(false);
  const [transferDueDate, setTransferDueDate] = useState<string>('');
  const [transferMaxAttempts, setTransferMaxAttempts] = useState<number>(1);
  const [isTransferring, setIsTransferring] = useState<boolean>(false);

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
   * @brief Loads all tasks assigned to the selected cohort.
   * @param id Cohort identifier.
   */
  const loadGroupTasks = async (id: string) => {
    setIsLoadingTasks(true);
    try {
      const tasks = await groupApi.getGroupTasks(id);
      setGroupTasks(tasks);
    } catch (err) {
      console.error('Failed to load group tasks:', err);
    } finally {
      setIsLoadingTasks(false);
    }
  };

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
   * @brief Loads comprehensive details for a single cohort including rosters and tasks.
   * @param id Cohort identifier.
   */
  const loadGroupDetails = async (id: string) => {
    try {
      const details = await groupApi.getGroupDetails(id);
      setSelectedGroup(details);
      loadGroupTasks(id);
    } catch (err) {
      console.error('Failed to load group details:', err);
    }
  };

  /**
   * @brief Opens the transfer / copy modal for an assigned task.
   * @param task Target assigned task.
   */
  const handleOpenTransferModal = (task: GroupTask) => {
    setTransferModalTask(task);
    const availableGroups = groups.filter((g) => g.id !== selectedGroup?.id);
    setTransferTargetGroupId(availableGroups.length > 0 ? availableGroups[0].id : '');
    setTransferKeepInSource(false);
    setTransferDueDate(task.dueDate || '');
    setTransferMaxAttempts(task.maxAttempts ?? 1);
  };

  /**
   * @brief Dispatches the task transfer or copy request to backend.
   * @param e Form submit event.
   */
  const handleExecuteTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedGroup || !transferModalTask || !transferTargetGroupId) return;
    setIsTransferring(true);
    try {
      const targetGroup = groups.find((g) => g.id === transferTargetGroupId);
      await groupApi.transferTask(selectedGroup.id, transferModalTask.taskId, {
        targetGroupId: transferTargetGroupId,
        keepInSource: transferKeepInSource,
        dueDate: transferDueDate || undefined,
        maxAttempts: transferMaxAttempts,
      });
      addToast({
        type: 'success',
        title: transferKeepInSource ? 'Task Copied' : 'Task Transferred',
        message: transferKeepInSource
          ? `Assignment "${transferModalTask.grammarTopic}" copied to group "${targetGroup?.name}".`
          : `Assignment "${transferModalTask.grammarTopic}" moved to group "${targetGroup?.name}".`,
      });
      setTransferModalTask(null);
      await loadGroupTasks(selectedGroup.id);
      await loadGroups();
    } catch (err: any) {
      console.error('Failed to transfer task:', err);
      const msg = err.response?.data?.message || 'Failed to transfer task.';
      addToast({
        type: 'error',
        title: 'Transfer Failed',
        message: msg,
      });
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(msg);
      }
    } finally {
      setIsTransferring(false);
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
    } catch (err: any) {
      console.error('Failed to create group:', err);
      const msg = err.response?.data?.message || 'Could not create cohort group.';
      addToast({
        type: 'error',
        title: 'Creation Failed',
        message: msg,
      });
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(msg);
      }
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
                      <div className="flex items-center space-x-1.5 truncate">
                        {g.isLocked && <Lock className="w-3.5 h-3.5 text-amber-500 flex-shrink-0" />}
                        <span className="text-sm truncate">{g.name}</span>
                      </div>
                      <div className="flex items-center space-x-1.5 flex-shrink-0">
                        {g.isLocked && (
                          <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-800 font-bold">
                            Read-Only
                          </span>
                        )}
                        <span className="text-xs px-2 py-0.5 rounded-full bg-slate-200/60 text-slate-600 font-mono">
                          {g.studentCount}
                        </span>
                      </div>
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
              {selectedGroup.isLocked && (
                <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 flex items-start space-x-3 text-xs text-amber-950">
                  <Lock className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold">Cohort in Read-Only Mode: </span>
                    Your Free plan allows 1 active cohort. All historical student submissions, rosters, and analytics for this group are preserved safely. Upgrade to Educator Pro to reactivate assignment distribution for all cohorts.
                  </div>
                </div>
              )}
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

              {/* Primary Cohort View Tabs: Students & Roster vs Assigned Tasks */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-3">
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setCohortMainTab('roster')}
                    className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center space-x-2 ${
                      cohortMainTab === 'roster'
                        ? 'bg-primary text-white shadow-sm'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    <UserIcon className="w-4 h-4" />
                    <span>Students & Roster</span>
                    <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                      cohortMainTab === 'roster' ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-700'
                    }`}>
                      {selectedGroup.studentCount}
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setCohortMainTab('tasks')}
                    className={`px-4 py-2 rounded-xl text-xs font-bold transition flex items-center space-x-2 ${
                      cohortMainTab === 'tasks'
                        ? 'bg-primary text-white shadow-sm'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    <BookOpen className="w-4 h-4" />
                    <span>Assigned Tasks</span>
                    <span className={`px-1.5 py-0.2 rounded-full text-[10px] ${
                      cohortMainTab === 'tasks' ? 'bg-white/20 text-white' : 'bg-slate-200 text-slate-700'
                    }`}>
                      {groupTasks.length}
                    </span>
                  </button>
                </div>

                <Link
                  to={`/teacher/configure?groupId=${selectedGroup.id}`}
                  className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Assign / Generate Task</span>
                </Link>
              </div>

              {cohortMainTab === 'roster' ? (
                <>
                  {/* Add Student by Email */}
              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-3">
                <div className="flex items-center space-x-2 text-xs font-bold text-slate-700">
                  <UserPlus className="w-4 h-4 text-primary" />
                  <span>Add Student by Email</span>
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
                      <span>Pending Invitations · {selectedGroup.pendingStudents.length}</span>
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
                        <option value="name_asc">Sort: Name A–Z</option>
                        <option value="name_desc">Sort: Name Z–A</option>
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
            </>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                  Tasks Assigned to {selectedGroup.name} ({groupTasks.length})
                </h3>
              </div>

              {isLoadingTasks ? (
                <LoadingSpinner size="sm" message="Loading assigned tasks..." />
              ) : groupTasks.length === 0 ? (
                <div className="p-12 text-center text-slate-400 text-xs space-y-3 bg-slate-50/50 rounded-2xl border border-dashed border-slate-200">
                  <BookOpen className="w-8 h-8 mx-auto text-slate-300" />
                  <p className="font-semibold text-slate-700 text-sm">No tasks assigned to this cohort yet</p>
                  <p className="text-slate-500 max-w-sm mx-auto">
                    Generate new AI assignments or assign curriculum practice tasks directly to this group.
                  </p>
                  <Link
                    to={`/teacher/configure?groupId=${selectedGroup.id}`}
                    className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-sm"
                  >
                    <Plus className="w-4 h-4" />
                    <span>Generate Task for {selectedGroup.name}</span>
                  </Link>
                </div>
              ) : (
                <div className="space-y-3">
                  {groupTasks.map((t) => (
                    <div
                      key={t.taskId}
                      className="p-4 rounded-2xl bg-white border border-slate-200/80 hover:border-slate-300 shadow-sm space-y-3 transition"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div className="flex flex-wrap items-center gap-2">
                          <CefrBadge level={t.cefrLevel as CefrLevel} size="sm" />
                          <span className="text-xs font-bold px-2 py-0.5 rounded-lg bg-slate-100 text-slate-700">
                            {t.type}
                          </span>
                          <span className="text-xs font-bold text-slate-900">{t.grammarTopic}</span>
                          <span className="text-[11px] font-semibold text-slate-400 bg-slate-50 px-2 py-0.5 rounded-md border border-slate-100">
                            {t.domain}
                          </span>
                        </div>

                        <div className="flex items-center space-x-2 self-start sm:self-auto">
                          <button
                            type="button"
                            onClick={() => handleOpenTransferModal(t)}
                            className="inline-flex items-center space-x-1 px-3 py-1.5 rounded-xl border border-indigo-200 bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition shadow-sm"
                            title="Move or copy this assignment to another cohort group"
                          >
                            <ArrowRightLeft className="w-3.5 h-3.5" />
                            <span>Transfer / Copy</span>
                          </button>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                        <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                          <span className="text-[10px] font-bold text-slate-400 uppercase block mb-0.5">Total Points</span>
                          <span className="font-bold text-slate-800">{t.totalPoints} pts</span>
                        </div>

                        <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                          <span className="text-[10px] font-bold text-slate-400 uppercase block mb-0.5">Submissions</span>
                          <span className="font-bold text-slate-800">
                            {t.completedStudentCount} / {t.assignedStudentCount}
                            <span className="text-slate-400 font-normal ml-1">
                              ({t.assignedStudentCount > 0 ? Math.round((t.completedStudentCount / t.assignedStudentCount) * 100) : 0}%)
                            </span>
                          </span>
                        </div>

                        <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                          <span className="text-[10px] font-bold text-slate-400 uppercase block mb-0.5">Average Score</span>
                          <span className="font-bold text-slate-800">
                            {t.avgScore != null ? `${t.avgScore}%` : 'No grades yet'}
                          </span>
                        </div>

                        <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-100">
                          <span className="text-[10px] font-bold text-slate-400 uppercase block mb-0.5">Due Date</span>
                          <span className="font-medium text-slate-700 truncate block">
                            {t.dueDate ? new Date(t.dueDate).toLocaleDateString() : 'No deadline'}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
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

      {/* Transfer / Copy Assignment Modal */}
      {transferModalTask && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-5 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 rounded-xl bg-indigo-50 text-primary flex items-center justify-center">
                  <ArrowRightLeft className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Transfer Assignment</h3>
                  <p className="text-xs text-slate-400">
                    From cohort: <strong className="text-slate-700">{selectedGroup?.name}</strong>
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setTransferModalTask(null)}
                className="p-1 rounded-xl text-slate-400 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Task summary */}
            <div className="p-3 bg-slate-50 rounded-2xl border border-slate-200/80 flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <CefrBadge level={transferModalTask.cefrLevel as CefrLevel} size="sm" />
                <div>
                  <span className="text-xs font-bold text-slate-800 block">
                    {transferModalTask.grammarTopic}
                  </span>
                  <span className="text-[11px] text-slate-500">
                    {transferModalTask.type} · {transferModalTask.domain} · {transferModalTask.totalPoints} pts
                  </span>
                </div>
              </div>
            </div>

            <form onSubmit={handleExecuteTransfer} className="space-y-4">
              {/* Target Group */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Target Destination Cohort
                </label>
                {groups.filter((g) => g.id !== selectedGroup?.id).length === 0 ? (
                  <p className="text-xs text-amber-700 bg-amber-50 p-3 rounded-xl border border-amber-200">
                    No other cohort groups available. Create another group first to transfer tasks.
                  </p>
                ) : (
                  <select
                    value={transferTargetGroupId}
                    onChange={(e) => setTransferTargetGroupId(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-sm font-semibold text-slate-800 bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  >
                    {groups
                      .filter((g) => g.id !== selectedGroup?.id)
                      .map((g) => (
                        <option key={g.id} value={g.id}>
                          {g.name} ({g.studentCount} students){g.isLocked ? ' [Read-Only]' : ''}
                        </option>
                      ))}
                  </select>
                )}
              </div>

              {/* Mode: Move vs Copy */}
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                  Transfer Mode
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  <button
                    type="button"
                    onClick={() => setTransferKeepInSource(false)}
                    className={`p-3 rounded-2xl border text-left transition ${
                      !transferKeepInSource
                        ? 'border-primary bg-indigo-50/60 ring-2 ring-primary/20'
                        : 'border-slate-200 hover:border-slate-300 bg-white'
                    }`}
                  >
                    <div className="flex items-center space-x-2">
                      <span className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${
                        !transferKeepInSource ? 'border-primary bg-primary' : 'border-slate-300'
                      }`}>
                        {!transferKeepInSource && <span className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </span>
                      <span className="text-xs font-bold text-slate-800">Move</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-1 leading-normal pl-5">
                      Transfers task to new cohort. Removes unstarted assignments from current cohort while preserving completed grades.
                    </p>
                  </button>

                  <button
                    type="button"
                    onClick={() => setTransferKeepInSource(true)}
                    className={`p-3 rounded-2xl border text-left transition ${
                      transferKeepInSource
                        ? 'border-primary bg-indigo-50/60 ring-2 ring-primary/20'
                        : 'border-slate-200 hover:border-slate-300 bg-white'
                    }`}
                  >
                    <div className="flex items-center space-x-2">
                      <span className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${
                        transferKeepInSource ? 'border-primary bg-primary' : 'border-slate-300'
                      }`}>
                        {transferKeepInSource && <span className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </span>
                      <span className="text-xs font-bold text-slate-800">Copy</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-1 leading-normal pl-5">
                      Keeps assignment in current cohort and additionally assigns it to the target cohort.
                    </p>
                  </button>
                </div>
              </div>

              {/* Due Date & Max Attempts */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                <div>
                  <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">
                    Due Date
                  </label>
                  <input
                    type="datetime-local"
                    value={transferDueDate}
                    onChange={(e) => setTransferDueDate(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1">
                    Allowed Attempts
                  </label>
                  <select
                    value={transferMaxAttempts}
                    onChange={(e) => setTransferMaxAttempts(Number(e.target.value))}
                    className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  >
                    <option value={1}>1 Attempt</option>
                    <option value={0}>∞ Unlimited Attempts</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-end space-x-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setTransferModalTask(null)}
                  className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isTransferring || !transferTargetGroupId}
                  className="px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50 flex items-center space-x-1.5"
                >
                  <ArrowRightLeft className="w-4 h-4" />
                  <span>
                    {isTransferring
                      ? 'Processing...'
                      : transferKeepInSource
                      ? 'Copy to Target Cohort'
                      : 'Move to Target Cohort'}
                  </span>
                </button>
              </div>
            </form>
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
