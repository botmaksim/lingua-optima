import { useAuthStore } from '../store/authStore';

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
