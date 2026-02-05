import { useState } from 'react'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import {
  MagnifyingGlassIcon,
  ArrowDownTrayIcon,
  EyeIcon,
  ArrowPathIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
} from '@heroicons/react/24/outline'
import Modal from '../../components/common/Modal'
import toast from 'react-hot-toast'

const mockPayments = [
  {
    id: 'PAY-001',
    orderId: 'ORD-20260205-001',
    customer: { name: 'John Doe', email: 'john@example.com' },
    amount: 35.46,
    method: 'CARD',
    cardLast4: '4242',
    status: 'COMPLETED',
    createdAt: '2026-02-05T14:30:00Z',
    transactionId: 'txn_1abc123',
  },
  {
    id: 'PAY-002',
    orderId: 'ORD-20260205-002',
    customer: { name: 'Jane Smith', email: 'jane@example.com' },
    amount: 30.02,
    method: 'CARD',
    cardLast4: '1234',
    status: 'COMPLETED',
    createdAt: '2026-02-05T15:45:00Z',
    transactionId: 'txn_2def456',
  },
  {
    id: 'PAY-003',
    orderId: 'ORD-20260205-003',
    customer: { name: 'Mike Johnson', email: 'mike@example.com' },
    amount: 53.65,
    method: 'CASH',
    cardLast4: null,
    status: 'PENDING',
    createdAt: '2026-02-05T16:00:00Z',
    transactionId: null,
  },
  {
    id: 'PAY-004',
    orderId: 'ORD-20260205-004',
    customer: { name: 'Sarah Wilson', email: 'sarah@example.com' },
    amount: 24.83,
    method: 'CARD',
    cardLast4: '5678',
    status: 'FAILED',
    createdAt: '2026-02-05T16:15:00Z',
    transactionId: null,
  },
  {
    id: 'PAY-005',
    orderId: 'ORD-20260205-005',
    customer: { name: 'Chris Brown', email: 'chris@example.com' },
    amount: 47.02,
    method: 'CARD',
    cardLast4: '9012',
    status: 'REFUNDED',
    createdAt: '2026-02-05T12:00:00Z',
    transactionId: 'txn_3ghi789',
  },
]

const statusOptions = ['ALL', 'COMPLETED', 'PENDING', 'FAILED', 'REFUNDED']

const getStatusColor = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'bg-green-100 text-green-700'
    case 'PENDING':
      return 'bg-yellow-100 text-yellow-700'
    case 'FAILED':
      return 'bg-red-100 text-red-700'
    case 'REFUNDED':
      return 'bg-blue-100 text-blue-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

const getMethodIcon = (method: string) => {
  switch (method) {
    case 'CARD':
      return '💳'
    case 'CASH':
      return '💵'
    default:
      return '💰'
  }
}

