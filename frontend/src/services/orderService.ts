import api, { generateIdempotencyKey } from './api'
import type { Order, CreateOrderRequest, OrderRating, ApiResponse, PaginatedResponse } from '../types'

export const orderService = {
  createOrder: async (data: CreateOrderRequest): Promise<ApiResponse<Order>> => {
    const response = await api.post('/api/orders', data, {
      headers: {
        'X-Idempotency-Key': generateIdempotencyKey(),
      },
    })
    return response.data
  },
  
  getOrder: async (orderId: string): Promise<ApiResponse<Order>> => {
    const response = await api.get(`/api/orders/${orderId}`)
    return response.data
  },
  
  getOrderStatus: async (orderId: string): Promise<ApiResponse<{ orderId: string; status: string }>> => {
    const response = await api.get(`/api/orders/${orderId}/status`)
    return response.data
  },
  
  getCustomerOrders: async (
    customerId: string, 
    page = 0, 
    size = 20
  ): Promise<ApiResponse<PaginatedResponse<Order>>> => {
    const response = await api.get(`/api/orders/customer/${customerId}`, {
      params: { page, size },
    })
    return response.data
  },
  
  cancelOrder: async (orderId: string, reason: string): Promise<ApiResponse<Order>> => {
    const response = await api.post(`/api/orders/${orderId}/cancel`, {
      reason,
      cancelledBy: 'CUSTOMER',
    })
    return response.data
  },
  
  rateOrder: async (orderId: string, rating: OrderRating): Promise<ApiResponse<Order>> => {
    const response = await api.post(`/api/orders/${orderId}/rate`, rating)
    return response.data
  },
  
  // Admin endpoints
  getAllOrders: async (
    page = 0, 
    size = 20,
    status?: string
  ): Promise<ApiResponse<PaginatedResponse<Order>>> => {
    const response = await api.get('/api/orders', {
      params: { page, size, status },
    })
    return response.data
  },
  
  updateOrderStatus: async (
    orderId: string, 
    status: string, 
    additionalData?: Record<string, unknown>
  ): Promise<ApiResponse<Order>> => {
    const response = await api.put(`/api/orders/${orderId}/status`, {
      status,
      ...additionalData,
    })
    return response.data
  },
}
