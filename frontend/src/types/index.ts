// User types
export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  phoneNumber?: string
  role: 'CUSTOMER' | 'ADMIN' | 'RESTAURANT_OWNER' | 'DRIVER'
  avatar?: string
  createdAt: string
}

// Auth types
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phoneNumber?: string
}

export interface AuthResponse {
  success: boolean
  message: string
  data: {
    accessToken: string
    refreshToken: string
    tokenType: string
    expiresIn: number
    user?: User
  }
}

// Address types
export interface Address {
  id: string
  label: string
  street: string
  city: string
  state: string
  zipCode: string
  country: string
  latitude?: number
  longitude?: number
  isDefault: boolean
}

// Restaurant types
export interface Restaurant {
  id: string
  name: string
  description: string
  image: string
  coverImage?: string
  cuisine: string[]
  rating: number
  reviewCount: number
  deliveryTime: string
  deliveryFee: number
  minOrder: number
  isOpen: boolean
  address: Address
  openingHours: {
    day: string
    open: string
    close: string
  }[]
}

// Menu types
export interface MenuItem {
  id: string
  restaurantId: string
  name: string
  description: string
  price: number
  image?: string
  category: string
  isAvailable: boolean
  isPopular?: boolean
  calories?: number
  allergens?: string[]
  customizations?: MenuCustomization[]
}

export interface MenuCustomization {
  id: string
  name: string
  required: boolean
  maxSelections: number
  options: {
    id: string
    name: string
    price: number
  }[]
}

// Cart types
export interface CartItem {
  id: string
  menuItem: MenuItem
  quantity: number
  specialInstructions?: string
  selectedCustomizations?: {
    customizationId: string
    optionIds: string[]
  }[]
  totalPrice: number
}

export interface Cart {
  restaurantId: string
  restaurantName: string
  items: CartItem[]
  subtotal: number
  deliveryFee: number
  tax: number
  total: number
}

// Order types
export type OrderStatus = 
  | 'PENDING'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'READY_FOR_PICKUP'
  | 'PICKED_UP'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED'

export interface OrderItem {
  id: string
  menuItemId: string
  name: string
  quantity: number
  unitPrice: number
  totalPrice: number
  specialInstructions?: string
}

export interface Order {
  id: string
  orderNumber: string
  customerId: string
  customerName: string
  restaurantId: string
  restaurantName: string
  status: OrderStatus
  items: OrderItem[]
  deliveryAddress: Address
  subtotal: number
  deliveryFee: number
  tax: number
  tip?: number
  total: number
  paymentMethod: string
  paymentStatus: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'
  driverId?: string
  driverName?: string
  driverPhone?: string
  specialInstructions?: string
  estimatedDeliveryTime?: string
  actualDeliveryTime?: string
  rating?: OrderRating
  createdAt: string
  updatedAt: string
}

export interface OrderRating {
  foodRating: number
  deliveryRating: number
  overallRating: number
  comment?: string
}

export interface CreateOrderRequest {
  restaurantId: string
  restaurantName: string
  customerId: string
  customerName: string
  customerPhone: string
  deliveryAddress: Omit<Address, 'id' | 'label' | 'isDefault'>
  items: {
    menuItemId: string
    name: string
    quantity: number
    unitPrice: number
    specialInstructions?: string
  }[]
  specialInstructions?: string
  paymentMethod: string
  tip?: number
}

// Payment types
export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'

export interface Payment {
  id: string
  orderId: string
  customerId: string
  amount: number
  currency: string
  status: PaymentStatus
  paymentMethod: string
  gatewayTransactionId?: string
  cardLastFour?: string
  cardBrand?: string
  failureReason?: string
  refundedAmount?: number
  createdAt: string
  updatedAt: string
}

export interface CreatePaymentRequest {
  orderId: string
  customerId: string
  amount: number
  currency: string
  paymentMethod: string
  gatewayToken: string
}

// Notification types
export interface Notification {
  id: string
  userId: string
  type: 'ORDER_UPDATE' | 'PROMOTION' | 'SYSTEM' | 'PAYMENT'
  title: string
  message: string
  data?: Record<string, unknown>
  isRead: boolean
  createdAt: string
}

// Analytics types
export interface OrderAnalytics {
  totalOrders: number
  completedOrders: number
  cancelledOrders: number
  averageOrderValue: number
  ordersByStatus: Record<OrderStatus, number>
  ordersByDay: { date: string; count: number }[]
}

export interface RevenueAnalytics {
  totalRevenue: number
  netRevenue: number
  refunds: number
  averageOrderValue: number
  revenueByDay: { date: string; revenue: number }[]
}

// API Response types
export interface ApiResponse<T> {
  success: boolean
  message?: string
  data: T
  errors?: { field: string; message: string }[]
}

export interface PaginatedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}
