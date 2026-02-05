import { useState } from 'react'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import {
  MagnifyingGlassIcon,
  FunnelIcon,
  EyeIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
} from '@heroicons/react/24/outline'
import Modal from '../../components/common/Modal'

const mockOrders = [
  {
    id: 'ORD-20260205-001',
    customer: { name: 'John Doe', email: 'john@example.com', phone: '+1 555-0101' },
    restaurant: 'Pizza Palace',
    items: [
      { name: 'Margherita Pizza', qty: 2, price: 12.99 },
      { name: 'Garlic Bread', qty: 1, price: 4.99 },
    ],
    total: 35.46,
    status: 'DELIVERED',
    paymentStatus: 'COMPLETED',
    createdAt: '2026-02-05T14:30:00Z',
    address: '123 Main St, New York, NY 10001',
  },
  {
    id: 'ORD-20260205-002',
    customer: { name: 'Jane Smith', email: 'jane@example.com', phone: '+1 555-0102' },
    restaurant: 'Burger Barn',
    items: [
      { name: 'Double Cheeseburger', qty: 1, price: 15.99 },
      { name: 'Fries', qty: 2, price: 5.99 },
    ],
    total: 30.02,
    status: 'PREPARING',
    paymentStatus: 'COMPLETED',
    createdAt: '2026-02-05T15:45:00Z',
    address: '456 Oak Ave, Brooklyn, NY 11201',
  },
  {
    id: 'ORD-20260205-003',
    customer: { name: 'Mike Johnson', email: 'mike@example.com', phone: '+1 555-0103' },
    restaurant: 'Sushi Supreme',
    items: [
      { name: 'Dragon Roll', qty: 2, price: 18.99 },
      { name: 'Miso Soup', qty: 2, price: 4.99 },
    ],
    total: 53.65,
    status: 'OUT_FOR_DELIVERY',
    paymentStatus: 'COMPLETED',
    createdAt: '2026-02-05T16:00:00Z',
    address: '789 Park Blvd, Queens, NY 11375',
  },
  {
    id: 'ORD-20260205-004',
    customer: { name: 'Sarah Wilson', email: 'sarah@example.com', phone: '+1 555-0104' },
    restaurant: 'Taco Town',
    items: [
      { name: 'Burrito Bowl', qty: 1, price: 14.99 },
      { name: 'Chips & Guac', qty: 1, price: 6.99 },
    ],
    total: 24.83,
    status: 'PENDING',
    paymentStatus: 'PENDING',
    createdAt: '2026-02-05T16:15:00Z',
    address: '321 Elm St, Manhattan, NY 10010',
  },
  {
    id: 'ORD-20260205-005',
    customer: { name: 'Chris Brown', email: 'chris@example.com', phone: '+1 555-0105' },
    restaurant: 'Thai Delight',
    items: [
      { name: 'Pad Thai', qty: 2, price: 16.99 },
      { name: 'Spring Rolls', qty: 1, price: 8.99 },
    ],
    total: 47.02,
    status: 'CANCELLED',
    paymentStatus: 'REFUNDED',
    createdAt: '2026-02-05T12:00:00Z',
    address: '654 Pine Rd, Bronx, NY 10451',
  },
]

const statusOptions = ['ALL', 'PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED']

const getStatusColor = (status: string) => {
  switch (status) {
    case 'DELIVERED':
      return 'bg-green-100 text-green-700'
    case 'PREPARING':
      return 'bg-yellow-100 text-yellow-700'
    case 'OUT_FOR_DELIVERY':
      return 'bg-blue-100 text-blue-700'
    case 'PENDING':
      return 'bg-purple-100 text-purple-700'
    case 'CONFIRMED':
      return 'bg-indigo-100 text-indigo-700'
    case 'CANCELLED':
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

const getPaymentStatusColor = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'text-green-600'
    case 'PENDING':
      return 'text-yellow-600'
    case 'REFUNDED':
      return 'text-blue-600'
    default:
      return 'text-gray-600'
  }
}

