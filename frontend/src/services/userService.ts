import api from './api'
import type { User, Address, ApiResponse } from '../types'

export const userService = {
  getProfile: async (): Promise<ApiResponse<User>> => {
    const response = await api.get('/api/users/profile')
    return response.data
  },
  
  updateProfile: async (data: Partial<User>): Promise<ApiResponse<User>> => {
    const response = await api.put('/api/users/profile', data)
    return response.data
  },
  
  getAddresses: async (): Promise<ApiResponse<Address[]>> => {
    const response = await api.get('/api/users/addresses')
    return response.data
  },
  
  addAddress: async (address: Omit<Address, 'id'>): Promise<ApiResponse<Address>> => {
    const response = await api.post('/api/users/addresses', address)
    return response.data
  },
  
  updateAddress: async (addressId: string, address: Partial<Address>): Promise<ApiResponse<Address>> => {
    const response = await api.put(`/api/users/addresses/${addressId}`, address)
    return response.data
  },
  
  deleteAddress: async (addressId: string): Promise<ApiResponse<void>> => {
    const response = await api.delete(`/api/users/addresses/${addressId}`)
    return response.data
  },
  
  setDefaultAddress: async (addressId: string): Promise<ApiResponse<Address>> => {
    const response = await api.put(`/api/users/addresses/${addressId}/default`)
    return response.data
  },
}
