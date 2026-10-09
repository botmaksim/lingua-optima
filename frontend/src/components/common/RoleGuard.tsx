/**
 * @file RoleGuard.tsx
 * @brief Authorization guard rendering children conditionally based on authenticated user role.
 */

import React from 'react';
import { Role } from '../../types/user';
import { useAuth } from '../../hooks/useAuth';

/**
 * @brief Props for the RoleGuard component.
 */
interface RoleGuardProps {
  /** @brief Property representing allowed roles in RoleGuardProps. */
  allowedRoles: Role[];
  /** @brief Property representing children in RoleGuardProps. */
  children: React.ReactNode;
  /** @brief Property representing fallback in RoleGuardProps. */
  fallback?: React.ReactNode;
}

/**
 * @brief Conditionally renders children if the authenticated user has one of the allowed roles.
 * @param allowedRoles Array of permitted user roles.
 * @param children Elements rendered upon authorization.
 * @param fallback Optional fallback element rendered if unauthorized.
 * @return JSX elements or fallback.
 */
export const RoleGuard: React.FC<RoleGuardProps> = ({
  allowedRoles,
  children,
  fallback = null,
}) => {
  const { user } = useAuth();

  if (!user || !allowedRoles.includes(user.role)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};