export default function AdminOrders() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [selectedOrder, setSelectedOrder] = useState<typeof mockOrders[0] | null>(null)
  const [currentPage, setCurrentPage] = useState(1)
  const itemsPerPage = 10

  const filteredOrders = mockOrders.filter((order) => {
    const matchesSearch =
      order.id.toLowerCase().includes(search.toLowerCase()) ||
      order.customer.name.toLowerCase().includes(search.toLowerCase())
    const matchesStatus = statusFilter === 'ALL' || order.status === statusFilter
    return matchesSearch && matchesStatus
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Orders</h1>
        <div className="flex gap-3">
          <button className="btn-outline flex items-center gap-2">
            <FunnelIcon className="h-5 w-5" />
            Export
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="card p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="relative flex-1">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              className="input pl-10"
              placeholder="Search by order ID or customer..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <select
            className="input w-full md:w-48"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            {statusOptions.map((status) => (
              <option key={status} value={status}>
                {status === 'ALL' ? 'All Status' : status.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card overflow-hidden"
      >
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Customer
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Restaurant
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Total
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Payment
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredOrders.map((order) => (
                <tr key={order.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-primary-600">
                    {order.id}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{order.customer.name}</div>
                    <div className="text-sm text-gray-500">{order.customer.email}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {order.restaurant}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    ${order.total.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(order.status)}`}>
                      {order.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`text-sm font-medium ${getPaymentStatusColor(order.paymentStatus)}`}>
                      {order.paymentStatus}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {format(new Date(order.createdAt), 'MMM d, h:mm a')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <button
                      onClick={() => setSelectedOrder(order)}
                      className="p-2 text-gray-400 hover:text-primary-600 transition-colors"
                    >
                      <EyeIcon className="h-5 w-5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="px-6 py-4 border-t flex items-center justify-between">
          <p className="text-sm text-gray-500">
            Showing {filteredOrders.length} of {mockOrders.length} orders
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="p-2 rounded-lg border hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeftIcon className="h-5 w-5" />
            </button>
            <span className="px-4 py-2 text-sm">Page {currentPage}</span>
            <button
              onClick={() => setCurrentPage((p) => p + 1)}
              disabled={filteredOrders.length < itemsPerPage}
              className="p-2 rounded-lg border hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronRightIcon className="h-5 w-5" />
            </button>
          </div>
        </div>
      </motion.div>

      {/* Order Detail Modal */}
      <Modal
        isOpen={!!selectedOrder}
        onClose={() => setSelectedOrder(null)}
        title={`Order ${selectedOrder?.id}`}
        size="lg"
      >
        {selectedOrder && (
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-2 gap-6">
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-2">Customer Information</h4>
                <p className="font-medium">{selectedOrder.customer.name}</p>
                <p className="text-sm text-gray-600">{selectedOrder.customer.email}</p>
                <p className="text-sm text-gray-600">{selectedOrder.customer.phone}</p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-2">Delivery Address</h4>
                <p className="text-sm text-gray-600">{selectedOrder.address}</p>
              </div>
            </div>

            <div>
              <h4 className="text-sm font-medium text-gray-500 mb-2">Order Items</h4>
              <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                {selectedOrder.items.map((item, index) => (
                  <div key={index} className="flex justify-between">
                    <span className="text-gray-600">
                      {item.qty}x {item.name}
                    </span>
                    <span className="font-medium">${(item.qty * item.price).toFixed(2)}</span>
                  </div>
                ))}
                <div className="border-t pt-2 mt-2 flex justify-between font-medium">
                  <span>Total</span>
                  <span>${selectedOrder.total.toFixed(2)}</span>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Order Status</h4>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(selectedOrder.status)}`}>
                  {selectedOrder.status.replace('_', ' ')}
                </span>
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Payment Status</h4>
                <span className={`font-medium ${getPaymentStatusColor(selectedOrder.paymentStatus)}`}>
                  {selectedOrder.paymentStatus}
                </span>
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Order Time</h4>
                <span className="text-gray-600">
                  {format(new Date(selectedOrder.createdAt), 'MMM d, yyyy h:mm a')}
                </span>
              </div>
            </div>

            <div className="flex gap-3 pt-4 border-t">
              <select className="input flex-1">
                <option value="">Update Status</option>
                {statusOptions.filter((s) => s !== 'ALL').map((status) => (
                  <option key={status} value={status}>{status.replace('_', ' ')}</option>
                ))}
              </select>
              <button className="btn-primary">Update</button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
