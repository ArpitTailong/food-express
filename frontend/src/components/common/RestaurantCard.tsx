import { motion } from 'framer-motion'
import { StarIcon } from '@heroicons/react/24/solid'
import { ClockIcon } from '@heroicons/react/24/outline'
import { Link } from 'react-router-dom'
import type { Restaurant } from '../../types'

interface RestaurantCardProps {
  restaurant: Restaurant
}

export default function RestaurantCard({ restaurant }: RestaurantCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ y: -4 }}
      transition={{ duration: 0.2 }}
    >
      <Link to={`/restaurants/${restaurant.id}`} className="block">
        <div className="card-hover">
          {/* Image */}
          <div className="relative aspect-[16/10] overflow-hidden">
            <img
              src={restaurant.image}
              alt={restaurant.name}
              className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
            {!restaurant.isOpen && (
              <div className="absolute inset-0 bg-black/60 flex items-center justify-center">
                <span className="text-white font-semibold">Currently Closed</span>
              </div>
            )}
            {/* Delivery fee badge */}
            <div className="absolute top-3 left-3">
              <span className="bg-white/95 backdrop-blur-sm text-gray-900 text-xs font-medium px-2 py-1 rounded-full shadow-sm">
                {restaurant.deliveryFee === 0 ? 'Free delivery' : `$${restaurant.deliveryFee.toFixed(2)} delivery`}
              </span>
            </div>
          </div>

          {/* Content */}
          <div className="p-4">
            <div className="flex items-start justify-between gap-2 mb-2">
              <h3 className="font-semibold text-gray-900 truncate">{restaurant.name}</h3>
              <div className="flex items-center gap-1 shrink-0">
                <StarIcon className="h-4 w-4 text-yellow-400" />
                <span className="text-sm font-medium text-gray-700">{restaurant.rating.toFixed(1)}</span>
                <span className="text-sm text-gray-400">({restaurant.reviewCount})</span>
              </div>
            </div>

            <p className="text-sm text-gray-500 mb-3 line-clamp-1">
              {restaurant.cuisine.join(' • ')}
            </p>

            <div className="flex items-center gap-4 text-sm text-gray-500">
              <div className="flex items-center gap-1">
                <ClockIcon className="h-4 w-4" />
                <span>{restaurant.deliveryTime}</span>
              </div>
              <span>•</span>
              <span>Min ${restaurant.minOrder.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </Link>
    </motion.div>
  )
}
