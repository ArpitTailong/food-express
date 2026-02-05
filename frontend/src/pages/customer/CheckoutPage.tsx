import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  MapPinIcon,
  CreditCardIcon,
  BanknotesIcon,
  CheckCircleIcon,
  PlusIcon,
} from '@heroicons/react/24/outline'
import toast from 'react-hot-toast'
import { ButtonLoader } from '../../components/common/LoadingSpinner'
import { useCartStore } from '../../store/cartStore'
import { useAuthStore } from '../../store/authStore'
import { orderService } from '../../services/orderService'
import { paymentService } from '../../services/paymentService'
import type { Address } from '../../types'

const savedAddresses: Address[] = [
  {
    id: '1',
    label: 'Home',
    street: '123 Main Street',
    city: 'New York',
    state: 'NY',
    zipCode: '10001',
    country: 'USA',
    isDefault: true,
  },
  {
    id: '2',
    label: 'Work',
    street: '456 Business Ave',
    city: 'New York',
    state: 'NY',
    zipCode: '10002',
    country: 'USA',
    isDefault: false,
  },
]

const paymentMethods = [
  { id: 'card', name: 'Credit/Debit Card', icon: CreditCardIcon },
  { id: 'cash', name: 'Cash on Delivery', icon: BanknotesIcon },
]

