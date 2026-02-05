import { useState } from 'react'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import {
  BellIcon,
  CheckIcon,
  ShoppingBagIcon,
  CreditCardIcon,
  MegaphoneIcon,
} from '@heroicons/react/24/outline'
import toast from 'react-hot-toast'
import { useAuthStore } from '../../store/authStore'
import type { Notification } from '../../types'

// Mock notifications
const mockNotifications: Notification[] = [
  {
    id: '1',
    userId: 'user-001',
    type: 'ORDER_UPDATE',
    title: 'Order Delivered',
    message: 'Your order from Pizza Palace has been delivered. Enjoy your meal!',
    isRead: false,
    createdAt: '2026-02-05T14:30:00Z',
  },
  {
    id: '2',
    userId: 'user-001',
    type: 'PROMOTION',
    title: '20% Off Your Next Order',
    message: 'Use code SAVE20 to get 20% off your next order. Valid until Feb 10!',
    isRead: false,
    createdAt: '2026-02-05T10:00:00Z',
  },
  {
    id: '3',
    userId: 'user-001',
    type: 'PAYMENT',
    title: 'Payment Successful',
    message: 'Your payment of $30.02 for order ORD-20260205-0002 was successful.',
    isRead: true,
    createdAt: '2026-02-04T18:00:00Z',
  },
  {
    id: '4',
    userId: 'user-001',
    type: 'ORDER_UPDATE',
    title: 'Order Confirmed',
    message: 'Burger Barn has confirmed your order and is preparing it now.',
    isRead: true,
    createdAt: '2026-02-04T14:05:00Z',
  },
  {
    id: '5',
    userId: 'user-001',
    type: 'SYSTEM',
    title: 'Welcome to FoodExpress!',
    message: 'Thanks for joining. Start exploring restaurants and get your first meal delivered!',
    isRead: true,
    createdAt: '2026-02-01T09:00:00Z',
  },
]

const getNotificationIcon = (type: Notification['type']) => {
  switch (type) {
    case 'ORDER_UPDATE':
      return { icon: ShoppingBagIcon, color: 'bg-primary-100 text-primary-600' }
    case 'PAYMENT':
      return { icon: CreditCardIcon, color: 'bg-green-100 text-green-600' }
    case 'PROMOTION':
      return { icon: MegaphoneIcon, color: 'bg-purple-100 text-purple-600' }
    default:
      return { icon: BellIcon, color: 'bg-gray-100 text-gray-600' }
  }
}

export default function NotificationsPage() {
  const { user } = useAuthStore()
  const [notifications, setNotifications] = useState<Notification[]>(mockNotifications)

  const unreadCount = notifications.filter((n) => !n.isRead).length

  const markAsRead = (id: string) => {
    setNotifications(notifications.map((n) =>
      n.id === id ? { ...n, isRead: true } : n
    ))
  }

  const markAllAsRead = () => {
    setNotifications(notifications.map((n) => ({ ...n, isRead: true })))
    toast.success('All notifications marked as read')
  }

  const deleteNotification = (id: string) => {
    setNotifications(notifications.filter((n) => n.id !== id))
    toast.success('Notification deleted')
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
          {unreadCount > 0 && (
            <p className="text-gray-500 mt-1">{unreadCount} unread</p>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={markAllAsRead}
            className="text-primary-600 hover:text-primary-700 text-sm font-medium flex items-center gap-1"
          >
            <CheckIcon className="h-4 w-4" />
            Mark all read
          </button>
        )}
      </div>

      <div className="space-y-3">
        {notifications.map((notification, index) => {
          const { icon: Icon, color } = getNotificationIcon(notification.type)
          return (
            <motion.div
              key={notification.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.03 }}
              onClick={() => markAsRead(notification.id)}
              className={`card p-4 cursor-pointer transition-colors ${
                !notification.isRead ? 'bg-primary-50/50 border-primary-200' : ''
              }`}
            >
              <div className="flex gap-4">
                <div className={`p-3 rounded-full ${color} shrink-0`}>
                  <Icon className="h-5 w-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h3 className={`font-medium ${!notification.isRead ? 'text-gray-900' : 'text-gray-700'}`}>
                        {notification.title}
                      </h3>
                      <p className="text-sm text-gray-500 mt-1">{notification.message}</p>
                    </div>
                    {!notification.isRead && (
                      <span className="w-2 h-2 bg-primary-600 rounded-full shrink-0 mt-2" />
                    )}
                  </div>
                  <div className="flex items-center justify-between mt-3">
                    <span className="text-xs text-gray-400">
                      {format(new Date(notification.createdAt), 'MMM d, h:mm a')}
                    </span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        deleteNotification(notification.id)
                      }}
                      className="text-xs text-gray-400 hover:text-red-500"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            </motion.div>
          )
        })}

        {notifications.length === 0 && (
          <div className="text-center py-12">
            <BellIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">No notifications yet</p>
          </div>
        )}
      </div>
    </div>
  )
}