export default function AdminPayments() {
  const [payments] = useState(mockPayments)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [selectedPayment, setSelectedPayment] = useState<typeof mockPayments[0] | null>(null)

  const filteredPayments = payments.filter((payment) => {
    const matchesSearch =
      payment.id.toLowerCase().includes(search.toLowerCase()) ||
      payment.orderId.toLowerCase().includes(search.toLowerCase()) ||
      payment.customer.name.toLowerCase().includes(search.toLowerCase())
    const matchesStatus = statusFilter === 'ALL' || payment.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const totalRevenue = payments
    .filter((p) => p.status === 'COMPLETED')
    .reduce((sum, p) => sum + p.amount, 0)

  const handleRefund = (paymentId: string) => {
    if (confirm('Are you sure you want to refund this payment?')) {
      toast.success('Refund initiated successfully!')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Payments</h1>
        <button className="btn-outline flex items-center gap-2">
          <ArrowDownTrayIcon className="h-5 w-5" />
          Export CSV
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {[
          { label: 'Total Revenue', value: `$${totalRevenue.toFixed(2)}`, color: 'text-green-600' },
          { label: 'Completed', value: payments.filter((p) => p.status === 'COMPLETED').length, color: 'text-green-600' },
          { label: 'Pending', value: payments.filter((p) => p.status === 'PENDING').length, color: 'text-yellow-600' },
          { label: 'Refunded', value: payments.filter((p) => p.status === 'REFUNDED').length, color: 'text-blue-600' },
        ].map((stat) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card p-4"
          >
            <p className={`text-2xl font-bold ${stat.color}`}>{stat.value}</p>
            <p className="text-sm text-gray-500">{stat.label}</p>
          </motion.div>
        ))}
      </div>

      {/* Filters */}
      <div className="card p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="relative flex-1">
            <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              className="input pl-10"
              placeholder="Search by payment ID, order ID, or customer..."
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
                {status === 'ALL' ? 'All Status' : status}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Payments Table */}
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
                  Payment ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Order
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Customer
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Method
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
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
              {filteredPayments.map((payment) => (
                <tr key={payment.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {payment.id}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-primary-600 font-medium">
                    {payment.orderId}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{payment.customer.name}</div>
                    <div className="text-sm text-gray-500">{payment.customer.email}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <span>{getMethodIcon(payment.method)}</span>
                      <span className="text-sm text-gray-900">
                        {payment.method}
                        {payment.cardLast4 && ` •••• ${payment.cardLast4}`}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    ${payment.amount.toFixed(2)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(payment.status)}`}>
                      {payment.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {format(new Date(payment.createdAt), 'MMM d, h:mm a')}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setSelectedPayment(payment)}
                        className="p-2 text-gray-400 hover:text-primary-600 transition-colors"
                        title="View Details"
                      >
                        <EyeIcon className="h-5 w-5" />
                      </button>
                      {payment.status === 'COMPLETED' && (
                        <button
                          onClick={() => handleRefund(payment.id)}
                          className="p-2 text-gray-400 hover:text-red-600 transition-colors"
                          title="Refund"
                        >
                          <ArrowPathIcon className="h-5 w-5" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="px-6 py-4 border-t flex items-center justify-between">
          <p className="text-sm text-gray-500">
            Showing {filteredPayments.length} of {payments.length} payments
          </p>
          <div className="flex items-center gap-2">
            <button className="p-2 rounded-lg border hover:bg-gray-50 disabled:opacity-50" disabled>
              <ChevronLeftIcon className="h-5 w-5" />
            </button>
            <span className="px-4 py-2 text-sm">Page 1</span>
            <button className="p-2 rounded-lg border hover:bg-gray-50 disabled:opacity-50" disabled>
              <ChevronRightIcon className="h-5 w-5" />
            </button>
          </div>
        </div>
      </motion.div>

      {/* Payment Detail Modal */}
      <Modal
        isOpen={!!selectedPayment}
        onClose={() => setSelectedPayment(null)}
        title={`Payment ${selectedPayment?.id}`}
        size="md"
      >
        {selectedPayment && (
          <div className="p-6 space-y-6">
            <div className="grid grid-cols-2 gap-6">
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Order ID</h4>
                <p className="font-medium text-primary-600">{selectedPayment.orderId}</p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Transaction ID</h4>
                <p className="font-medium text-gray-900">
                  {selectedPayment.transactionId || 'N/A'}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Customer</h4>
                <p className="font-medium text-gray-900">{selectedPayment.customer.name}</p>
                <p className="text-sm text-gray-500">{selectedPayment.customer.email}</p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Amount</h4>
                <p className="text-2xl font-bold text-gray-900">${selectedPayment.amount.toFixed(2)}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Payment Method</h4>
                <p className="flex items-center gap-2 font-medium text-gray-900">
                  {getMethodIcon(selectedPayment.method)}
                  {selectedPayment.method}
                  {selectedPayment.cardLast4 && ` •••• ${selectedPayment.cardLast4}`}
                </p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-500 mb-1">Status</h4>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(selectedPayment.status)}`}>
                  {selectedPayment.status}
                </span>
              </div>
            </div>

            <div>
              <h4 className="text-sm font-medium text-gray-500 mb-1">Date & Time</h4>
              <p className="text-gray-900">
                {format(new Date(selectedPayment.createdAt), 'MMMM d, yyyy \'at\' h:mm a')}
              </p>
            </div>

            {selectedPayment.status === 'COMPLETED' && (
              <div className="pt-4 border-t">
                <button
                  onClick={() => {
                    handleRefund(selectedPayment.id)
                    setSelectedPayment(null)
                  }}
                  className="btn-danger w-full"
                >
                  Process Refund
                </button>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}
