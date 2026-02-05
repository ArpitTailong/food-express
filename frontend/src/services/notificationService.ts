import api from './api'
import type { Notification, ApiResponse, PaginatedResponse } from '../types'

export const notificationService = {
  getNotifications: async (
    userId: string,
    page = 0,
    size = 20,
    unreadOnly = false
  ): Promise<ApiResponse<PaginatedResponse<Notification>>> => {
    const response = await api.get(`/api/notifications/user/${userId}`, {
      params: { page, size, unreadOnly },
    })
    return response.data
  },
  
  markAsRead: async (notificationId: string): Promise<ApiResponse<Notification>> => {
    const response = await api.put(`/api/notifications/${notificationId}/read`)
    return response.data
  },
  
  markAllAsRead: async (userId: string): Promise<ApiResponse<void>> => {
    const response = await api.put(`/api/notifications/user/${userId}/read-all`)
    return response.data
  },
  
  getUnreadCount: async (userId: string): Promise<ApiResponse<{ count: number }>> => {
    const response = await api.get(`/api/notifications/user/${userId}/unread-count`)
    return response.data
  },
  
  deleteNotification: async (notificationId: string): Promise<ApiResponse<void>> => {
    const response = await api.delete(`/api/notifications/${notificationId}`)
    return response.data
  },
}
