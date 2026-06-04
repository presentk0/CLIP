import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import styles from './ProtectedRoute.module.css';

// 로그인 체크용
export default function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  // 로딩 중
  if (isLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.spinner} aria-hidden="true" />
        <p>로딩 중...</p>
      </div>
    );
  }

  // 비로그인 -> 로그인 페이지로 (원래 가려던 곳 기억)
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 로그인됨 -> 원래 페이지 보여줌
  return children;
}