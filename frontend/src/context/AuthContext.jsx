import React, { createContext, useContext, useState, useEffect } from 'react';
import {
  setAccessToken,
  loginUser,
  registerUser,
  logoutUser,
  refreshSession,
  fetchCurrentUser
} from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [activeOrganization, setActiveOrganization] = useState(null);
  const [loading, setLoading] = useState(true);

  const initAuth = async () => {
    try {
      const data = await refreshSession();
      if (data?.data?.accessToken) {
        setAccessToken(data.data.accessToken);
        setUser(data.data.user);
        if (data.data.user?.memberships?.length > 0) {
          setActiveOrganization(data.data.user.memberships[0]);
        }
      }
    } catch (err) {
      setAccessToken(null);
      setUser(null);
      setActiveOrganization(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    initAuth();
  }, []);

  useEffect(() => {
    if (user?.memberships?.length > 0 && (!activeOrganization || !user.memberships.some(m => m.organizationId === activeOrganization.organizationId))) {
      setActiveOrganization(user.memberships[0]);
    }
  }, [user]);

  const login = async (email, password) => {
    const res = await loginUser({ email, password });
    if (res?.data?.accessToken) {
      setAccessToken(res.data.accessToken);
      setUser(res.data.user);
      if (res.data.user?.memberships?.length > 0) {
        setActiveOrganization(res.data.user.memberships[0]);
      }
    }
    return res;
  };

  const register = async (firstName, lastName, email, password, organizationName) => {
    const res = await registerUser({ firstName, lastName, email, password, organizationName });
    if (res?.data?.accessToken) {
      setAccessToken(res.data.accessToken);
      setUser(res.data.user);
      if (res.data.user?.memberships?.length > 0) {
        setActiveOrganization(res.data.user.memberships[0]);
      }
    }
    return res;
  };

  const logout = async () => {
    try {
      await logoutUser();
    } finally {
      setAccessToken(null);
      setUser(null);
      setActiveOrganization(null);
    }
  };

  const handleOAuthToken = async (token) => {
    setAccessToken(token);
    try {
      const res = await fetchCurrentUser();
      if (res?.data) {
        setUser(res.data);
        if (res.data.memberships?.length > 0) {
          setActiveOrganization(res.data.memberships[0]);
        }
      }
    } catch (err) {
      setAccessToken(null);
      setUser(null);
      setActiveOrganization(null);
      throw err;
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        activeOrganization,
        setActiveOrganization,
        loading,
        isAuthenticated: !!user,
        login,
        register,
        logout,
        handleOAuthToken,
        setUser
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
