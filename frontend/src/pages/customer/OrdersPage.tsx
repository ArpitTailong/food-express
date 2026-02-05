import { useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import {
  MagnifyingGlassIcon,
  ShoppingBagIcon,
  ChevronRightIcon,
} from '@heroicons/react/24/outline'
import { useAuthStore } from '../../store/authStore'
import OrderStatusBadge from '../../components/common/OrderStatusBadge'
import EmptyState from '../../components/common/EmptyState'
import type { Order, OrderStatus } from '../../types'

// Mock orders data
const mockOrders: Order[] = [
  {
    id: '1',
    orderNumber: 'ORD-20260205-0001',
    customerId: 'cust-001',
    customerName: 'John Doe',
    restaurantId: 'rest-001',
    restaurantName: 'Pizza Palace',
    status: 'DELIVERED',
    items: [
      { id: '1', menuItemId: 'item-1', name: 'Margherita Pizza', quantity: 2, unitPrice: 14.99, totalPrice: 29.98 },
      { id: '2', menuItemId: 'item-2', name: 'Garlic Bread', quantity: 1, unitPrice: 5.99, totalPrice: 5.99 },
    ],
    deliveryAddress: { id: '1', label: 'Home', street: '123 Main St', city: 'New York', state: 'NY', zipCode: '10001', country: 'USA', isDefault: true },
    subtotal: 35.97,
    deliveryFee: 2.99,
    tax: 2.97,
    total: 41.93,
    paymentMethod: 'CARD',
    paymentStatus: 'SUCCESS',
    createdAt: '2026-02-05T10:30:00Z',
    updatedAt: '2026-02-05T11:15:00Z',
  },
  {
    id: '2',
    orderNumber: 'ORD-20260204-0003',
    customerId: 'cust-001',
    customerName: 'John Doe',
    restaurantId: 'rest-002',
    restaurantName: 'Burger Barn',
    status: 'OUT_FOR_DELIVERY',
    items: [
      { id: '1', menuItemId: 'item-1', name: 'Classic Burger', quantity: 1, unitPrice: 12.99, totalPrice: 12.99 },
      { id: '2', menuItemId: 'item-2', name: 'Fries', quantity: 1, unitPrice: 4.99, totalPrice: 4.99 },
    ],
    deliveryAddress: { id: '1', label: 'Home', street: '123 Main St', city: 'New York', state: 'NY', zipCode: '10001', country: 'USA', isDefault: true },
    subtotal: 17.98,
    deliveryFee: 0,
    tax: 1.48,
    total: 19.46,
    paymentMethod: 'CARD',
    paymentStatus: 'SUCCESS',
    driverName: 'Mike Driver',
    driverPhone: '+1555123456',
    estimatedDeliveryTime: '2026-02-05T14:30:00Z',
    createdAt: '2026-02-05T14:00:00Z',
    updatedAt: '2026-02-05T14:20:00Z',
  },
  {
    id: '3',
    orderNumber: 'ORD-20260203-0012',
    customerId: 'cust-001',
    customerName: 'John Doe',
    restaurantId: 'rest-003',
    restaurantName: 'Sushi Station',
    status: 'CANCELLED',
    items: [
      { id: '1', menuItemId: 'item-1', name: 'California Roll', quantity: 2, unitPrice: 8.99, totalPrice: 17.98 },
    ],
    deliveryAddress: { id: '1', label: 'Home', street: '123 Main St', city: 'New York', state: 'NY', zipCode: '10001', country: 'USA', isDefault: true },
    subtotal: 17.98,
    deliveryFee: 3.99,
    tax: 1.48,
    total: 23.45,
    paymentMethod: 'CARD',
    paymentStatus: 'REFUNDED',
    createdAt: '2026-02-03T18:00:00Z',
    updatedAt: '2026-02-03T18:30:00Z',
  },
]

const statusFilters: { value: OrderStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All Orders' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'PREPARING', label: 'Preparing' },
  { value: 'OUT_FOR_DELIVERY', label: 'On the way' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

export default function OrdersPage() {
  const { user } = useAuthStore()
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<OrderStatus | 'ALL'>('ALL')

  const filteredOrders = mockOrders
    .filter((order) => {
      const matchesSearch = order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        order.restaurantName.toLowerCase().includes(searchQuery.toLowerCase())
      const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter
      return matchesSearch && matchesStatus
    })

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">My Orders</h1>

      {/* Search and Filter */}
      <div className="flex flex-col sm:flex-row gap-4 mb-6">
        <div className="relative flex-1">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            placeholder="Search orders..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input pl-10"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as OrderStatus | 'ALL')}
          className="input w-full sm:w-48"
        >
          {statusFilters.map((filter) => (
            <option key={filter.value} value={filter.value}>
              {filter.label}
            </option>
          ))}
        </select>
      </div>

      {/* Orders List */}
      {filteredOrders.length === 0 ? (
        <EmptyState
          icon={<ShoppingBagIcon className="h-8 w-8 text-gray-400" />}
          title="No orders found"
          description={searchQuery || statusFilter !== 'ALL' 
            ? "Try adjusting your search or filters" 
            : "You haven't placed any orders yet. Start browsing restaurants!"}
          action={
            <Link to="/restaurants" className="btn-primary">
              Browse Restaurants
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {filteredOrders.map((order, index) => (
            <motion.div
              key={order.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
            >
              <Link to={`/orders/${order.id}/track`} className="block">
                <div className="card p-4 hover:border-primary-200 hover:shadow-md transition-all">
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h3 className="font-semibold text-gray-900">{order.restaurantName}</h3>
                      <p className="text-sm text-gray-500">{order.orderNumber}</p>
                    </div>
                    <OrderStatusBadge status={order.status} size="sm" />
                  </div>

                  <div className="flex items-center gap-4 text-sm text-gray-500 mb-3">
                    <span>{format(new Date(order.createdAt), 'MMM d, yyyy • h:mm a')}</span>
                    <span>•</span>
                    <span>{order.items.length} items</span>
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      {order.items.slice(0, 3).map((item, i) => (
                        <span key={i} className="text-sm text-gray-600">
                          {item.quantity}x {item.name}
                          {i < Math.min(order.items.length - 1, 2) && ','}
                        </span>
                      ))}
                      {order.items.length > 3 && (
                        <span className="text-sm text-gray-400">
                          +{order.items.length - 3} more
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-gray-900">
                        ${order.total.toFixed(2)}
                      </span>
                      <ChevronRightIcon className="h-5 w-5 text-gray-400" />
                    </div>
                  </div>

                  {/* Active order alert */}
                  {['CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY'].includes(order.status) && (
                    <div className="mt-3 pt-3 border-t border-gray-100">
                      <p className="text-sm text-primary-600 font-medium">
                        {order.status === 'OUT_FOR_DELIVERY' && order.estimatedDeliveryTime
                          ? `Arriving by ${format(new Date(order.estimatedDeliveryTime), 'h:mm a')}`
                          : 'Order in progress — Tap to track'}
                      </p>
                    </div>
                  )}
                </div>
              </Link>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  )
}
