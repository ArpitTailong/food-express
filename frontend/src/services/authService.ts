import api from './api'
import type { LoginRequest, RegisterRequest, AuthResponse, User, UserInfo } from '../types'

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/login', data)
    return response.data
  },
  
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/register', data)
    return response.data
  },
  
  logout: async (): Promise<void> => {
    try {
      await api.post('/api/auth/logout')
    } catch (e) {
      // Ignore logout errors
    }
  },
  
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/refresh', { refreshToken })
    return response.data
  },
  
  getCurrentUser: async (): Promise<{ success: boolean; data: UserInfo }> => {
    const response = await api.get('/api/auth/me')
    return response.data
  },
  
  // Convert UserInfo to User object for frontend
  userInfoToUser: (info: UserInfo): User => ({
    id: info.id,
    email: info.email,
    roles: info.roles,
    role: (info.roles[0] as User['role']) || 'CUSTOMER',
  }),
  
  forgotPassword: async (email: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post('/api/auth/forgot-password', { email })
    return response.data
  },
  
  resetPassword: async (token: string, password: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post('/api/auth/reset-password', { token, password })
    return response.data
  },
}
