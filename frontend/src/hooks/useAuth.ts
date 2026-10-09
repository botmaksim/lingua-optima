import { useAuthStore } from '../store/authStore';

/**
 * @file useAuth.ts
 * @brief Custom React hook exposing role-based access helpers and authentication state.
 */

/**
 * @brief Convenience hook exposing current user, role booleans (isStudent, isTeacher), and auth functions.
 * @return Auth context object.
 */
export const useAuth = () => {
  const { user, isAuthenticated, isLoading, login, register, logout } = useAuthStore();
  const isStudent = user?.role === 'STUDENT';
  const isTeacher = user?.role === 'TEACHER' || user?.role === 'ADMIN';

  return {
    user,
    isAuthenticated,
    isLoading,
    isStudent,
    isTeacher,
    login,
    register,
    logout,
  };
};
