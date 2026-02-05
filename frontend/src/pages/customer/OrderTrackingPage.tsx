import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import {
  PhoneIcon,
  ChatBubbleLeftIcon,
  MapPinIcon,
  ClockIcon,
  CheckCircleIcon,
} from '@heroicons/react/24/outline'
import { StarIcon } from '@heroicons/react/24/solid'
import toast from 'react-hot-toast'
import OrderStatusBadge, { OrderProgress } from '../../components/common/OrderStatusBadge'
import Modal from '../../components/common/Modal'
import { ButtonLoader } from '../../components/common/LoadingSpinner'
import type { Order } from '../../types'

// Mock order data
const mockOrder: Order = {
  id: '2',
  orderNumber: 'ORD-20260205-0002',
  customerId: 'cust-001',
  customerName: 'John Doe',
  restaurantId: 'rest-002',
  restaurantName: 'Burger Barn',
  status: 'OUT_FOR_DELIVERY',
  items: [
    { id: '1', menuItemId: 'item-1', name: 'Classic Burger', quantity: 1, unitPrice: 12.99, totalPrice: 12.99 },
    { id: '2', menuItemId: 'item-2', name: 'Cheese Fries', quantity: 1, unitPrice: 6.99, totalPrice: 6.99 },
    { id: '3', menuItemId: 'item-3', name: 'Coca-Cola', quantity: 2, unitPrice: 2.49, totalPrice: 4.98 },
  ],
  deliveryAddress: {
    id: '1',
    label: 'Home',
    street: '123 Main Street',
    city: 'New York',
    state: 'NY',
    zipCode: '10001',
    country: 'USA',
    isDefault: true,
  },
  subtotal: 24.96,
  deliveryFee: 0,
  tax: 2.06,
  tip: 3,
  total: 30.02,
  paymentMethod: 'CARD',
  paymentStatus: 'SUCCESS',
  driverId: 'driver-001',
  driverName: 'Mike Johnson',
  driverPhone: '+1 (555) 123-4567',
  estimatedDeliveryTime: '2026-02-05T14:45:00Z',
  createdAt: '2026-02-05T14:00:00Z',
  updatedAt: '2026-02-05T14:30:00Z',
}

