/**
 * @file ProtectedRoute.tsx
 * @brief Route protection wrapper enforcing authentication and role-based access control.
 */

import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Role } from '../../types/user';
import { LoadingSpinner } from './LoadingSpinner';

/**
 * @interface ProtectedRouteProps
 * @brief Props definition for route guard component.
 */
interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: Role[];
}

/**
 * @brief Guard wrapper component verifying user authentication and roles prior to rendering child routes.
 * @param props Component properties containing children and allowedRoles.
 * @return React component element or redirect.
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  allowedRoles,
}) => {
  const { user, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-surface">
        <LoadingSpinner size="lg" message="Authenticating..." />
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    const fallbackPath = user.role === 'STUDENT' ? '/student' : '/teacher';
    return <Navigate to={fallbackPath} replace />;
  }

  return <>{children}</>;
};
