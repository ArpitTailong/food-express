import { motion } from 'framer-motion'
import { PlusIcon } from '@heroicons/react/24/outline'
import type { MenuItem } from '../../types'

interface MenuItemCardProps {
  item: MenuItem
  onAddToCart: (item: MenuItem) => void
}

export default function MenuItemCard({ item, onAddToCart }: MenuItemCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      className="card p-4 flex gap-4"
    >
      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="font-medium text-gray-900">{item.name}</h3>
            {item.isPopular && (
              <span className="badge-primary mt-1">Popular</span>
            )}
          </div>
          <span className="font-semibold text-gray-900">${item.price.toFixed(2)}</span>
        </div>
        <p className="text-sm text-gray-500 mt-2 line-clamp-2">{item.description}</p>
        {item.calories && (
          <p className="text-xs text-gray-400 mt-2">{item.calories} cal</p>
        )}
      </div>

      {/* Image and Add button */}
      <div className="relative shrink-0">
        {item.image ? (
          <img
            src={item.image}
            alt={item.name}
            className="w-24 h-24 object-cover rounded-lg"
          />
        ) : (
          <div className="w-24 h-24 bg-gray-100 rounded-lg flex items-center justify-center">
            <span className="text-2xl">🍽️</span>
          </div>
        )}
        <button
          onClick={() => onAddToCart(item)}
          disabled={!item.isAvailable}
          className={`absolute -bottom-2 -right-2 w-8 h-8 rounded-full flex items-center justify-center shadow-lg transition-colors ${
            item.isAvailable
              ? 'bg-primary-600 text-white hover:bg-primary-700'
              : 'bg-gray-300 text-gray-500 cursor-not-allowed'
          }`}
        >
          <PlusIcon className="h-5 w-5" />
        </button>
      </div>
    </motion.div>
  )
}