export default function OrderTrackingPage() {
  const { id } = useParams()
  const [order] = useState<Order>(mockOrder)
  const [showRatingModal, setShowRatingModal] = useState(false)
  const [rating, setRating] = useState({ food: 0, delivery: 0, comment: '' })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmitRating = async () => {
    setIsSubmitting(true)
    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 1000))
    toast.success('Thanks for your feedback!')
    setShowRatingModal(false)
    setIsSubmitting(false)
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <Link to="/orders" className="text-primary-600 hover:text-primary-700 text-sm mb-2 inline-block">
            ← Back to orders
          </Link>
          <h1 className="text-2xl font-bold text-gray-900">{order.restaurantName}</h1>
          <p className="text-gray-500">{order.orderNumber}</p>
        </div>
        <OrderStatusBadge status={order.status} size="lg" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Progress Tracker */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card p-6"
          >
            <h2 className="font-semibold text-gray-900 mb-4">Order Progress</h2>
            <OrderProgress status={order.status} />

            {order.estimatedDeliveryTime && order.status !== 'DELIVERED' && order.status !== 'CANCELLED' && (
              <div className="mt-6 p-4 bg-primary-50 rounded-xl flex items-center gap-3">
                <ClockIcon className="h-6 w-6 text-primary-600" />
                <div>
                  <p className="text-sm text-primary-700">Estimated arrival</p>
                  <p className="font-semibold text-primary-900">
                    {format(new Date(order.estimatedDeliveryTime), 'h:mm a')}
                  </p>
                </div>
              </div>
            )}
          </motion.div>

          {/* Driver Info */}
          {order.driverName && ['PICKED_UP', 'OUT_FOR_DELIVERY'].includes(order.status) && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="card p-6"
            >
              <h2 className="font-semibold text-gray-900 mb-4">Your Driver</h2>
              <div className="flex items-center gap-4">
                <div className="w-14 h-14 bg-primary-100 rounded-full flex items-center justify-center text-primary-600 text-xl font-semibold">
                  {order.driverName.charAt(0)}
                </div>
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{order.driverName}</p>
                  <p className="text-sm text-gray-500">Your delivery partner</p>
                </div>
                <div className="flex gap-2">
                  <a
                    href={`tel:${order.driverPhone}`}
                    className="p-3 bg-primary-100 text-primary-600 rounded-full hover:bg-primary-200 transition-colors"
                  >
                    <PhoneIcon className="h-5 w-5" />
                  </a>
                  <button className="p-3 bg-gray-100 text-gray-600 rounded-full hover:bg-gray-200 transition-colors">
                    <ChatBubbleLeftIcon className="h-5 w-5" />
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {/* Delivery Address */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="card p-6"
          >
            <div className="flex items-center gap-2 mb-4">
              <MapPinIcon className="h-5 w-5 text-primary-600" />
              <h2 className="font-semibold text-gray-900">Delivery Address</h2>
            </div>
            <p className="text-gray-600">
              {order.deliveryAddress.street}<br />
              {order.deliveryAddress.city}, {order.deliveryAddress.state} {order.deliveryAddress.zipCode}
            </p>
          </motion.div>

          {/* Order Items */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
            className="card p-6"
          >
            <h2 className="font-semibold text-gray-900 mb-4">Order Items</h2>
            <div className="space-y-3">
              {order.items.map((item) => (
                <div key={item.id} className="flex justify-between">
                  <span className="text-gray-600">
                    {item.quantity}x {item.name}
                  </span>
                  <span className="text-gray-900">${item.totalPrice.toFixed(2)}</span>
                </div>
              ))}
            </div>
          </motion.div>

          {/* Rate Order (for delivered orders) */}
          {order.status === 'DELIVERED' && !order.rating && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="card p-6 bg-gradient-to-r from-primary-50 to-secondary-50"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-semibold text-gray-900">How was your order?</h3>
                  <p className="text-sm text-gray-500">Rate your experience</p>
                </div>
                <button
                  onClick={() => setShowRatingModal(true)}
                  className="btn-primary"
                >
                  Rate Order
                </button>
              </div>
            </motion.div>
          )}
        </div>

        {/* Order Summary Sidebar */}
        <div className="lg:col-span-1">
          <div className="card p-6 sticky top-24">
            <h2 className="font-semibold text-gray-900 mb-4">Order Summary</h2>
            
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Subtotal</span>
                <span className="text-gray-900">${order.subtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Delivery fee</span>
                <span className="text-gray-900">
                  {order.deliveryFee === 0 ? 'Free' : `$${order.deliveryFee.toFixed(2)}`}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Tax</span>
                <span className="text-gray-900">${order.tax.toFixed(2)}</span>
              </div>
              {order.tip && order.tip > 0 && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Tip</span>
                  <span className="text-gray-900">${order.tip.toFixed(2)}</span>
                </div>
              )}
              <div className="border-t pt-3 flex justify-between font-semibold">
                <span>Total</span>
                <span>${order.total.toFixed(2)}</span>
              </div>
            </div>

            <div className="mt-4 pt-4 border-t text-sm">
              <div className="flex justify-between text-gray-500">
                <span>Payment</span>
                <span>{order.paymentMethod}</span>
              </div>
              <div className="flex justify-between text-gray-500 mt-1">
                <span>Order placed</span>
                <span>{format(new Date(order.createdAt), 'MMM d, h:mm a')}</span>
              </div>
            </div>

            {/* Help Button */}
            <button className="btn-outline w-full mt-6">
              Need Help?
            </button>
          </div>
        </div>
      </div>

      {/* Rating Modal */}
      <Modal
        isOpen={showRatingModal}
        onClose={() => setShowRatingModal(false)}
        title="Rate Your Order"
        size="md"
      >
        <div className="p-6 space-y-6">
          {/* Food Rating */}
          <div>
            <label className="label">Food Quality</label>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  onClick={() => setRating({ ...rating, food: star })}
                  className="p-1"
                >
                  <StarIcon
                    className={`h-8 w-8 ${
                      star <= rating.food ? 'text-yellow-400' : 'text-gray-200'
                    }`}
                  />
                </button>
              ))}
            </div>
          </div>

          {/* Delivery Rating */}
          <div>
            <label className="label">Delivery Experience</label>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  onClick={() => setRating({ ...rating, delivery: star })}
                  className="p-1"
                >
                  <StarIcon
                    className={`h-8 w-8 ${
                      star <= rating.delivery ? 'text-yellow-400' : 'text-gray-200'
                    }`}
                  />
                </button>
              ))}
            </div>
          </div>

          {/* Comment */}
          <div>
            <label className="label">Comments (optional)</label>
            <textarea
              value={rating.comment}
              onChange={(e) => setRating({ ...rating, comment: e.target.value })}
              rows={3}
              className="input"
              placeholder="Tell us about your experience..."
            />
          </div>

          <button
            onClick={handleSubmitRating}
            disabled={isSubmitting || rating.food === 0 || rating.delivery === 0}
            className="btn-primary w-full"
          >
            {isSubmitting ? <ButtonLoader /> : 'Submit Rating'}
          </button>
        </div>
      </Modal>
    </div>
  )
}
