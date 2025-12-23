import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/axiosConfig.js';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

const normalizeRole = (role) => (typeof role === 'string' ? role.trim().toUpperCase() : null);

const normalizeUser = (user) => {
  if (!user) {
    return null;
  }

  return {
    ...user,
    role: normalizeRole(user.role)
  };
};

const loadStoredUser = () => {
  try {
    const raw = localStorage.getItem('user');
    return raw ? normalizeUser(JSON.parse(raw)) : null;
  } catch (error) {
    console.error('Failed to parse stored user', error);
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => loadStoredUser());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  }, [user]);

  const login = async (username, password) => {
    setLoading(true);
    try {
      const response = await api.post('/auth/login', { username, password });
      const { token, userId, role, fullName, username: responseUsername } = response.data;
      localStorage.setItem('token', token);
      const userPayload = normalizeUser({
        userId,
        username: responseUsername || username,
        role,
        fullName
      });
      setUser(userPayload);
      return { success: true };
    } catch (error) {
      console.error('Login failed', error);
      return {
        success: false,
        message: error.response?.data?.message || 'Invalid username or password'
      };
    } finally {
      setLoading(false);
    }
  };

  const register = async (payload) => {
    setLoading(true);
    try {
      const response = await api.post('/auth/register', payload);
      const { token, userId, role, fullName, username } = response.data;
      localStorage.setItem('token', token);
      const userPayload = normalizeUser({ userId, username, role, fullName });
      setUser(userPayload);
      return { success: true };
    } catch (error) {
      console.error('Registration failed', error);
      return {
        success: false,
        message: error.response?.data || 'Unable to create account'
      };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };

  const contextValue = useMemo(
    () => ({
      user,
      loading,
      login,
      register,
      logout,
      isAuthenticated: Boolean(user)
    }),
    [user, loading]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
};
