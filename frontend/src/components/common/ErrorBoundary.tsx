/**
 * @file ErrorBoundary.tsx
 * @brief React error boundary for catching rendering failures and providing a fallback UI.
 */

import { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

/**
 * @brief Props for the ErrorBoundary component.
 */
interface Props {
  /** @brief Property representing children in Props. */
  children: ReactNode;
}

/**
 * @brief State tracking whether an uncaught error occurred during render.
 */
interface State {
  /** @brief Property representing has error in State. */
  hasError: boolean;
  /** @brief Property representing error in State. */
  error?: Error;
}

/**
 * @brief Top-level React error boundary displaying user-friendly recovery UI on unhandled errors.
 */
export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
  };

  /**
   * @brief Derives error state from caught exception.
   * @return Updated component state with hasError set to true.
   */
  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  /**
   * @brief Logs error diagnostic details when a child component throws.
   */
  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  /**
   * @brief Renders child components or error fallback UI.
   * @return Rendered React node.
   */
  public render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-[400px] flex flex-col items-center justify-center p-6 text-center">
          <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-500 flex items-center justify-center mb-4">
            <AlertTriangle className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 mb-2">Something went wrong</h2>
          <p className="text-sm text-slate-600 max-w-md mb-6">
            An unexpected error occurred while rendering this page.
          </p>
          <button
            onClick={() => window.location.reload()}
            className="flex items-center space-x-2 px-4 py-2 rounded-xl bg-primary text-white font-medium hover:bg-primary-hover transition"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Reload Page</span>
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
