/**
 * @file components.test.tsx
 * @brief Unit tests for common React UI components (CefrBadge, Toast, LoginPage).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { CefrBadge } from '../components/common/CefrBadge';
import { Toast } from '../components/common/Toast';
import { LoginPage } from '../components/auth/LoginPage';
import { TranslatorDropdown } from '../components/common/TranslatorDropdown';
import { AIReview } from '../components/student/AIReview';
import { submissionApi } from '../api/submissionApi';
import { useAuthStore } from '../store/authStore';

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
  it('renders message and calls onClose when dismiss button clicked', () => {
    const handleClose = vi.fn();
    render(
      <Toast
        id="toast-1"
        type="success"
        message="Saved successfully"
        onClose={handleClose}
      />
    );

    expect(screen.getByText('Saved successfully')).toBeInTheDocument();
    const closeBtn = screen.getByRole('button', { name: /dismiss toast/i });
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledWith('toast-1');
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

