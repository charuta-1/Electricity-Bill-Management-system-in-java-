import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.js';

const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="container py-5 text-center">Checking permissions...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const normalizedRole = user.role?.toUpperCase();
  if (allowedRoles && normalizedRole && !allowedRoles.map((role) => role.toUpperCase()).includes(normalizedRole)) {
    const fallback = normalizedRole === 'ADMIN' ? '/admin/dashboard' : '/customer/dashboard';
    return <Navigate to={fallback} replace />;
  }

  return children;
};

export default ProtectedRoute;
