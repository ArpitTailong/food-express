import api from './api'
import type { LoginRequest, RegisterRequest, AuthResponse, User } from '../types'

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
    await api.post('/api/auth/logout')
  },
  
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await api.post('/api/auth/refresh', { refreshToken })
    return response.data
  },
  
  getCurrentUser: async (): Promise<{ success: boolean; data: User }> => {
    const response = await api.get('/api/auth/me')
    return response.data
  },
  
  forgotPassword: async (email: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post('/api/auth/forgot-password', { email })
    return response.data
  },
  
  resetPassword: async (token: string, password: string): Promise<{ success: boolean; message: string }> => {
    const response = await api.post('/api/auth/reset-password', { token, password })
    return response.data
  },
}
