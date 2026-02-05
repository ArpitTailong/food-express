import api from './api'
import type { OrderAnalytics, RevenueAnalytics, ApiResponse } from '../types'

export const analyticsService = {
  getOrderAnalytics: async (
    startDate: string,
    endDate: string,
    restaurantId?: string
  ): Promise<ApiResponse<OrderAnalytics>> => {
    const response = await api.get('/api/analytics/orders', {
      params: { startDate, endDate, restaurantId },
    })
    return response.data
  },
  
  getRevenueAnalytics: async (
    startDate: string,
    endDate: string,
    groupBy: 'DAY' | 'WEEK' | 'MONTH' = 'DAY'
  ): Promise<ApiResponse<RevenueAnalytics>> => {
    const response = await api.get('/api/analytics/revenue', {
      params: { startDate, endDate, groupBy },
    })
    return response.data
  },
  
  getUserAnalytics: async (userId: string): Promise<ApiResponse<{
    totalOrders: number
    totalSpent: number
    favoriteRestaurants: { restaurantId: string; name: string; orderCount: number }[]
  }>> => {
    const response = await api.get(`/api/analytics/users/${userId}`)
    return response.data
  },
  
  trackEvent: async (event: {
    eventType: string
    userId?: string
    properties?: Record<string, unknown>
  }): Promise<ApiResponse<void>> => {
    const response = await api.post('/api/analytics/events', {
      ...event,
      timestamp: new Date().toISOString(),
    })
    return response.data
  },
}
