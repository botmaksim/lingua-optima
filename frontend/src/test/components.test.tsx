/**
 * @file components.test.tsx
 * @brief Unit tests for common React UI components (CefrBadge, Toast, LoginPage).
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { CefrBadge } from '../components/common/CefrBadge';
import { Toast } from '../components/common/Toast';
import { LoginPage } from '../components/auth/LoginPage';
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

