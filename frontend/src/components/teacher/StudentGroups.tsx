/**
 * @file StudentGroups.tsx
 * @brief Educator student group management view allowing group creation, enrollment, and member soft-deletion.
 */

import React, { useState, useEffect } from 'react';
import { Plus, Trash2, UserPlus, Mail } from 'lucide-react';
import { groupApi } from '../../api/groupApi';
import { Group } from '../../types/group';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Cohort management panel for educators to organize groups, add students, and monitor enrollment.
 * @return JSX cohort management view.
 */
export const StudentGroups: React.FC = () => {
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const [newGroupName, setNewGroupName] = useState('');
  const [isCreatingGroup, setIsCreatingGroup] = useState(false);

  const [studentEmail, setStudentEmail] = useState('');
  const [isAddingStudent, setIsAddingStudent] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  /**
   * @brief Event handler or helper executing load groups.
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
   * @brief Event handler or helper executing load group details.
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
   * @brief Event handler or helper executing handle create group.
   */
  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGroupName.trim()) return;
    setIsCreatingGroup(true);
    try {
      const created = await groupApi.createGroup({ name: newGroupName.trim() });
      setNewGroupName('');
      await loadGroups();
      await loadGroupDetails(created.id);
    } catch (err) {
      console.error('Failed to create group:', err);
    } finally {
      setIsCreatingGroup(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle add student.
   */
  const handleAddStudent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedGroup || !studentEmail.trim()) return;
    setIsAddingStudent(true);
    setActionMessage(null);

    try {
      await groupApi.addStudent(selectedGroup.id, studentEmail.trim());
      setStudentEmail('');
      setActionMessage('Student enrolled (or restored from archive) successfully!');
      setTimeout(() => setActionMessage(null), 3000);
      await loadGroupDetails(selectedGroup.id);
      await loadGroups();
    } catch (err: any) {
      console.error('Failed to add student:', err);
      setActionMessage(err.response?.data?.message || 'Failed to add student.');
    } finally {
      setIsAddingStudent(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle remove student.
   */
  const handleRemoveStudent = async (studentId: string) => {
    if (!selectedGroup) return;
    if (!confirm('Remove student from group? Their historical records will remain safe and restored if re-added.')) {
      return;
    }

    try {
      await groupApi.removeStudent(selectedGroup.id, studentId);
      await loadGroupDetails(selectedGroup.id);
      await loadGroups();
    } catch (err) {
      console.error('Failed to remove student:', err);
    }
  };

  /**
   * @brief Event handler or helper executing handle delete group.
   */
  const handleDeleteGroup = async () => {
    if (!selectedGroup) return;
    if (!confirm(`Are you sure you want to delete "${selectedGroup.name}"?`)) return;

    try {
      await groupApi.deleteGroup(selectedGroup.id);
      setSelectedGroup(null);
      await loadGroups();
    } catch (err) {
      console.error('Failed to delete group:', err);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading your class groups..." />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Student Cohorts</h1>
        <p className="text-sm text-slate-500 mt-1">
          Organize your students into groups. Leaderboards, assignments, and reports are isolated per group.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
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

          <div className="bg-white rounded-3xl p-4 border border-slate-100 shadow-sm space-y-1">
            <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider px-3 py-2">
              Cohorts ({groups.length})
            </h2>

            {groups.length === 0 ? (
              <p className="text-xs text-slate-400 p-4 text-center">No groups created yet</p>
            ) : (
              groups.map((g) => {
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

                <button
                  onClick={handleDeleteGroup}
                  className="flex items-center space-x-1.5 text-xs font-semibold text-rose-600 hover:text-rose-700 p-2 rounded-xl hover:bg-rose-50 transition"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>Delete Group</span>
                </button>
              </div>

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

              <div className="space-y-3">
                <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                  Enrolled Students
                </h3>

                {(!selectedGroup.students || selectedGroup.students.length === 0) ? (
                  <p className="text-sm text-slate-500 py-6 text-center">
                    No active students in this cohort. Invite students via email above.
                  </p>
                ) : (
                  <div className="divide-y divide-slate-100">
                    {selectedGroup.students.map((student) => (
                      <div
                        key={student.id}
                        className="py-3 flex items-center justify-between hover:bg-slate-50/50 px-2 rounded-xl transition"
                      >
                        <div className="space-y-0.5">
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-bold text-slate-800">{student.fullName}</span>
                            {student.cefrLevel && <CefrBadge level={student.cefrLevel} size="sm" />}
                          </div>
                          <span className="text-xs text-slate-400">{student.email}</span>
                        </div>

                        <button
                          onClick={() => handleRemoveStudent(student.id)}
                          className="text-xs text-slate-400 hover:text-rose-600 transition p-1.5"
                          title="Remove student (soft-delete)"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
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
    </div>
  );
};
