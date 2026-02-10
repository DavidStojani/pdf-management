import React, { createContext, useContext, useState, useEffect, useCallback } from "react";
import { api } from "@/lib/api";
import { toast } from "sonner";

interface AuthContextType {
  isAuthenticated: boolean;
  userEmail: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (
    username: string,
    email: string,
    password: string,
    confirmPassword: string
  ) => Promise<void>;
  forgotPassword: (email: string) => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be inside AuthProvider");
  return ctx;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(api.isAuthenticated());
  const [userEmail, setUserEmail] = useState<string | null>(
    localStorage.getItem("vault_email")
  );
  const [loading, setLoading] = useState(false);

  const logout = useCallback(() => {
    api.clearToken();
    localStorage.removeItem("vault_email");
    setIsAuthenticated(false);
    setUserEmail(null);
  }, []);

  useEffect(() => {
    const handler = () => {
      logout();
      toast.error("Session expired. Please log in again.");
    };
    window.addEventListener("auth:expired", handler);
    return () => window.removeEventListener("auth:expired", handler);
  }, [logout]);

  const login = async (email: string, password: string) => {
    setLoading(true);
    try {
      await api.login(email, password);
      localStorage.setItem("vault_email", email);
      setUserEmail(email);
      setIsAuthenticated(true);
    } finally {
      setLoading(false);
    }
  };

  const register = async (
    username: string,
    email: string,
    password: string,
    confirmPassword: string
  ) => {
    setLoading(true);
    try {
      await api.register(username, email, password, confirmPassword);
      localStorage.setItem("vault_email", email);
      setUserEmail(email);
      setIsAuthenticated(true);
    } finally {
      setLoading(false);
    }
  };

  const forgotPassword = async (email: string) => {
    setLoading(true);
    try {
      await api.forgotPassword(email);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, userEmail, login, register, forgotPassword, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};
