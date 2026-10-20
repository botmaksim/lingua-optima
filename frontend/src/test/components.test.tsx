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
import { ForgotPassword } from '../components/auth/ForgotPassword';
import { authApi } from '../api/authApi';
import { TranslatorDropdown } from '../components/common/TranslatorDropdown';
import { AIReview } from '../components/student/AIReview';
import { TaskView } from '../components/student/TaskView';
import { taskApi } from '../api/taskApi';
import { submissionApi } from '../api/submissionApi';
import { useAuthStore } from '../store/authStore';
import { groupApi } from '../api/groupApi';
import { Footer } from '../components/common/Footer';
import { PrivacyPolicy } from '../pages/PrivacyPolicy';
import { TermsOfService } from '../pages/TermsOfService';
import { HelpCenter } from '../pages/HelpCenter';
import { ReadingConfigPanel, ReadingConfig } from '../components/teacher/ReadingConfigPanel';
import { AnnotatedTextReview } from '../components/common/AnnotatedTextReview';
import { SentenceCorrection } from '../types/submission';
import { SpeakingConfigPanel, TARGET_SOUND_PRESETS } from '../components/teacher/SpeakingConfigPanel';
import { SpeakingPracticeView } from '../components/common/SpeakingPracticeView';
import { GrammarConfigPanel } from '../components/teacher/GrammarConfigPanel';
import { QuickModePanel } from '../components/common/QuickModePanel';
import { getIpaTranscription, alignSpokenWords, speakWord, speakText, stopSpeech } from '../utils/phonetics';

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

  it('opens translation menu and displays Google Translate options', () => {
    render(<TranslatorDropdown />);
    const button = screen.getByRole('button', { name: /translate page/i });
    fireEvent.click(button);

    expect(screen.getByText('Translate Webpage')).toBeInTheDocument();
    expect(screen.getByText('Google Translate')).toBeInTheDocument();
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

  it('renders skipped questions with badge and unattempted metrics, enforcing mutual exclusivity', async () => {
    vi.spyOn(submissionApi, 'getSubmissionById').mockResolvedValue({
      id: 'sub-456',
      studentId: 'user-1',
      submissionType: 'TEXT',
      originalText: 'Q1: demonstrates\nQ2: wrong_ans\nQ3: No answer',
      score: 30,
      effectiveScore: 30,
      feedback: 'Good attempt, review conditionals.',
      submittedAt: new Date().toISOString(),
      grammarTopic: 'Conditionals 0, 1, 2',
      items: [
        {
          questionNumber: 1,
          sentence: 'If water reaches 100 degrees, it boils.',
          studentAnswer: 'demonstrates',
          correctAnswer: 'demonstrates',
          isCorrect: true,
          isSkipped: false,
          explanation: 'Correct application of zero conditional.',
          grammarRule: 'Conditional 0 (General truths)',
        },
        {
          questionNumber: 2,
          sentence: 'If it rains, we will stay home.',
          studentAnswer: 'wrong_ans',
          correctAnswer: 'integrate',
          isCorrect: false,
          isSkipped: false,
          explanation: 'Incorrect form.',
          grammarRule: 'Conditional 1 (Real possibilities)',
        },
        {
          questionNumber: 3,
          sentence: 'If I had a million dollars, I would travel.',
          studentAnswer: 'No answer',
          correctAnswer: 'would travel',
          isCorrect: false,
          isSkipped: true,
          explanation: 'Unanswered question.',
          grammarRule: 'Conditional 2 (Hypothetical situations)',
        },
      ],
      aiAnalysis: {
        summary: 'You answered 1 out of 3 questions correctly (33% overall score). Attempt accuracy: 50% (1 of 2 answered, 1 skipped).',
        weaknesses: ['Conditional 1 (Real possibilities)'],
        strengths: ['Conditional 0 (General truths)'],
        recommendations: 'Focus on Conditional 1.',
        suggestedTopics: ['Conditional 1 (Real possibilities)'],
        attemptedCount: 2,
        correctCount: 1,
        totalQuestions: 3,
        skippedCount: 1,
        attemptAccuracy: 50,
      },
    } as any);

    render(
      <MemoryRouter initialEntries={['/student/review/sub-456']}>
        <Routes>
          <Route path="/student/review/:submissionId" element={<AIReview />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Submission Results')).toBeInTheDocument();
    expect(screen.getByText('50% Attempt Accuracy')).toBeInTheDocument();
    expect(screen.getByText('1/2 answered · 1 skipped')).toBeInTheDocument();
    expect(screen.getByText('Skipped · 0 pts')).toBeInTheDocument();
    expect(screen.getByText('No answer submitted')).toBeInTheDocument();

    // Check mutual exclusivity: Conditional 1 in weaknesses and recommendation, Conditional 0 in strengths
    expect(screen.getAllByText('Conditional 1 (Real possibilities)').length).toBe(2);
    expect(screen.getByText('Conditional 0 (General truths)')).toBeInTheDocument();
  });
});

describe('TaskView component', () => {
  it('renders questions, tracks answered count, and prompts confirmation when submitting with unanswered questions', async () => {
    vi.spyOn(taskApi, 'getTaskById').mockResolvedValue({
      id: 'task-999',
      grammarTopic: 'Conditionals',
      cefrLevel: 'B1',
      totalPoints: 20,
      questions: [
        {
          id: 'q-1',
          questionOrder: 1,
          questionText: 'If you heat ice, it ___ (melt).',
          correctAnswer: 'melts',
          options: ['melts', 'melted', 'will melt'],
          difficulty: 1,
          points: 10,
        },
        {
          id: 'q-2',
          questionOrder: 2,
          questionText: 'If it rains tomorrow, I ___ (take) an umbrella.',
          correctAnswer: 'will take',
          options: ['will take', 'take', 'took'],
          difficulty: 2,
          points: 10,
        },
      ],
    } as any);

    const submitSpy = vi.spyOn(submissionApi, 'submitText').mockResolvedValue({
      id: 'sub-new-123',
    } as any);

    render(
      <MemoryRouter initialEntries={['/student/task/task-999']}>
        <Routes>
          <Route path="/student/task/:taskId" element={<TaskView />} />
          <Route path="/student/review/:submissionId" element={<div>Review Page for sub-new-123</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText(/If you heat ice, it ___ \(melt\)\./i)).toBeInTheDocument();
    expect(screen.getByText('0 of 2 answered')).toBeInTheDocument();

    // Click answer for Q1
    fireEvent.click(screen.getByText('melts'));
    expect(screen.getByText('1 of 2 answered')).toBeInTheDocument();

    // Click Submit Answers while Q2 is unanswered
    fireEvent.click(screen.getByText('Submit Answers'));

    // Confirmation dialog should appear
    expect(await screen.findByText('Unanswered Questions Detected')).toBeInTheDocument();
    expect(screen.getByText(/You have 1 unanswered questions out of 2/i)).toBeInTheDocument();

    // Click "Keep Answering" to dismiss
    fireEvent.click(screen.getByText('Keep Answering'));
    await waitFor(() => {
      expect(screen.queryByText('Unanswered Questions Detected')).not.toBeInTheDocument();
    });
    expect(submitSpy).not.toHaveBeenCalled();

    // Click Submit Answers again and confirm
    fireEvent.click(screen.getByText('Submit Answers'));
    expect(await screen.findByText('Unanswered Questions Detected')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Submit Anyway'));

    await waitFor(() => {
      expect(submitSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          taskId: 'task-999',
          text: expect.stringContaining('Q1: melts\nQ2: No answer'),
        })
      );
    });
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

    const previewBtn = screen.getByRole('button', { name: /preview/i });
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

  it('supports multi-cohort selection and displays cohort total count banner', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { taskApi } = await import('../api/taskApi');
    const { apiKeyApi } = await import('../api/apiKeyApi');
    const { ConfigureTask } = await import('../components/teacher/ConfigureTask');

    vi.spyOn(apiKeyApi, 'getKeys').mockResolvedValue([]);
    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([
      { id: 'g1', name: 'Cohort Alpha', studentCount: 10 },
      { id: 'g2', name: 'Cohort Beta', studentCount: 15 },
    ] as any);
    vi.spyOn(taskApi, 'getTopicsCatalog').mockResolvedValue({
      topicsByLevel: { B1: ['Past Simple'] },
      mixedTopicsByLevel: {},
      crossLevelTopics: [],
      domains: ['Everyday Life'],
    } as any);

    render(
      <MemoryRouter>
        <ConfigureTask />
      </MemoryRouter>
    );

    expect(await screen.findByText(/Cohort Alpha/i)).toBeInTheDocument();
    expect(screen.getByText(/Cohort Beta/i)).toBeInTheDocument();

    // Select second group
    const betaBtn = screen.getByText(/Cohort Beta/i).closest('button')!;
    fireEvent.click(betaBtn);

    // Multi-cohort deployment banner
    expect(await screen.findByText(/Deploying simultaneously to/i)).toBeInTheDocument();
    expect(screen.getByText(/25 students total/i)).toBeInTheDocument();
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
    expect(screen.getByText('Student Pick: Correct')).toBeInTheDocument();
    expect(screen.getByText('Student Pick: Incorrect')).toBeInTheDocument();
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

  it('switches to Assigned Tasks tab and opens transfer task modal', async () => {
    const { groupApi } = await import('../api/groupApi');
    const { StudentGroups } = await import('../components/teacher/StudentGroups');

    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([
      { id: 'grp-1', name: 'Cohort One', studentCount: 5, createdAt: '2026-10-01T00:00:00Z' },
      { id: 'grp-2', name: 'Cohort Two', studentCount: 8, createdAt: '2026-10-01T00:00:00Z' },
    ] as any);

    vi.spyOn(groupApi, 'getGroupDetails').mockResolvedValue({
      id: 'grp-1',
      name: 'Cohort One',
      studentCount: 5,
      createdAt: '2026-10-01T00:00:00Z',
      students: [],
      pendingStudents: [],
    } as any);

    vi.spyOn(groupApi, 'getGroupTasks').mockResolvedValue([
      {
        taskId: 't-101',
        type: 'MCQ',
        cefrLevel: 'B2',
        grammarTopic: 'Reported Speech',
        domain: 'Workplace',
        totalPoints: 100,
        assignedStudentCount: 5,
        completedStudentCount: 3,
        avgScore: 82,
        createdAt: '2026-10-10T00:00:00Z',
      },
    ] as any);

    const transferSpy = vi.spyOn(groupApi, 'transferTask').mockResolvedValue();

    render(
      <MemoryRouter>
        <StudentGroups />
      </MemoryRouter>
    );

    expect(await screen.findByText('Cohort One')).toBeInTheDocument();

    // Click Assigned Tasks tab
    const tasksTabBtn = screen.getByRole('button', { name: /assigned tasks/i });
    fireEvent.click(tasksTabBtn);

    expect(await screen.findByText('Reported Speech')).toBeInTheDocument();
    expect(screen.getByText(/3 \/ 5/i)).toBeInTheDocument();

    // Click Transfer / Copy button
    const transferBtn = screen.getByRole('button', { name: /transfer \/ copy/i });
    fireEvent.click(transferBtn);

    expect(await screen.findByText('Transfer Assignment')).toBeInTheDocument();
    expect(screen.getByText('Target Destination Cohort')).toBeInTheDocument();

    // Submit transfer (Move mode default)
    const submitTransferBtn = screen.getByRole('button', { name: /move to target cohort/i });
    fireEvent.click(submitTransferBtn);

    await waitFor(() => {
      expect(transferSpy).toHaveBeenCalledWith('grp-1', 't-101', expect.objectContaining({
        targetGroupId: 'grp-2',
        keepInSource: false,
      }));
    });
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
    const rosterTabBtn = screen.getByRole('button', { name: /student roster/i });
    fireEvent.click(rosterTabBtn);
    expect(await screen.findByText('alice@example.com')).toBeInTheDocument();
    expect(screen.getByText('bob@example.com')).toBeInTheDocument();

    // Trigger download
    const downloadBtn = screen.getByRole('button', { name: /download report/i });
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

describe('ForgotPassword component', () => {
  it('requests recovery code and advances to verification step', async () => {
    const forgotSpy = vi.spyOn(authApi, 'forgotPassword').mockResolvedValue(undefined);

    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    expect(screen.getByRole('heading', { level: 2, name: /forgot password\?/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('name@example.com')).toBeInTheDocument();

    const emailInput = screen.getByPlaceholderText('name@example.com');
    fireEvent.change(emailInput, { target: { value: 'user@lingua.com' } });

    const submitBtn = screen.getByRole('button', { name: /send recovery code/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(forgotSpy).toHaveBeenCalledWith('user@lingua.com');
      expect(screen.getByRole('heading', { level: 2, name: /set new password/i })).toBeInTheDocument();
    });

    expect(screen.getByPlaceholderText('123456')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('At least 6 characters')).toBeInTheDocument();
  });

  it('validates password mismatch and successfully completes reset', async () => {
    vi.spyOn(authApi, 'forgotPassword').mockResolvedValue(undefined);
    const resetSpy = vi.spyOn(authApi, 'resetPassword').mockResolvedValue({
      message: 'Password reset successfully',
      email: 'user@lingua.com',
    });

    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    // Step 1: submit email
    fireEvent.change(screen.getByPlaceholderText('name@example.com'), { target: { value: 'user@lingua.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send recovery code/i }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 2, name: /set new password/i })).toBeInTheDocument();
    });

    const codeInput = screen.getByPlaceholderText('123456');
    const newPassInput = screen.getByPlaceholderText('At least 6 characters');
    const confirmPassInput = screen.getByPlaceholderText('Repeat new password');
    const resetBtn = screen.getByRole('button', { name: /reset password & continue/i });

    // Enter code and mismatched password
    fireEvent.change(codeInput, { target: { value: '123456' } });
    fireEvent.change(newPassInput, { target: { value: 'secretPass1' } });
    fireEvent.change(confirmPassInput, { target: { value: 'differentPass' } });
    fireEvent.click(resetBtn);

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    expect(resetSpy).not.toHaveBeenCalled();

    // Fix password match and submit
    fireEvent.change(confirmPassInput, { target: { value: 'secretPass1' } });
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(resetSpy).toHaveBeenCalledWith({
        email: 'user@lingua.com',
        code: '123456',
        newPassword: 'secretPass1',
      });
      expect(screen.getByText(/password reset successful!/i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /proceed to sign in/i })).toBeInTheDocument();
    });
  });
});

describe('ReadingConfigPanel component', () => {
  it('renders reading configuration fields and handles changes', () => {
    const config: ReadingConfig = {
      readingTitle: 'Renewable Power Evolution',
      readingLength: 'Medium (250-400 words)',
      readingParagraphs: '3 paragraphs on solar and wind',
      mcqQuestionsCount: 3,
      mcqPoints: 10,
      trueFalseQuestionsCount: 2,
      trueFalsePoints: 10,
      openQuestionsCount: 2,
      openPoints: 15,
    };
    const handleChange = vi.fn();
    const handleVocabChange = vi.fn();

    render(
      <ReadingConfigPanel
        config={config}
        onChange={handleChange}
        cefrLevel="B2"
        customVocabulary="grid, solar, turbine"
        onCustomVocabularyChange={handleVocabChange}
      />
    );

    expect(screen.getByText(/Reading Practice/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue('Renewable Power Evolution')).toBeInTheDocument();
    expect(screen.getByDisplayValue('grid, solar, turbine')).toBeInTheDocument();

    // Check total points badge: 3*10 + 2*10 + 2*15 = 80 pts
    expect(screen.getByText(/80 pts/i)).toBeInTheDocument();

    // Change title
    const titleInput = screen.getByPlaceholderText(/The Green Transition/i);
    fireEvent.change(titleInput, { target: { value: 'New Reading Title' } });
    expect(handleChange).toHaveBeenCalledWith({ readingTitle: 'New Reading Title' });

    // Change length preset
    const shortBtn = screen.getByRole('button', { name: /Short/i });
    fireEvent.click(shortBtn);
    expect(handleChange).toHaveBeenCalledWith({ readingLength: 'Short (150-250 words)' });

    // Change vocabulary
    const vocabInput = screen.getByPlaceholderText(/breakthrough, sustainable/i);
    fireEvent.change(vocabInput, { target: { value: 'clean, green' } });
    expect(handleVocabChange).toHaveBeenCalledWith('clean, green');
  });
});

describe('AnnotatedTextReview component', () => {
  const sampleText = 'He go to school yesterday and have three cat.';
  const corrections: SentenceCorrection[] = [
    {
      original: 'He go',
      corrected: 'He went',
      explanation: 'Past tense required for past time reference (yesterday).',
      grammarRule: 'Past Simple',
    },
    {
      original: 'have three cat',
      corrected: 'had three cats',
      explanation: 'Past tense of have is had; plural noun cats needed after three.',
      grammarRule: 'Plural Nouns & Past Tense',
    },
  ];

  it('renders text with inline red mistake highlights and displays explanation on click', () => {
    render(<AnnotatedTextReview text={sampleText} corrections={corrections} editable={false} />);

    // Text segments rendered
    expect(screen.getByText('He go')).toBeInTheDocument();
    expect(screen.getByText('have three cat')).toBeInTheDocument();

    // Click on the first highlighted mistake
    fireEvent.click(screen.getByText('He go'));

    // Inspector card appears
    expect(screen.getByText(/Diagnostic Analysis #1/i)).toBeInTheDocument();
    expect(screen.getByText('Past tense required for past time reference (yesterday).')).toBeInTheDocument();
    expect(screen.getByText('He went')).toBeInTheDocument();
  });

  it('allows teacher to edit explanation and delete a highlight in editable mode', () => {
    const handleCorrectionsChange = vi.fn();

    render(
      <AnnotatedTextReview
        text={sampleText}
        corrections={corrections}
        editable={true}
        onCorrectionsChange={handleCorrectionsChange}
      />
    );

    // Educator diagnostic mode banner is visible
    expect(screen.getByText(/Educator Diagnostic Mode/i)).toBeInTheDocument();

    // Click first error to inspect
    fireEvent.click(screen.getByText('He go'));

    // Click edit button
    const editBtn = screen.getByTitle(/Edit explanation and correction/i);
    fireEvent.click(editBtn);

    // Update explanation
    const explInput = screen.getByDisplayValue('Past tense required for past time reference (yesterday).');
    fireEvent.change(explInput, { target: { value: 'Teacher updated explanation.' } });

    // Save changes
    const saveBtn = screen.getByRole('button', { name: /Save Changes/i });
    fireEvent.click(saveBtn);

    expect(handleCorrectionsChange).toHaveBeenCalled();
    const updatedList = handleCorrectionsChange.mock.calls[0][0];
    expect(updatedList[0].explanation).toBe('Teacher updated explanation.');

    // Click delete highlight button
    const deleteBtn = screen.getByTitle(/Remove error highlight/i);
    fireEvent.click(deleteBtn);
    expect(handleCorrectionsChange).toHaveBeenCalledWith([corrections[1]]);
  });

  it('allows teacher to manually open new mistake highlight modal and add correction', () => {
    const handleCorrectionsChange = vi.fn();

    render(
      <AnnotatedTextReview
        text={sampleText}
        corrections={corrections}
        editable={true}
        onCorrectionsChange={handleCorrectionsChange}
      />
    );

    const highlightBtn = screen.getByRole('button', { name: /Highlight New Mistake/i });
    fireEvent.click(highlightBtn);

    expect(screen.getByText(/Highlight Grammatical Mistake/i)).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/Text snippet containing error/i), {
      target: { value: 'three cat' },
    });
    fireEvent.change(screen.getByPlaceholderText(/Correct grammatically polished phrasing/i), {
      target: { value: 'three cats' },
    });
    fireEvent.change(screen.getByPlaceholderText(/Explain why this phrasing is incorrect/i), {
      target: { value: 'Plural -s needed.' },
    });

    const applyBtn = screen.getByRole('button', { name: /Apply Highlight/i });
    fireEvent.click(applyBtn);

    expect(handleCorrectionsChange).toHaveBeenCalled();
    const resultList = handleCorrectionsChange.mock.calls[0][0];
    expect(resultList.length).toBe(3);
    expect(resultList[2].original).toBe('three cat');
  });
});

describe('Phonetics Utilities (phonetics.ts)', () => {
  beforeEach(() => {
    // Setup mock speechSynthesis
    window.speechSynthesis = {
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn().mockReturnValue([
        { name: 'Google US English', lang: 'en-US', default: true },
        { name: 'Google UK English Female', lang: 'en-GB', default: false },
      ]),
      onvoiceschanged: null,
      paused: false,
      pending: false,
      speaking: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    } as any;

    (window as any).SpeechSynthesisUtterance = vi.fn().mockImplementation((text) => ({
      text,
      voice: null,
      rate: 1,
      pitch: 1,
      volume: 1,
      lang: 'en-US',
    }));
  });

  it('getIpaTranscription returns correct dictionary IPA and handles heuristics', () => {
    expect(getIpaTranscription('thoughtful')).toBe('/ˈθɔːtfəl/');
    expect(getIpaTranscription('three')).toBe('/θriː/');
    expect(getIpaTranscription('rhythm')).toBe('/ˈrɪðəm/');
    expect(getIpaTranscription('The')).toBe('/ðiː, ðə/');

    // Fallback suffix heuristics
    const walkingIpa = getIpaTranscription('walking');
    expect(walkingIpa).toContain('ɪŋ');

    const quicklyIpa = getIpaTranscription('quickly');
    expect(quicklyIpa).toContain('li');
  });

  it('alignSpokenWords correctly scores 100% on perfect matches', () => {
    const reference = 'The three brothers walk thoughtfully through the woods';
    const spoken = 'The three brothers walk thoughtfully through the woods';

    const result = alignSpokenWords(reference, spoken, 100);
    expect(result.accuracyScore).toBe(100);
    expect(result.overallScore).toBe(100);
    expect(result.words.every((w) => w.status === 'CORRECT')).toBe(true);
    expect(result.generalFeedback[0]).toContain('Excellent clarity');
  });

  it('alignSpokenWords flags mispronounced words and offers articulation advice', () => {
    const reference = 'The weather is very warm today in the morning';
    const spoken = 'The bad is dark cold yesterday in the morning';

    const result = alignSpokenWords(reference, spoken, 100);
    expect(result.accuracyScore).toBeLessThan(70);
    expect(result.words.some((w) => w.status === 'MISPRONOUNCED')).toBe(true);

    const mispronounced = result.words.filter((w) => w.status === 'MISPRONOUNCED');
    expect(mispronounced.length).toBeGreaterThan(0);
    expect(mispronounced[0].tip).toBeDefined();
  });

  it('alignSpokenWords handles omitted words and completely empty spoken text', () => {
    const reference = 'Practice makes perfect every single day';
    const spoken = 'Practice makes day';

    const result = alignSpokenWords(reference, spoken, 100);
    expect(result.words.some((w) => w.status === 'OMITTED')).toBe(true);

    const emptyResult = alignSpokenWords(reference, '', 100);
    expect(emptyResult.accuracyScore).toBe(0);
    expect(emptyResult.words.every((w) => w.status === 'OMITTED')).toBe(true);
  });

  it('alignSpokenWords calculates speech rate (WPM) and calibrated pacing score correctly', () => {
    const reference = 'The three brothers walk thoughtfully through the woods';
    const spoken = 'The three brothers walk thoughtfully through the woods';

    // 8 words in 4 seconds = 120 WPM (optimal 100-160 band)
    const normalResult = alignSpokenWords(reference, spoken, 100, 4);
    expect(normalResult.wpm).toBe(120);
    expect(normalResult.pacingScore).toBe(100);
    expect(normalResult.durationSeconds).toBe(4);
    expect(normalResult.pacingFeedback).toContain('Optimal speech rate');
    expect(normalResult.overallScore).toBe(100);

    // 8 words in 30 seconds = 16 WPM (excessively slow)
    const slowResult = alignSpokenWords(reference, spoken, 100, 30);
    expect(slowResult.wpm).toBe(16);
    expect(slowResult.pacingScore).toBeLessThan(50);
    expect(slowResult.pacingFeedback).toContain('delayed delivery');
    // Accuracy is 100%, but overall score is degraded due to sluggish pace
    expect(slowResult.overallScore).toBeLessThan(100);
    expect(slowResult.overallScore).toBeGreaterThan(60);

    // 8 words in 1.5 seconds = 320 WPM (rushed)
    const fastResult = alignSpokenWords(reference, spoken, 100, 1.5);
    expect(fastResult.wpm).toBeGreaterThan(190);
    expect(fastResult.pacingScore).toBeLessThan(80);
    expect(fastResult.pacingFeedback).toContain('Rushed delivery');
  });

  it('speakWord, speakText, and stopSpeech invoke speechSynthesis methods safely', () => {
    speakWord('thoughtful', 'en-US');
    expect(window.speechSynthesis.speak).toHaveBeenCalled();

    speakText('She sells sea shells by the sea shore.', 'en-GB');
    expect(window.speechSynthesis.speak).toHaveBeenCalled();

    stopSpeech();
    expect(window.speechSynthesis.cancel).toHaveBeenCalled();
  });
});

describe('SpeakingConfigPanel component', () => {
  it('renders in educator mode and handles preset selection, title, points and length changes', () => {
    const handleTitleChange = vi.fn();
    const handleSoundsChange = vi.fn();
    const handleLengthChange = vi.fn();
    const handlePointsChange = vi.fn();

    render(
      <SpeakingConfigPanel
        speakingTitle="Dental Fricatives Mastery"
        onTitleChange={handleTitleChange}
        targetSounds={TARGET_SOUND_PRESETS[0].value}
        onTargetSoundsChange={handleSoundsChange}
        speakingLength="MEDIUM"
        onSpeakingLengthChange={handleLengthChange}
        totalPoints={100}
        onTotalPointsChange={handlePointsChange}
        isStudent={false}
      />
    );

    // Educator header is visible
    expect(screen.getByText('Speaking Practice')).toBeInTheDocument();
    expect(screen.getByText('Total Points for Pronunciation')).toBeInTheDocument();

    // Change title
    const titleInput = screen.getByPlaceholderText(/e\.g\. Master the Dental Fricatives/i);
    fireEvent.change(titleInput, { target: { value: 'New Speaking Exercise' } });
    expect(handleTitleChange).toHaveBeenCalledWith('New Speaking Exercise');

    // Click another target sound preset (Liquids)
    const liquidPresetBtn = screen.getByText('Liquids: /r/ vs /l/');
    fireEvent.click(liquidPresetBtn);
    expect(handleSoundsChange).toHaveBeenCalledWith(TARGET_SOUND_PRESETS[1].value);

    // Change length preset
    const shortLengthBtn = screen.getByText('Short Drill');
    fireEvent.click(shortLengthBtn);
    expect(handleLengthChange).toHaveBeenCalledWith('SHORT');

    // Change points
    const pointsInput = screen.getByDisplayValue('100');
    fireEvent.change(pointsInput, { target: { value: '80' } });
    expect(handlePointsChange).toHaveBeenCalledWith(80);

    // Click TTS audio tester button
    const testAudioBtn = screen.getByTitle(/Click to test pronunciation audio playback/i);
    fireEvent.click(testAudioBtn);
    expect(window.speechSynthesis.speak).toHaveBeenCalled();
  });

  it('renders in student mode without points allocation panel', () => {
    render(
      <SpeakingConfigPanel
        speakingTitle=""
        onTitleChange={vi.fn()}
        targetSounds={TARGET_SOUND_PRESETS[0].value}
        onTargetSoundsChange={vi.fn()}
        speakingLength="SHORT"
        onSpeakingLengthChange={vi.fn()}
        totalPoints={100}
        onTotalPointsChange={vi.fn()}
        isStudent={true}
      />
    );

    expect(screen.getByText('Speaking Practice')).toBeInTheDocument();
    expect(screen.queryByText('Assignment Points (Max Score)')).not.toBeInTheDocument();
  });
});

describe('SpeakingPracticeView component', () => {
  const samplePassage = 'Three thoughtful brothers thought through the theory.';

  beforeEach(() => {
    window.speechSynthesis = {
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn().mockReturnValue([]),
      onvoiceschanged: null,
      paused: false,
      pending: false,
      speaking: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    } as any;

    class MockSpeechRecognition {
      continuous = false;
      interimResults = false;
      lang = 'en-US';
      start = vi.fn().mockImplementation(() => {
        if (this.onstart) this.onstart();
      });
      stop = vi.fn().mockImplementation(() => {
        if (this.onend) this.onend();
      });
      abort = vi.fn();
      onstart: (() => void) | null = null;
      onresult: ((ev: any) => void) | null = null;
      onerror: ((ev: any) => void) | null = null;
      onend: (() => void) | null = null;
    }
    (window as any).SpeechRecognition = MockSpeechRecognition;
    (window as any).webkitSpeechRecognition = MockSpeechRecognition;
  });

  it('renders passage words, allows accent switching and triggers pronunciation on click', () => {
    render(
      <SpeakingPracticeView
        passageText={samplePassage}
        taskTitle="Dental Fricatives Practice"
        targetSounds="/θ/ and /ð/"
        points={100}
        onSubmit={vi.fn()}
      />
    );

    expect(screen.getByText('Dental Fricatives Practice')).toBeInTheDocument();
    expect(screen.getByText(/Phonetic Focus:/i)).toBeInTheDocument();
    expect(screen.getByText('Three')).toBeInTheDocument();

    // Click word to pronounce
    const wordBtn = screen.getByText('thoughtful');
    fireEvent.click(wordBtn);
    expect(window.speechSynthesis.speak).toHaveBeenCalled();

    // Switch accent
    const ukBtn = screen.getByText('UK Accent');
    fireEvent.click(ukBtn);
    expect(ukBtn).toHaveClass('bg-indigo-600');
  });

  it('displays pronunciation diagnostic metrics and submits results', () => {
    const handleSubmit = vi.fn();
    const mockResult = alignSpokenWords(samplePassage, 'Three thoughtful brothers thought through the theory', 100);

    render(
      <SpeakingPracticeView
        passageText={samplePassage}
        taskTitle="Practice Drill"
        points={100}
        onSubmit={handleSubmit}
        initialResult={mockResult}
      />
    );

    // Accuracy badge is visible
    expect(screen.getByText(/Pronunciation Accuracy/i)).toBeInTheDocument();
    expect(screen.getAllByText(/100%/i).length).toBeGreaterThan(0);

    // Click submit evaluation
    const submitBtn = screen.getByRole('button', { name: /Submit Speaking Assessment/i });
    fireEvent.click(submitBtn);

    expect(handleSubmit).toHaveBeenCalledTimes(1);
    const submittedPayload = handleSubmit.mock.calls[0][0];
    expect(submittedPayload.accuracyScore).toBe(100);
    expect(submittedPayload.overallScore).toBe(100);
    expect(submittedPayload.wpm).toBe(140);
  });
});

describe('TaskView Reading Window Scaling', () => {
  it('renders reading passage toolbar and resizes column proportions, font size, and height toggle', async () => {
    vi.spyOn(taskApi, 'getTaskById').mockResolvedValue({
      id: 'reading-123',
      type: 'READING',
      grammarTopic: 'The Future of Renewable Energy',
      cefrLevel: 'B2',
      domain: 'Science & Technology',
      content: 'Renewable energy systems have experienced exponential technological innovation in recent decades.',
      totalPoints: 30,
      questions: [
        {
          id: 'rq-1',
          questionOrder: 1,
          questionText: 'What has experienced exponential innovation?',
          correctAnswer: 'Renewable energy systems',
          options: ['Renewable energy systems', 'Fossil fuel extraction'],
          difficulty: 2,
          points: 15,
        },
      ],
    } as any);

    render(
      <MemoryRouter initialEntries={['/student/task/reading-123']}>
        <Routes>
          <Route path="/student/task/:taskId" element={<TaskView />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('The Future of Renewable Energy')).toBeInTheDocument();
    expect(screen.getByText('Reading Passage')).toBeInTheDocument();

    // Check width proportion buttons
    const compactBtn = screen.getByRole('button', { name: /35%/i });
    const wideBtn = screen.getByRole('button', { name: /58%/i });
    const expandedBtn = screen.getByRole('button', { name: /67%/i });
    expect(compactBtn).toBeInTheDocument();
    expect(wideBtn).toBeInTheDocument();
    expect(expandedBtn).toBeInTheDocument();

    // Click 58% preset
    fireEvent.click(wideBtn);
    expect(wideBtn).toHaveClass('text-primary');

    // Font size controls
    const increaseFontBtn = screen.getByLabelText(/Increase font size/i);
    const decreaseFontBtn = screen.getByLabelText(/Decrease font size/i);
    fireEvent.click(increaseFontBtn);
    expect(screen.getByText('lg')).toBeInTheDocument();
    fireEvent.click(decreaseFontBtn);
    expect(screen.getByText('base')).toBeInTheDocument();

    // Height toggle button
    const heightToggleBtn = screen.getByLabelText(/Toggle full height reading window/i);
    fireEvent.click(heightToggleBtn);
    expect(heightToggleBtn).toHaveClass('bg-indigo-50');
  });
});

describe('SpeakingConfigPanel Custom Settings', () => {
  it('reveals target word count and scenario instructions when Custom length preset is selected', () => {
    const handleWordCountChange = vi.fn();
    const handleInstructionsChange = vi.fn();
    const handleLengthChange = vi.fn();

    const { rerender } = render(
      <SpeakingConfigPanel
        speakingTitle="My Speech"
        onTitleChange={vi.fn()}
        targetSounds="/θ/ and /ð/"
        onTargetSoundsChange={vi.fn()}
        speakingLength="MEDIUM"
        onSpeakingLengthChange={handleLengthChange}
        totalPoints={100}
        onTotalPointsChange={vi.fn()}
        isStudent={true}
        customWordCount={80}
        onCustomWordCountChange={handleWordCountChange}
        customInstructions="Roleplay an airport check-in dialogue"
        onCustomInstructionsChange={handleInstructionsChange}
      />
    );

    expect(screen.queryByText(/Target Word Count/i)).not.toBeInTheDocument();

    // Switch to CUSTOM
    rerender(
      <SpeakingConfigPanel
        speakingTitle="My Speech"
        onTitleChange={vi.fn()}
        targetSounds="/θ/ and /ð/"
        onTargetSoundsChange={vi.fn()}
        speakingLength="CUSTOM"
        onSpeakingLengthChange={handleLengthChange}
        totalPoints={100}
        onTotalPointsChange={vi.fn()}
        isStudent={true}
        customWordCount={80}
        onCustomWordCountChange={handleWordCountChange}
        customInstructions="Roleplay an airport check-in dialogue"
        onCustomInstructionsChange={handleInstructionsChange}
      />
    );

    expect(screen.getByText(/Target Word Count/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/e.g. A job interview opening/i)).toBeInTheDocument();

    // Modify instructions
    const instructionsInput = screen.getByPlaceholderText(/e.g. A job interview opening/i);
    fireEvent.change(instructionsInput, { target: { value: 'Customer service call' } });
    expect(handleInstructionsChange).toHaveBeenCalledWith('Customer service call');
  });
});

describe('QuickModePanel component', () => {
  it('renders skill format pills and triggers quick generation with randomized defaults', () => {
    const handleQuickGenerate = vi.fn();
    const handleCefrChange = vi.fn();

    render(
      <QuickModePanel
        cefrLevel="B2"
        onCefrLevelChange={handleCefrChange}
        onQuickGenerate={handleQuickGenerate}
        isLoading={false}
        isStudent={true}
      />
    );

    // Verify format options
    expect(screen.getByText(/Surprise Mix/i)).toBeInTheDocument();
    expect(screen.getByText(/Grammar Quiz/i)).toBeInTheDocument();
    expect(screen.getByText(/Reading Article/i)).toBeInTheDocument();
    expect(screen.getByText(/Speaking Drill/i)).toBeInTheDocument();

    // Select Reading Article format
    const readingPill = screen.getByText(/Reading Article/i);
    fireEvent.click(readingPill);

    // Click Start Quick Practice button
    const startBtn = screen.getByRole('button', { name: /Start 1-Click Practice Now/i });
    fireEvent.click(startBtn);

    expect(handleQuickGenerate).toHaveBeenCalledTimes(1);
    const calledParams = handleQuickGenerate.mock.calls[0][0];
    expect(calledParams.taskType).toBe('READING');
    expect(calledParams.ecoMode).toBe(false);
    expect(calledParams.readingConfig).toBeDefined();
    expect(calledParams.readingConfig.readingLength).toBe('Medium (250-400 words)');
  });

  it('supports shuffling topic and custom topic input', () => {
    const handleQuickGenerate = vi.fn();

    render(
      <QuickModePanel
        cefrLevel="B1"
        onCefrLevelChange={vi.fn()}
        onQuickGenerate={handleQuickGenerate}
        isLoading={false}
        isStudent={false}
      />
    );

    // Click custom topic toggle
    const customToggleBtn = screen.getByRole('button', { name: /Type custom topic/i });
    fireEvent.click(customToggleBtn);

    const customInput = screen.getByPlaceholderText(/e\.g\. Past Continuous vs Past Simple in crime stories/i);
    fireEvent.change(customInput, { target: { value: 'Third Conditional in Job Interviews' } });

    // Click Generate & Preview Quick Task
    const generateBtn = screen.getByRole('button', { name: /Start 1-Click Practice Now/i });
    fireEvent.click(generateBtn);

    expect(handleQuickGenerate).toHaveBeenCalledTimes(1);
    expect(handleQuickGenerate.mock.calls[0][0].grammarTopic).toBe('Third Conditional in Job Interviews');
  });
});

describe('GrammarConfigPanel component', () => {
  it('renders unified topic selector, difficulty, question count, and eco mode', () => {
    const handleTopicChange = vi.fn();
    const handleDifficultyChange = vi.fn();
    const handleCountChange = vi.fn();
    const handleEcoChange = vi.fn();

    render(
      <GrammarConfigPanel
        cefrLevel="B1"
        grammarTopic="Present Perfect vs Past Simple"
        onGrammarTopicChange={handleTopicChange}
        domain="Academic"
        onDomainChange={vi.fn()}
        difficulty="MEDIUM"
        onDifficultyChange={handleDifficultyChange}
        numberOfQuestions={7}
        onNumberOfQuestionsChange={handleCountChange}
        ecoMode={false}
        onEcoModeChange={handleEcoChange}
        customRule=""
        onCustomRuleChange={vi.fn()}
        customVocabulary=""
        onCustomVocabularyChange={vi.fn()}
        uploadedRuleFile={null}
        onUploadedRuleFileChange={vi.fn()}
        uploadedVocabFile={null}
        onUploadedVocabFileChange={vi.fn()}
        catalog={null}
        isStudent={false}
      />
    );

    expect(screen.getByText('Grammar Practice')).toBeInTheDocument();
    expect(screen.getByText('CEFR B1')).toBeInTheDocument();
    expect(screen.getByText(/Difficulty Tier/i)).toBeInTheDocument();
    expect(screen.getByText(/Number of Questions/i)).toBeInTheDocument();
    expect(screen.getByText(/Eco Mode/i)).toBeInTheDocument();

    // Toggle Eco Mode
    const ecoBtn = screen.getByRole('button', { name: /Toggle Eco Mode/i });
    fireEvent.click(ecoBtn);
    expect(handleEcoChange).toHaveBeenCalledWith(true);

    // Change difficulty to HARD
    const hardBtn = screen.getByRole('button', { name: /Hard/i });
    fireEvent.click(hardBtn);
    expect(handleDifficultyChange).toHaveBeenCalledWith('HARD');
  });

  it('renders curriculum rules and target vocabulary with library saving', () => {
    const handleSaveRule = vi.fn();
    const handleSaveVocab = vi.fn();
    const handleRuleChange = vi.fn();
    const handleVocabChange = vi.fn();

    render(
      <GrammarConfigPanel
        cefrLevel="C1"
        grammarTopic="Inversion"
        onGrammarTopicChange={vi.fn()}
        domain="Business"
        onDomainChange={vi.fn()}
        difficulty="HARD"
        onDifficultyChange={vi.fn()}
        numberOfQuestions={5}
        onNumberOfQuestionsChange={vi.fn()}
        ecoMode={false}
        onEcoModeChange={vi.fn()}
        customRule="Inversion occurs after negative adverbials like Hardly, Seldom."
        onCustomRuleChange={handleRuleChange}
        customVocabulary="seldom, scarcely, barely, no sooner"
        onCustomVocabularyChange={handleVocabChange}
        uploadedRuleFile={null}
        onUploadedRuleFileChange={vi.fn()}
        uploadedVocabFile={null}
        onUploadedVocabFileChange={vi.fn()}
        catalog={null}
        isStudent={false}
        onSaveRuleToLibrary={handleSaveRule}
        onSaveVocabToLibrary={handleSaveVocab}
      />
    );

    // Expand the curriculum collapsible panel
    const accordionBtn = screen.getByText(/Target Rules & Vocabulary Reference/i);
    fireEvent.click(accordionBtn);

    expect(screen.getByText(/Target Grammar Rule \/ Formula/i)).toBeInTheDocument();
    expect(screen.getByText(/Target Vocabulary \/ Lexicon List/i)).toBeInTheDocument();

    // Save rule button
    const saveRuleBtn = screen.getByRole('button', { name: /Save Rule to Library/i });
    fireEvent.click(saveRuleBtn);
    expect(handleSaveRule).toHaveBeenCalledTimes(1);

    // Save vocab button
    const saveVocabBtn = screen.getByRole('button', { name: /Save Vocabulary to Library/i });
    fireEvent.click(saveVocabBtn);
    expect(handleSaveVocab).toHaveBeenCalledTimes(1);
  });
});

describe('Keyboard Hotkeys System', () => {
  beforeEach(() => {
    window.speechSynthesis = {
      speak: vi.fn(),
      cancel: vi.fn(),
      getVoices: vi.fn().mockReturnValue([]),
      paused: false,
      pending: false,
      speaking: false,
      onvoiceschanged: null,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    } as any;
    (window as any).SpeechSynthesisUtterance = vi.fn().mockImplementation((text) => ({
      text,
      lang: 'en-US',
      rate: 1,
      pitch: 1,
    }));
  });

  it('navigates cards, selects options with number keys, triggers speech on space, and opens cheatsheet on ?', async () => {
    vi.spyOn(taskApi, 'getTaskById').mockResolvedValue({
      id: 'task-hotkey-1',
      grammarTopic: 'Hotkeys Practice',
      cefrLevel: 'B2',
      type: 'MCQ',
      totalPoints: 20,
      questions: [
        {
          id: 'q-hk-1',
          questionOrder: 1,
          questionText: 'Which key is used to speak the sentence?',
          correctAnswer: 'Space',
          options: ['Enter', 'Space', 'Escape', 'Tab'],
          difficulty: 1,
          points: 10,
        },
        {
          id: 'q-hk-2',
          questionOrder: 2,
          questionText: 'Second question for navigation test.',
          correctAnswer: 'ArrowRight',
          options: ['ArrowLeft', 'ArrowRight'],
          difficulty: 1,
          points: 10,
        },
      ],
    } as any);

    render(
      <MemoryRouter initialEntries={['/student/task/task-hotkey-1']}>
        <Routes>
          <Route path="/student/task/:taskId" element={<TaskView />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Which key is used to speak the sentence?')).toBeInTheDocument();
    expect(screen.getByText('0 of 2 answered')).toBeInTheDocument();

    // 1. Press '2' (number key) to select option 2 ('Space')
    fireEvent.keyDown(window, { key: '2', code: 'Digit2' });
    expect(screen.getByText('1 of 2 answered')).toBeInTheDocument();

    // 2. Press 'Space' to trigger speech synthesis
    fireEvent.keyDown(window, { key: ' ', code: 'Space' });
    expect(window.speechSynthesis.speak).toHaveBeenCalled();

    // 3. Press 'ArrowRight' to navigate to Question 2
    fireEvent.keyDown(window, { key: 'ArrowRight', code: 'ArrowRight' });
    expect(await screen.findByText('Second question for navigation test.')).toBeInTheDocument();

    // 4. Press 'ArrowLeft' to navigate back to Question 1
    fireEvent.keyDown(window, { key: 'ArrowLeft', code: 'ArrowLeft' });
    expect(await screen.findByText('Which key is used to speak the sentence?')).toBeInTheDocument();

    // 5. Press '?' to toggle Hotkeys modal cheatsheet
    fireEvent.keyDown(window, { key: '?', code: 'Slash', shiftKey: true });
    expect(await screen.findByText('Горячие клавиши (Hotkeys)')).toBeInTheDocument();
    expect(screen.getByText('Управляйте процессотом решения заданий без мыши')).toBeInTheDocument();

    // 6. Press 'Escape' to dismiss Hotkeys modal
    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByText('Горячие клавиши (Hotkeys)')).not.toBeInTheDocument();
    });
  });

  it('SubmissionsReview focuses search input on / hotkey and clears on Escape', async () => {
    const { SubmissionsReview } = await import('../components/teacher/SubmissionsReview');
    vi.spyOn(submissionApi, 'getTeacherSubmissions').mockResolvedValue([]);
    vi.spyOn(groupApi, 'getGroups').mockResolvedValue([]);

    render(
      <MemoryRouter>
        <SubmissionsReview />
      </MemoryRouter>
    );

    const searchInput = await screen.findByPlaceholderText(/Search student, cohort, topic... \(\/\)/i);
    expect(searchInput).toBeInTheDocument();

    // Press '/' when not typing in an input
    fireEvent.keyDown(window, { key: '/', code: 'Slash' });
    expect(document.activeElement).toBe(searchInput);

    // Type text and press Escape
    fireEvent.change(searchInput, { target: { value: 'B1 Upper' } });
    expect(searchInput).toHaveValue('B1 Upper');

    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });
    expect(searchInput).toHaveValue('');
  });
});