export default function CheckoutPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { items, restaurantId, restaurantName, subtotal, deliveryFee, tax, total, clearCart } = useCartStore()
  
  const [selectedAddress, setSelectedAddress] = useState(savedAddresses.find(a => a.isDefault)?.id || '')
  const [selectedPayment, setSelectedPayment] = useState('card')
  const [tip, setTip] = useState(0)
  const [specialInstructions, setSpecialInstructions] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const tipOptions = [0, 2, 5, 10]
  const finalTotal = total + tip

  const handlePlaceOrder = async () => {
    if (!selectedAddress) {
      toast.error('Please select a delivery address')
      return
    }

    if (!user) {
      toast.error('Please log in to place an order')
      navigate('/login')
      return
    }

    setIsLoading(true)

    try {
      const address = savedAddresses.find(a => a.id === selectedAddress)!
      
      // Create order
      const orderResponse = await orderService.createOrder({
        restaurantId: restaurantId!,
        restaurantName: restaurantName!,
        customerId: user.id,
        customerName: `${user.firstName} ${user.lastName}`,
        customerPhone: user.phoneNumber || '',
        deliveryAddress: {
          street: address.street,
          city: address.city,
          state: address.state,
          zipCode: address.zipCode,
          country: address.country,
        },
        items: items.map(item => ({
          menuItemId: item.menuItem.id,
          name: item.menuItem.name,
          quantity: item.quantity,
          unitPrice: item.menuItem.price,
          specialInstructions: item.specialInstructions,
        })),
        specialInstructions,
        paymentMethod: selectedPayment.toUpperCase(),
        tip: tip > 0 ? tip : undefined,
      })

      if (orderResponse.success && selectedPayment === 'card') {
        // Process payment
        await paymentService.createPayment({
          orderId: orderResponse.data.id,
          customerId: user.id,
          amount: finalTotal,
          currency: 'USD',
          paymentMethod: 'CARD',
          gatewayToken: 'tok_success', // Mock token
        })
      }

      clearCart()
      toast.success('Order placed successfully!')
      navigate(`/orders/${orderResponse.data.id}/track`)
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to place order')
    } finally {
      setIsLoading(false)
    }
  }

  if (items.length === 0) {
    navigate('/cart')
    return null
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">Checkout</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column - Forms */}
        <div className="lg:col-span-2 space-y-6">
          {/* Delivery Address */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="card p-6"
          >
            <div className="flex items-center gap-2 mb-4">
              <MapPinIcon className="h-5 w-5 text-primary-600" />
              <h2 className="font-semibold text-gray-900">Delivery Address</h2>
            </div>

            <div className="space-y-3">
              {savedAddresses.map((address) => (
                <label
                  key={address.id}
                  className={`block p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                    selectedAddress === address.id
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <input
                      type="radio"
                      name="address"
                      value={address.id}
                      checked={selectedAddress === address.id}
                      onChange={(e) => setSelectedAddress(e.target.value)}
                      className="mt-1 text-primary-600 focus:ring-primary-500"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{address.label}</span>
                        {address.isDefault && (
                          <span className="badge-secondary text-xs">Default</span>
                        )}
                      </div>
                      <p className="text-sm text-gray-500 mt-1">
                        {address.street}, {address.city}, {address.state} {address.zipCode}
                      </p>
                    </div>
                    {selectedAddress === address.id && (
                      <CheckCircleIcon className="h-5 w-5 text-primary-600" />
                    )}
                  </div>
                </label>
              ))}

              <button className="flex items-center gap-2 text-primary-600 hover:text-primary-700 font-medium text-sm">
                <PlusIcon className="h-4 w-4" />
                Add new address
              </button>
            </div>
          </motion.div>

          {/* Payment Method */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="card p-6"
          >
            <div className="flex items-center gap-2 mb-4">
              <CreditCardIcon className="h-5 w-5 text-primary-600" />
              <h2 className="font-semibold text-gray-900">Payment Method</h2>
            </div>

            <div className="space-y-3">
              {paymentMethods.map((method) => (
                <label
                  key={method.id}
                  className={`flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-colors ${
                    selectedPayment === method.id
                      ? 'border-primary-500 bg-primary-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment"
                    value={method.id}
                    checked={selectedPayment === method.id}
                    onChange={(e) => setSelectedPayment(e.target.value)}
                    className="text-primary-600 focus:ring-primary-500"
                  />
                  <method.icon className="h-5 w-5 text-gray-400" />
                  <span className="font-medium">{method.name}</span>
                  {selectedPayment === method.id && (
                    <CheckCircleIcon className="h-5 w-5 text-primary-600 ml-auto" />
                  )}
                </label>
              ))}
            </div>

            {selectedPayment === 'card' && (
              <div className="mt-4 p-4 bg-gray-50 rounded-xl">
                <p className="text-sm text-gray-500">
                  Your saved cards will appear here. For this demo, payment will be processed automatically.
                </p>
              </div>
            )}
          </motion.div>

          {/* Tip & Instructions */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="card p-6"
          >
            <h2 className="font-semibold text-gray-900 mb-4">Add a tip for your driver</h2>
            <div className="flex gap-2 mb-6">
              {tipOptions.map((amount) => (
                <button
                  key={amount}
                  onClick={() => setTip(amount)}
                  className={`flex-1 py-2 rounded-lg font-medium transition-colors ${
                    tip === amount
                      ? 'bg-primary-600 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {amount === 0 ? 'No tip' : `$${amount}`}
                </button>
              ))}
            </div>

            <div>
              <label className="label">Special instructions (optional)</label>
              <textarea
                value={specialInstructions}
                onChange={(e) => setSpecialInstructions(e.target.value)}
                rows={3}
                className="input"
                placeholder="E.g., Ring the doorbell twice, leave at door..."
              />
            </div>
          </motion.div>
        </div>

        {/* Right Column - Order Summary */}
        <div className="lg:col-span-1">
          <div className="card p-6 sticky top-24">
            <h2 className="font-semibold text-gray-900 mb-4">Order Summary</h2>

            {/* Items */}
            <div className="space-y-3 mb-4 max-h-48 overflow-y-auto">
              {items.map((item) => (
                <div key={item.id} className="flex justify-between text-sm">
                  <span className="text-gray-600">
                    {item.quantity}x {item.menuItem.name}
                  </span>
                  <span className="text-gray-900">${item.totalPrice.toFixed(2)}</span>
                </div>
              ))}
            </div>

            <div className="border-t pt-4 space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Subtotal</span>
                <span className="text-gray-900">${subtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Delivery fee</span>
                <span className="text-gray-900">
                  {deliveryFee === 0 ? 'Free' : `$${deliveryFee.toFixed(2)}`}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Tax</span>
                <span className="text-gray-900">${tax.toFixed(2)}</span>
              </div>
              {tip > 0 && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Driver tip</span>
                  <span className="text-gray-900">${tip.toFixed(2)}</span>
                </div>
              )}
              <div className="border-t pt-3 flex justify-between font-semibold text-base">
                <span>Total</span>
                <span>${finalTotal.toFixed(2)}</span>
              </div>
            </div>

            <button
              onClick={handlePlaceOrder}
              disabled={isLoading}
              className="btn-primary w-full btn-lg mt-6"
            >
              {isLoading ? <ButtonLoader /> : `Place Order • $${finalTotal.toFixed(2)}`}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
