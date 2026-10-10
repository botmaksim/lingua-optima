/**
 * @file components.test.tsx
 * @brief Unit tests for common React UI components (CefrBadge, Toast, LoginPage).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { CefrBadge } from '../components/common/CefrBadge';
import { Toast } from '../components/common/Toast';
import { ConfirmDialog } from '../components/common/ConfirmDialog';
import { LoginPage } from '../components/auth/LoginPage';
import { TranslatorDropdown } from '../components/common/TranslatorDropdown';
import { AIReview } from '../components/student/AIReview';
import { submissionApi } from '../api/submissionApi';
import { useAuthStore } from '../store/authStore';
import { Footer } from '../components/common/Footer';
import { PrivacyPolicy } from '../pages/PrivacyPolicy';
import { TermsOfService } from '../pages/TermsOfService';
import { HelpCenter } from '../pages/HelpCenter';

describe('CefrBadge component', () => {
  it('renders level text and badge correctly', () => {
    render(<CefrBadge level="B2" />);
    expect(screen.getByText('B2')).toBeInTheDocument();
  });

  it('renders C1 with correct label', () => {
    render(<CefrBadge level="C1" />);
    expect(screen.getByText('C1')).toBeInTheDocument();
  });
});

describe('Toast component', () => {
  it('renders title, message, and calls onClose when dismiss button clicked', () => {
    const handleClose = vi.fn();
    render(
      <Toast
        id="toast-1"
        type="success"
        title="Action Complete"
        message="Saved successfully"
        onClose={handleClose}
      />
    );

    expect(screen.getByText('Action Complete')).toBeInTheDocument();
    expect(screen.getByText('Saved successfully')).toBeInTheDocument();
    const closeBtn = screen.getByRole('button', { name: /dismiss toast/i });
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledWith('toast-1');
  });
});

describe('ConfirmDialog component', () => {
  it('renders destructive confirmation modal and handles confirm and Escape actions', () => {
    const handleConfirm = vi.fn();
    const handleCancel = vi.fn();

    render(
      <ConfirmDialog
        isOpen={true}
        title="Delete Cohort Group"
        message="Are you sure you want to delete this group?"
        confirmText="Delete Group"
        isDestructive={true}
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    );

    expect(screen.getByText('Destructive Action')).toBeInTheDocument();
    expect(screen.getByText('Delete Cohort Group')).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete this group?')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Delete Group'));
    expect(handleConfirm).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleCancel).toHaveBeenCalledTimes(1);
  });
});

describe('LoginPage component', () => {
  it('renders both email/password form and Google OAuth2 sign-in button', () => {
    useAuthStore.setState({ user: null, isAuthenticated: false, isLoading: false });
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText('name@example.com')).toBeInTheDocument();
    expect(screen.getByText('Sign In with Email')).toBeInTheDocument();
    const googleBtn = screen.getByTestId('google-oauth-button');
    expect(googleBtn).toBeInTheDocument();
    expect(screen.getByText('Continue with Google')).toBeInTheDocument();

    fireEvent.click(googleBtn);
    expect(screen.getByText(/VITE_GOOGLE_CLIENT_ID|Google Identity Services/i)).toBeInTheDocument();
  });
});

describe('TranslatorDropdown component', () => {
  it('renders dropdown toggle button with globe icon', () => {
    render(<TranslatorDropdown />);
    const button = screen.getByRole('button', { name: /translate page/i });
    expect(button).toBeInTheDocument();
    expect(screen.getByText('Translate')).toBeInTheDocument();
  });

  it('opens translation menu and displays Google and Yandex options', () => {
    render(<TranslatorDropdown />);
    const button = screen.getByRole('button', { name: /translate page/i });
    fireEvent.click(button);

    expect(screen.getByText('Translate Webpage')).toBeInTheDocument();
    expect(screen.getByText('Google Translate')).toBeInTheDocument();
    expect(screen.getByText('Yandex Translate')).toBeInTheDocument();
    expect(screen.getAllByText(/Russian \(RU\)/i)).toHaveLength(2);
  });
});

describe('AIReview component', () => {
  it('renders sentence-by-sentence questions with green/red status, correct answers, and AI gap analysis', async () => {
    vi.spyOn(submissionApi, 'getSubmissionById').mockResolvedValue({
      id: 'sub-123',
      studentId: 'user-1',
      submissionType: 'TEXT',
      originalText: 'Q1: woke\nQ2: did went',
      score: 80,
      effectiveScore: 80,
      feedback: 'Great job overall, review past simple irregular verbs.',
      submittedAt: new Date().toISOString(),
      grammarTopic: 'Past Simple',
      items: [
        {
          questionNumber: 1,
          sentence: 'He ___ (wake) up early yesterday.',
          studentAnswer: 'woke',
          correctAnswer: 'woke',
          isCorrect: true,
          explanation: 'Correct irregular past form of wake.',
          grammarRule: 'Past Simple Irregular Verbs',
        },
        {
          questionNumber: 2,
          sentence: 'She ___ (go) to the library on Monday.',
          studentAnswer: 'did went',
          correctAnswer: 'went',
          isCorrect: false,
          explanation: 'In affirmative sentences, do not use auxiliary did. Use went.',
          grammarRule: 'Past Simple Auxiliary Usage',
        },
      ],
      aiAnalysis: {
        summary: 'You answered 1 out of 2 questions correctly (50% accuracy).',
        weaknesses: ['Past Simple Auxiliary Usage'],
        strengths: ['Past Simple Irregular Verbs'],
        recommendations: 'Focus on affirmative vs interrogative sentence structures.',
        suggestedTopics: ['Past Simple Auxiliary Usage'],
      },
    } as any);

    render(
      <MemoryRouter initialEntries={['/student/review/sub-123']}>
        <Routes>
          <Route path="/student/review/:submissionId" element={<AIReview />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Sentence-by-Sentence Question Breakdown')).toBeInTheDocument();
    expect(screen.getByText('Sentence #1')).toBeInTheDocument();
    expect(screen.getByText('He ___ (wake) up early yesterday.')).toBeInTheDocument();
    expect(screen.getByText('Correct')).toBeInTheDocument();

    expect(screen.getByText('Sentence #2')).toBeInTheDocument();
    expect(screen.getByText('She ___ (go) to the library on Monday.')).toBeInTheDocument();
    expect(screen.getByText('Needs Revision')).toBeInTheDocument();
    expect(screen.getByText('did went')).toBeInTheDocument();
    expect(screen.getByText('went')).toBeInTheDocument();

    expect(screen.getByText(/In affirmative sentences, do not use auxiliary did/i)).toBeInTheDocument();
    expect(screen.getAllByText('Past Simple Auxiliary Usage').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Practice This Topic')).toBeInTheDocument();
  });
});

import { CustomSelect } from '../components/common/CustomSelect';

describe('CustomSelect component', () => {
  it('renders selected option, opens styled dropdown, and invokes onChange when option clicked', () => {
    const handleChange = vi.fn();
    render(
      <CustomSelect
        value="Cleft Sentences"
        onChange={handleChange}
        ariaLabel="Grammar Topic"
        groups={[
          {
            label: 'Core Syllabus (C1)',
            options: [
              { value: 'Advanced Inversion & Fronting', label: 'Advanced Inversion & Fronting', badge: 'C1' },
              { value: 'Cleft Sentences', label: 'Cleft Sentences', badge: 'C1' },
            ],
          },
        ]}
      />
    );

    const trigger = screen.getByRole('button', { name: /grammar topic/i });
    expect(trigger).toHaveTextContent('Cleft Sentences');

    fireEvent.click(trigger);
    expect(screen.getByText('Core Syllabus (C1)')).toBeInTheDocument();

    const nextOption = screen.getByRole('option', { name: /advanced inversion & fronting/i });
    fireEvent.click(nextOption);
    expect(handleChange).toHaveBeenCalledWith('Advanced Inversion & Fronting');
  });
});

describe('ConfigureTask component', () => {
  it('renders configuration form and handles interactive preview customizer', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { taskApi } = await import('../api/taskApi');
    const { apiKeyApi } = await import('../api/apiKeyApi');
    const { ConfigureTask } = await import('../components/teacher/ConfigureTask');

    vi.spyOn(apiKeyApi, 'getKeys').mockResolvedValue([]);
    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([{ id: 'g1', name: 'Group 1', studentCount: 5 }] as any);
    vi.spyOn(taskApi, 'getTopicsCatalog').mockResolvedValue({
      topicsByLevel: { B1: ['Past Simple'] },
      mixedTopicsByLevel: {},
      crossLevelTopics: [],
      domains: ['Everyday Life'],
    } as any);
    vi.spyOn(taskApi, 'previewTask').mockResolvedValue({
      id: 'task-preview-1',
      type: 'MCQ',
      cefrLevel: 'B1',
      grammarTopic: 'Past Simple',
      domain: 'Everyday Life',
      difficulty: 'MEDIUM',
      content: 'Choose the correct past verb form.',
      answerKey: '{"1":"went"}',
      createdAt: '2026-10-10T00:00:00Z',
      questions: [
        {
          id: 'q1',
          questionOrder: 1,
          questionText: 'She _____ to London yesterday.',
          correctAnswer: 'went',
          options: ['go', 'went', 'gone', 'going'],
          difficulty: 0.5,
          grammarRule: 'Irregular past: go -> went',
        },
      ],
    } as any);

    render(
      <MemoryRouter>
        <ConfigureTask />
      </MemoryRouter>
    );

    expect(screen.getByText('Configure & Deploy Task')).toBeInTheDocument();
    expect(screen.getByText('Live Preview & Customizer')).toBeInTheDocument();
    expect(screen.getByText(/Click "Preview" to inspect and customize/i)).toBeInTheDocument();

    const previewBtn = screen.getByRole('button', { name: /^preview$/i });
    fireEvent.click(previewBtn);

    const questionInput = await screen.findByDisplayValue('She _____ to London yesterday.');
    expect(questionInput).toBeInTheDocument();
    expect(screen.getByDisplayValue('Choose the correct past verb form.')).toBeInTheDocument();
    expect(screen.getByText('Correct')).toBeInTheDocument();

    // In-place edit question text
    fireEvent.change(questionInput, { target: { value: 'They _____ to Rome last week.' } });
    expect(screen.getByDisplayValue('They _____ to Rome last week.')).toBeInTheDocument();

    // Verify Total Assignment Points and Question Points inputs
    expect(screen.getByText('Total Assignment Points / Max Score')).toBeInTheDocument();
    expect(screen.getByText(/Questions Sum:/i)).toBeInTheDocument();
    expect(screen.getByText(/100 \/ 100 pts/i)).toBeInTheDocument();

    // Edit question points to 50
    const pointsInput = screen.getByRole('spinbutton', { name: 'Question 1 Points' });
    fireEvent.change(pointsInput, { target: { value: '50' } });

    // Verify Questions Sum indicates mismatch and Sync Total button appears
    expect(screen.getByText(/50 \/ 100 pts/i)).toBeInTheDocument();
    const syncTotalBtn = screen.getByRole('button', { name: /sync total \(50\)/i });
    expect(syncTotalBtn).toBeInTheDocument();

    // Click Sync Total to match total points with question points sum
    fireEvent.click(syncTotalBtn);
    expect(screen.getByText(/50 \/ 50 pts/i)).toBeInTheDocument();

    // Test Distribute Evenly button
    const distributeBtn = screen.getByRole('button', { name: /distribute evenly/i });
    expect(distributeBtn).toBeInTheDocument();
    fireEvent.click(distributeBtn);
    expect(screen.getByText(/50 \/ 50 pts/i)).toBeInTheDocument();
  });
});

describe('Student account renaming', () => {
  it('allows educator to update student name via userApi', async () => {
    const { userApi } = await import('../api/userApi');
    const spy = vi.spyOn(userApi, 'updateStudentName').mockResolvedValue({
      id: 's1',
      fullName: 'Alexandre Dumas',
      email: 'alex@lingua.com',
      role: 'STUDENT',
      cefrLevel: 'B2',
      streakCount: 3,
      createdAt: '2026-10-10T00:00:00Z',
    } as any);

    const res = await userApi.updateStudentName('s1', 'Alexandre Dumas');
    expect(res.fullName).toBe('Alexandre Dumas');
    expect(spy).toHaveBeenCalledWith('s1', 'Alexandre Dumas');
  });
});

describe('SubmissionsReview component', () => {
  it('renders submissions, filters by search, and toggles hide/unhide', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { SubmissionsReview } = await import('../components/teacher/SubmissionsReview');

    vi.spyOn(submissionApi, 'getTeacherSubmissions').mockResolvedValue([
      {
        id: 'sub-1',
        studentId: 'stud-1',
        studentName: 'Alice Smith',
        studentEmail: 'alice@example.com',
        submissionType: 'TEXT',
        originalText: 'She went to Paris.',
        score: 95,
        submittedAt: '2026-10-10T00:00:00Z',
        grammarTopic: 'Past Simple',
      },
      {
        id: 'sub-2',
        studentId: 'stud-2',
        studentName: 'Bob Jones',
        studentEmail: 'bob@example.com',
        submissionType: 'TEXT',
        originalText: 'They have arrived.',
        score: 80,
        submittedAt: '2026-10-10T01:00:00Z',
        grammarTopic: 'Present Perfect',
      },
    ] as any);

    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([
      {
        id: 'grp-1',
        name: 'Group Alpha',
        studentCount: 1,
        students: [
          {
            id: 'stud-1',
            fullName: 'Alice Smith',
            email: 'alice@example.com',
            role: 'STUDENT',
          },
        ],
        createdAt: '2026-10-01T00:00:00Z',
      },
    ] as any);

    render(
      <MemoryRouter>
        <SubmissionsReview />
      </MemoryRouter>
    );

    expect(await screen.findByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    expect(screen.getByText('Group Alpha')).toBeInTheDocument();

    // Search filter test
    const searchInput = screen.getByPlaceholderText(/search student, cohort, topic/i);
    fireEvent.change(searchInput, { target: { value: 'Past Simple' } });
    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    expect(screen.queryByText('Bob Jones')).not.toBeInTheDocument();

    // Clear search
    fireEvent.change(searchInput, { target: { value: '' } });
    expect(screen.getByText('Bob Jones')).toBeInTheDocument();

    // Hide submission test
    const hideButtons = screen.getAllByTitle(/hide from active queue/i);
    fireEvent.click(hideButtons[0]);

    // Hidden tab shows the hidden item (sub-2 Bob Jones is newer than sub-1 Alice Smith)
    const hiddenTabBtn = screen.getByRole('button', { name: /hidden 1/i });
    fireEvent.click(hiddenTabBtn);
    expect(screen.getByText('Bob Jones')).toBeInTheDocument();
    expect(screen.getByTitle(/restore to active queue/i)).toBeInTheDocument();
  });

  it('expands detailed question breakdown with options and edits AI feedback & teacher advice inline', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { SubmissionsReview } = await import('../components/teacher/SubmissionsReview');

    const mockSub = {
      id: 'sub-detail-1',
      studentId: 'stud-10',
      studentName: 'Clara Oswald',
      studentEmail: 'clara@tardis.com',
      submissionType: 'TEXT',
      originalText: 'Q1: went\nQ2: has gone',
      taskContent: 'Complete the sentences with correct past tense forms.',
      score: 75,
      effectiveScore: 75,
      feedback: 'Original AI diagnostic feedback comment.',
      teacherComment: 'Original educator advice.',
      submittedAt: '2026-10-10T02:00:00Z',
      grammarTopic: 'Past Tenses',
      items: [
        {
          questionNumber: 1,
          sentence: 'Yesterday she ___ to London.',
          studentAnswer: 'went',
          correctAnswer: 'went',
          isCorrect: true,
          options: ['go', 'went', 'gone'],
          explanation: 'Past simple for finished action in the past.',
          grammarRule: 'Past Simple',
        },
        {
          questionNumber: 2,
          sentence: 'She ___ never ___ there before.',
          studentAnswer: 'has gone',
          correctAnswer: 'had gone',
          isCorrect: false,
          options: ['had gone', 'has gone', 'went'],
          explanation: 'Past perfect indicates action before another past event.',
          grammarRule: 'Past Perfect',
        },
      ],
    };

    vi.spyOn(submissionApi, 'getTeacherSubmissions').mockResolvedValue([mockSub] as any);
    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([]);
    const overrideSpy = vi.spyOn(submissionApi, 'overrideScore').mockResolvedValue({
      ...mockSub,
      feedback: 'Updated AI diagnostic evaluation.',
      teacherComment: 'Updated teacher recommendation.',
    } as any);

    render(
      <MemoryRouter>
        <SubmissionsReview />
      </MemoryRouter>
    );

    expect(await screen.findByText('Clara Oswald')).toBeInTheDocument();

    // Click expand button
    const expandBtn = screen.getByTitle('Detailed breakdown');
    fireEvent.click(expandBtn);

    // Verify task content
    expect(screen.getByText('Complete the sentences with correct past tense forms.')).toBeInTheDocument();

    // Verify questions and options
    expect(screen.getByText('Yesterday she ___ to London.')).toBeInTheDocument();
    expect(screen.getByText('She ___ never ___ there before.')).toBeInTheDocument();
    expect(screen.getByText('Student Pick (✓)')).toBeInTheDocument();
    expect(screen.getByText('Student Pick (✗)')).toBeInTheDocument();
    expect(screen.getAllByText('Expected Answer:').length).toBe(2);

    // Test inline edit of AI comment
    const editAiBtn = screen.getByRole('button', { name: /edit ai comment/i });
    fireEvent.click(editAiBtn);
    const aiTextarea = screen.getByPlaceholderText(/edit ai evaluation feedback/i);
    fireEvent.change(aiTextarea, { target: { value: 'Updated AI diagnostic evaluation.' } });
    const saveAiBtn = screen.getByRole('button', { name: /^save$/i });
    fireEvent.click(saveAiBtn);

    await waitFor(() => {
      expect(screen.queryByPlaceholderText(/edit ai evaluation feedback/i)).not.toBeInTheDocument();
    });

    expect(overrideSpy).toHaveBeenCalledWith('sub-detail-1', {
      overrideScore: 75,
      teacherComment: 'Original educator advice.',
      feedback: 'Updated AI diagnostic evaluation.',
    });

    // Test inline edit of teacher advice
    const editAdviceBtn = screen.getByRole('button', { name: /edit advice/i });
    fireEvent.click(editAdviceBtn);
    const adviceTextarea = screen.getByPlaceholderText(/write personalized guidance/i);
    fireEvent.change(adviceTextarea, { target: { value: 'Updated teacher recommendation.' } });
    const saveAdviceBtn = screen.getByRole('button', { name: /^save$/i });
    fireEvent.click(saveAdviceBtn);

    await waitFor(() => {
      expect(screen.queryByPlaceholderText(/write personalized guidance/i)).not.toBeInTheDocument();
    });

    expect(overrideSpy).toHaveBeenCalledWith('sub-detail-1', {
      overrideScore: 75,
      teacherComment: 'Updated teacher recommendation.',
      feedback: 'Updated AI diagnostic evaluation.',
    });
  });
});

describe('StudentGroups component', () => {
  it('renders cohort students, filters by CEFR and hides/unhides student', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { StudentGroups } = await import('../components/teacher/StudentGroups');

    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([
      {
        id: 'grp-1',
        name: 'Evening B2',
        studentCount: 2,
        createdAt: '2026-10-01T00:00:00Z',
      },
    ] as any);

    vi.spyOn(groupApi, 'getGroupDetails').mockResolvedValue({
      id: 'grp-1',
      name: 'Evening B2',
      studentCount: 2,
      createdAt: '2026-10-01T00:00:00Z',
      students: [
        {
          id: 's-1',
          fullName: 'Maria Garcia',
          email: 'maria@example.com',
          role: 'STUDENT',
          cefrLevel: 'B2',
        },
        {
          id: 's-2',
          fullName: 'John Miller',
          email: 'john@example.com',
          role: 'STUDENT',
          cefrLevel: 'A2',
        },
      ],
      pendingStudents: [],
    } as any);

    render(
      <MemoryRouter>
        <StudentGroups />
      </MemoryRouter>
    );

    expect(await screen.findByText('Maria Garcia')).toBeInTheDocument();
    expect(screen.getByText('John Miller')).toBeInTheDocument();

    // Filter by search
    const searchInput = screen.getByPlaceholderText(/search student name or email/i);
    fireEvent.change(searchInput, { target: { value: 'Maria' } });
    expect(screen.getByText('Maria Garcia')).toBeInTheDocument();
    expect(screen.queryByText('John Miller')).not.toBeInTheDocument();

    // Clear search
    fireEvent.change(searchInput, { target: { value: '' } });
    expect(screen.getByText('John Miller')).toBeInTheDocument();

    // Hide student test
    const hideBtn = screen.getAllByTitle(/hide student from active list/i)[0];
    fireEvent.click(hideBtn);

    // Switch to Hidden tab
    const hiddenTabBtn = screen.getByRole('button', { name: /hidden 1/i });
    fireEvent.click(hiddenTabBtn);
    expect(screen.getByTitle(/unhide student/i)).toBeInTheDocument();
  });
});

describe('ExportReports component', () => {
  it('renders cohort homework breakdown and roster, and triggers report download', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { exportApi } = await import('../api/exportApi');
    const { ExportReports } = await import('../components/teacher/ExportReports');

    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([
      {
        id: 'grp-test-1',
        name: 'Advanced Grammar B2',
        studentCount: 2,
        createdAt: '2026-10-01T00:00:00Z',
      },
    ] as any);

    const mockReportData = {
      groupId: 'grp-test-1',
      groupName: 'Advanced Grammar B2',
      generatedAt: '2026-10-10T12:00:00Z',
      activeStudentCount: 2,
      assignedHomeworkCount: 1,
      totalSubmissionsCount: 1,
      groupAverageScore: 85.0,
      studentSummaries: [
        {
          studentId: 's-1',
          studentName: 'Alice Student',
          email: 'alice@example.com',
          cefrLevel: 'B2',
          totalTasks: 1,
          completedTasks: 1,
          averageScore: 85.0,
          submissionCount: 1,
          joinedDate: '2026-10-01',
        },
        {
          studentId: 's-2',
          studentName: 'Bob Student',
          email: 'bob@example.com',
          cefrLevel: 'B1',
          totalTasks: 1,
          completedTasks: 0,
          averageScore: null,
          submissionCount: 0,
          joinedDate: '2026-10-02',
        },
      ],
      homeworkReports: [
        {
          taskId: 'task-1',
          grammarTopic: 'Past Perfect vs Simple Past',
          taskType: 'MCQ',
          cefrLevel: 'B2',
          totalPoints: 100,
          dueDate: '2026-10-15T18:00:00Z',
          assignedStudentCount: 2,
          submittedCount: 1,
          studentResults: [
            {
              studentId: 's-1',
              studentName: 'Alice Student',
              email: 'alice@example.com',
              status: 'GRADED',
              score: 85.0,
              percentage: 85.0,
              totalPoints: 100,
              attemptsUsed: 1,
              maxAttempts: 1,
              submittedAt: '2026-10-08T10:00:00Z',
              teacherComment: 'Good command of tenses',
              aiFeedback: 'Accurate auxiliary verb usage',
            },
            {
              studentId: 's-2',
              studentName: 'Bob Student',
              email: 'bob@example.com',
              status: 'PENDING',
              score: null,
              percentage: null,
              totalPoints: 100,
              attemptsUsed: 0,
              maxAttempts: 1,
              submittedAt: null,
              teacherComment: '—',
              aiFeedback: '—',
            },
          ],
        },
      ],
    };

    vi.spyOn(exportApi, 'getGroupReportPreview').mockResolvedValue(mockReportData as any);
    const downloadSpy = vi.spyOn(exportApi, 'downloadGroupReport').mockResolvedValue(new Blob(['pdf content']));

    // Mock URL.createObjectURL, URL.revokeObjectURL, and HTMLAnchorElement.prototype.click
    const originalCreateObjectURL = window.URL.createObjectURL;
    const originalRevokeObjectURL = window.URL.revokeObjectURL;
    window.URL.createObjectURL = vi.fn(() => 'blob:mock-url');
    window.URL.revokeObjectURL = vi.fn();
    const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    render(
      <MemoryRouter>
        <ExportReports />
      </MemoryRouter>
    );

    expect(screen.getByText('Export Academic Reports')).toBeInTheDocument();

    // Verify preview content rendered
    expect(await screen.findByText('Past Perfect vs Simple Past')).toBeInTheDocument();
    expect(screen.getByText('Alice Student')).toBeInTheDocument();
    expect(screen.getByText('Good command of tenses')).toBeInTheDocument();
    expect(screen.getByText('Pending')).toBeInTheDocument();

    // Switch to Student Roster tab
    const rosterTabBtn = screen.getByRole('button', { name: /student roster overview/i });
    fireEvent.click(rosterTabBtn);
    expect(await screen.findByText('alice@example.com')).toBeInTheDocument();
    expect(screen.getByText('bob@example.com')).toBeInTheDocument();

    // Trigger download
    const downloadBtn = screen.getByRole('button', { name: /download report \(pdf\)/i });
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(downloadSpy).toHaveBeenCalledWith('grp-test-1', 'pdf');
    });

    clickSpy.mockRestore();
    window.URL.createObjectURL = originalCreateObjectURL;
    window.URL.revokeObjectURL = originalRevokeObjectURL;
  });
});

describe('Footer component', () => {
  it('renders branding and navigation links to privacy, terms, help, and pricing', () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>
    );

    expect(screen.getByText('Lingua Optima')).toBeInTheDocument();
    expect(screen.getByText('Presentation')).toHaveAttribute('href', '/presentation.html');
    expect(screen.getByRole('link', { name: /privacy policy/i })).toHaveAttribute('href', '/privacy');
    expect(screen.getByRole('link', { name: /terms of service/i })).toHaveAttribute('href', '/terms');
    expect(screen.getByRole('link', { name: /help center/i })).toHaveAttribute('href', '/help');
    expect(screen.getByRole('link', { name: /pricing/i })).toHaveAttribute('href', '/subscription');
  });
});

describe('PrivacyPolicy page', () => {
  it('renders Zero-Retention OCR guarantee and key privacy policy sections', () => {
    render(
      <MemoryRouter>
        <PrivacyPolicy />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { level: 1, name: /privacy policy/i })).toBeInTheDocument();
    expect(screen.getByText(/zero-retention ocr architecture guarantee/i)).toBeInTheDocument();
    expect(screen.getByText(/ephemeral volatile memory \(RAM\)/i)).toBeInTheDocument();
    expect(screen.getByText(/1\. Information We Collect/i)).toBeInTheDocument();
    expect(screen.getByText(/2\. Artificial Intelligence & Diagnostic Processing/i)).toBeInTheDocument();
    expect(screen.getByText(/3\. Data Storage & Cryptographic Security/i)).toBeInTheDocument();
    expect(screen.getByText(/4\. Your Data Rights \(GDPR & CCPA Compliance\)/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /back to home/i })).toHaveAttribute('href', '/');
  });
});

describe('TermsOfService page', () => {
  it('renders core terms summary cards and detailed terms sections', () => {
    render(
      <MemoryRouter>
        <TermsOfService />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { level: 1, name: /terms of service/i })).toBeInTheDocument();
    expect(screen.getByText('Academic Integrity')).toBeInTheDocument();
    expect(screen.getByText('Educator Authority')).toBeInTheDocument();
    expect(screen.getByText('Fair Usage Quotas')).toBeInTheDocument();
    expect(screen.getByText(/1\. Acceptance of Agreement/i)).toBeInTheDocument();
    expect(screen.getByText(/2\. Account Roles and Conduct/i)).toBeInTheDocument();
    expect(screen.getByText(/3\. AI Diagnostic Evaluations & Pedagogical Advice/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /back to home/i })).toHaveAttribute('href', '/');
  });
});

describe('HelpCenter page', () => {
  it('renders FAQ list, filters by search query and category, and toggles accordions', () => {
    render(
      <MemoryRouter>
        <HelpCenter />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { level: 1, name: /help center & knowledge base/i })).toBeInTheDocument();

    // Check initial questions
    expect(screen.getByText(/How does Zero-Retention OCR work/i)).toBeInTheDocument();
    expect(screen.getByText(/What is the difference between Free and Pro plans/i)).toBeInTheDocument();

    // Filter by search query
    const searchInput = screen.getByPlaceholderText(/search questions by keyword/i);
    fireEvent.change(searchInput, { target: { value: 'Eco Mode' } });
    expect(screen.getByText(/What is Eco Mode when creating educational tasks/i)).toBeInTheDocument();
    expect(screen.queryByText(/How does Zero-Retention OCR work/i)).not.toBeInTheDocument();

    // Clear search
    fireEvent.change(searchInput, { target: { value: '' } });

    // Filter by category: For Educators
    const teacherTab = screen.getByRole('button', { name: /for educators/i });
    fireEvent.click(teacherTab);
    expect(screen.getByText(/How do educators create cohort groups and invite students/i)).toBeInTheDocument();
    expect(screen.queryByText(/How is my CEFR language level evaluated/i)).not.toBeInTheDocument();

    // Toggle accordion
    const questionBtn = screen.getByRole('button', { name: /How do educators create cohort groups and invite students/i });
    // Expand the question
    fireEvent.click(questionBtn);
    expect(screen.getByText(/From the Teacher Dashboard, open "Student Cohorts"/i)).toBeInTheDocument();

    // Collapse the question
    fireEvent.click(questionBtn);
    expect(screen.queryByText(/From the Teacher Dashboard, open "Student Cohorts"/i)).not.toBeInTheDocument();
  });
});





