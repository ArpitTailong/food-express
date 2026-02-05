import { motion } from 'framer-motion'
import {
  CheckCircleIcon,
  ClockIcon,
  FireIcon,
  TruckIcon,
  HomeIcon,
  XCircleIcon,
} from '@heroicons/react/24/solid'
import type { OrderStatus } from '../../types'

interface OrderStatusBadgeProps {
  status: OrderStatus
  size?: 'sm' | 'md' | 'lg'
}

const statusConfig: Record<OrderStatus, {
  label: string
  color: string
  bgColor: string
  icon: React.ComponentType<{ className?: string }>
}> = {
  PENDING: {
    label: 'Pending',
    color: 'text-yellow-700',
    bgColor: 'bg-yellow-100',
    icon: ClockIcon,
  },
  CONFIRMED: {
    label: 'Confirmed',
    color: 'text-blue-700',
    bgColor: 'bg-blue-100',
    icon: CheckCircleIcon,
  },
  PREPARING: {
    label: 'Preparing',
    color: 'text-orange-700',
    bgColor: 'bg-orange-100',
    icon: FireIcon,
  },
  READY_FOR_PICKUP: {
    label: 'Ready',
    color: 'text-purple-700',
    bgColor: 'bg-purple-100',
    icon: CheckCircleIcon,
  },
  PICKED_UP: {
    label: 'Picked Up',
    color: 'text-indigo-700',
    bgColor: 'bg-indigo-100',
    icon: TruckIcon,
  },
  OUT_FOR_DELIVERY: {
    label: 'On the way',
    color: 'text-primary-700',
    bgColor: 'bg-primary-100',
    icon: TruckIcon,
  },
  DELIVERED: {
    label: 'Delivered',
    color: 'text-green-700',
    bgColor: 'bg-green-100',
    icon: HomeIcon,
  },
  CANCELLED: {
    label: 'Cancelled',
    color: 'text-red-700',
    bgColor: 'bg-red-100',
    icon: XCircleIcon,
  },
}

export default function OrderStatusBadge({ status, size = 'md' }: OrderStatusBadgeProps) {
  const config = statusConfig[status]
  const Icon = config.icon

  const sizeClasses = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-sm',
    lg: 'px-3 py-1.5 text-base',
  }

  const iconSizes = {
    sm: 'h-3 w-3',
    md: 'h-4 w-4',
    lg: 'h-5 w-5',
  }

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full ${config.bgColor} ${config.color} ${sizeClasses[size]}`}
    >
      <Icon className={iconSizes[size]} />
      {config.label}
    </span>
  )
}

interface OrderProgressProps {
  status: OrderStatus
}

const progressSteps = [
  { status: 'CONFIRMED', label: 'Confirmed' },
  { status: 'PREPARING', label: 'Preparing' },
  { status: 'READY_FOR_PICKUP', label: 'Ready' },
  { status: 'OUT_FOR_DELIVERY', label: 'On the way' },
  { status: 'DELIVERED', label: 'Delivered' },
]

const statusOrder: Record<OrderStatus, number> = {
  PENDING: 0,
  CONFIRMED: 1,
  PREPARING: 2,
  READY_FOR_PICKUP: 3,
  PICKED_UP: 3.5,
  OUT_FOR_DELIVERY: 4,
  DELIVERED: 5,
  CANCELLED: -1,
}

export function OrderProgress({ status }: OrderProgressProps) {
  const currentStep = statusOrder[status]

  if (status === 'CANCELLED') {
    return (
      <div className="text-center py-8">
        <XCircleIcon className="h-12 w-12 text-red-500 mx-auto mb-2" />
        <p className="text-red-600 font-medium">Order Cancelled</p>
      </div>
    )
  }

  if (status === 'PENDING') {
    return (
      <div className="text-center py-8">
        <ClockIcon className="h-12 w-12 text-yellow-500 mx-auto mb-2" />
        <p className="text-yellow-600 font-medium">Waiting for confirmation...</p>
      </div>
    )
  }

  return (
    <div className="py-4">
      <div className="flex items-center justify-between">
        {progressSteps.map((step, index) => {
          const stepNumber = statusOrder[step.status as OrderStatus]
          const isCompleted = currentStep >= stepNumber
          const isCurrent = Math.floor(currentStep) === index + 1

          return (
            <div key={step.status} className="flex flex-col items-center flex-1">
              {/* Line before */}
              {index > 0 && (
                <div
                  className={`absolute h-1 w-full -translate-x-1/2 ${
                    currentStep > stepNumber - 1 ? 'bg-primary-500' : 'bg-gray-200'
                  }`}
                  style={{ width: 'calc(100% - 2rem)' }}
                />
              )}
              
              {/* Circle */}
              <motion.div
                initial={false}
                animate={{
                  scale: isCurrent ? 1.1 : 1,
                  backgroundColor: isCompleted ? '#f97316' : '#e5e7eb',
                }}
                className={`relative z-10 w-8 h-8 rounded-full flex items-center justify-center ${
                  isCompleted ? 'text-white' : 'text-gray-400'
                }`}
              >
                {isCompleted ? (
                  <CheckCircleIcon className="h-5 w-5" />
                ) : (
                  <span className="text-sm font-medium">{index + 1}</span>
                )}
              </motion.div>

              {/* Label */}
              <span
                className={`mt-2 text-xs text-center ${
                  isCompleted ? 'text-primary-600 font-medium' : 'text-gray-400'
                }`}
              >
                {step.label}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
