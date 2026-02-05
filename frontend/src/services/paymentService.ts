import api, { generateIdempotencyKey } from './api'
import type { Payment, CreatePaymentRequest, ApiResponse } from '../types'

export const paymentService = {
  createPayment: async (data: CreatePaymentRequest): Promise<ApiResponse<Payment>> => {
    const response = await api.post('/api/payments', data, {
      headers: {
        'X-Idempotency-Key': generateIdempotencyKey(),
      },
    })
    return response.data
  },
  
  getPayment: async (paymentId: string): Promise<ApiResponse<Payment>> => {
    const response = await api.get(`/api/payments/${paymentId}`)
    return response.data
  },
  
  getPaymentStatus: async (paymentId: string): Promise<ApiResponse<{ paymentId: string; status: string }>> => {
    const response = await api.get(`/api/payments/${paymentId}/status`)
    return response.data
  },
  
  getPaymentByOrderId: async (orderId: string): Promise<ApiResponse<Payment>> => {
    const response = await api.get(`/api/payments/order/${orderId}`)
    return response.data
  },
  
  refundPayment: async (
    paymentId: string, 
    reason: string, 
    amount?: number
  ): Promise<ApiResponse<Payment>> => {
    const response = await api.post(`/api/payments/${paymentId}/refund`, {
      reason,
      amount,
    }, {
      headers: {
        'X-Idempotency-Key': generateIdempotencyKey(),
      },
    })
    return response.data
  },
  
  retryPayment: async (paymentId: string, gatewayToken: string): Promise<ApiResponse<Payment>> => {
    const response = await api.post(`/api/payments/${paymentId}/retry`, {
      gatewayToken,
    }, {
      headers: {
        'X-Idempotency-Key': generateIdempotencyKey(),
      },
    })
    return response.data
  },
}
