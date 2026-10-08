import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { CefrBadge } from '../components/common/CefrBadge';
import { Toast } from '../components/common/Toast';

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
