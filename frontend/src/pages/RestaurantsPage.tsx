import { useState } from 'react'
import { motion } from 'framer-motion'
import {
  MagnifyingGlassIcon,
  AdjustmentsHorizontalIcon,
  XMarkIcon,
} from '@heroicons/react/24/outline'
import RestaurantCard from '../components/common/RestaurantCard'
import { PageLoader } from '../components/common/LoadingSpinner'

// Mock data
const allRestaurants = [
  {
    id: '1',
    name: 'Pizza Palace',
    description: 'Authentic Italian pizzas',
    image: 'https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=500',
    cuisine: ['Italian', 'Pizza'],
    rating: 4.8,
    reviewCount: 243,
    deliveryTime: '25-35 min',
    deliveryFee: 2.99,
    minOrder: 15,
    isOpen: true,
    address: { id: '1', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '2',
    name: 'Burger Barn',
    description: 'Gourmet burgers & fries',
    image: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500',
    cuisine: ['American', 'Burgers'],
    rating: 4.6,
    reviewCount: 189,
    deliveryTime: '20-30 min',
    deliveryFee: 0,
    minOrder: 12,
    isOpen: true,
    address: { id: '2', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '3',
    name: 'Sushi Station',
    description: 'Fresh Japanese cuisine',
    image: 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=500',
    cuisine: ['Japanese', 'Sushi'],
    rating: 4.9,
    reviewCount: 312,
    deliveryTime: '30-40 min',
    deliveryFee: 3.99,
    minOrder: 20,
    isOpen: true,
    address: { id: '3', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '4',
    name: 'Taco Fiesta',
    description: 'Authentic Mexican flavors',
    image: 'https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=500',
    cuisine: ['Mexican', 'Tacos'],
    rating: 4.5,
    reviewCount: 156,
    deliveryTime: '20-25 min',
    deliveryFee: 1.99,
    minOrder: 10,
    isOpen: true,
    address: { id: '4', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '5',
    name: 'Curry House',
    description: 'Authentic Indian cuisine',
    image: 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=500',
    cuisine: ['Indian', 'Curry'],
    rating: 4.7,
    reviewCount: 198,
    deliveryTime: '35-45 min',
    deliveryFee: 2.49,
    minOrder: 18,
    isOpen: true,
    address: { id: '5', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '6',
    name: 'Noodle House',
    description: 'Asian noodles & rice bowls',
    image: 'https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500',
    cuisine: ['Chinese', 'Asian'],
    rating: 4.4,
    reviewCount: 134,
    deliveryTime: '25-35 min',
    deliveryFee: 1.99,
    minOrder: 12,
    isOpen: true,
    address: { id: '6', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '7',
    name: 'Mediterranean Grill',
    description: 'Fresh Mediterranean dishes',
    image: 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500',
    cuisine: ['Mediterranean', 'Greek'],
    rating: 4.6,
    reviewCount: 167,
    deliveryTime: '30-40 min',
    deliveryFee: 2.99,
    minOrder: 15,
    isOpen: false,
    address: { id: '7', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
  {
    id: '8',
    name: 'Sweet Treats',
    description: 'Desserts & ice cream',
    image: 'https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500',
    cuisine: ['Desserts', 'Ice Cream'],
    rating: 4.8,
    reviewCount: 221,
    deliveryTime: '15-25 min',
    deliveryFee: 1.49,
    minOrder: 8,
    isOpen: true,
    address: { id: '8', label: '', street: '', city: '', state: '', zipCode: '', country: '', isDefault: false },
    openingHours: [],
  },
]

const cuisineFilters = ['All', 'Pizza', 'Burgers', 'Sushi', 'Mexican', 'Indian', 'Chinese', 'Desserts']
const sortOptions = [
  { value: 'recommended', label: 'Recommended' },
  { value: 'rating', label: 'Rating' },
  { value: 'delivery_time', label: 'Delivery Time' },
  { value: 'delivery_fee', label: 'Delivery Fee' },
]

export default function RestaurantsPage() {
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCuisine, setSelectedCuisine] = useState('All')
  const [sortBy, setSortBy] = useState('recommended')
  const [showOpenOnly, setShowOpenOnly] = useState(false)
  const [showFilters, setShowFilters] = useState(false)
  const [isLoading] = useState(false)

  // Filter and sort restaurants
  const filteredRestaurants = allRestaurants
    .filter((restaurant) => {
      const matchesSearch = restaurant.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        restaurant.cuisine.some(c => c.toLowerCase().includes(searchQuery.toLowerCase()))
      const matchesCuisine = selectedCuisine === 'All' ||
        restaurant.cuisine.some(c => c.toLowerCase() === selectedCuisine.toLowerCase())
      const matchesOpen = !showOpenOnly || restaurant.isOpen
      return matchesSearch && matchesCuisine && matchesOpen
    })
    .sort((a, b) => {
      switch (sortBy) {
        case 'rating':
          return b.rating - a.rating
        case 'delivery_time':
          return parseInt(a.deliveryTime) - parseInt(b.deliveryTime)
        case 'delivery_fee':
          return a.deliveryFee - b.deliveryFee
        default:
          return b.rating * b.reviewCount - a.rating * a.reviewCount
      }
    })

  if (isLoading) {
    return <PageLoader />
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Restaurants</h1>
        <p className="text-gray-500">Discover the best restaurants in your area</p>
      </div>

      {/* Search and Filters */}
      <div className="mb-8 space-y-4">
        {/* Search Bar */}
        <div className="flex gap-4">
          <div className="relative flex-1">
            <MagnifyingGlassIcon className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search restaurants or cuisines..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="input pl-12"
            />
          </div>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`btn-outline flex items-center gap-2 ${showFilters ? 'bg-primary-50 border-primary-500' : ''}`}
          >
            <AdjustmentsHorizontalIcon className="h-5 w-5" />
            Filters
          </button>
        </div>

        {/* Cuisine Tabs */}
        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
          {cuisineFilters.map((cuisine) => (
            <button
              key={cuisine}
              onClick={() => setSelectedCuisine(cuisine)}
              className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
                selectedCuisine === cuisine
                  ? 'bg-primary-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {cuisine}
            </button>
          ))}
        </div>

        {/* Expanded Filters */}
        {showFilters && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="bg-gray-50 rounded-xl p-4 flex flex-wrap items-center gap-4"
          >
            <div className="flex items-center gap-2">
              <label className="text-sm text-gray-600">Sort by:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="input py-2 w-40"
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={showOpenOnly}
                onChange={(e) => setShowOpenOnly(e.target.checked)}
                className="rounded text-primary-600 focus:ring-primary-500"
              />
              <span className="text-sm text-gray-600">Open now only</span>
            </label>
            <button
              onClick={() => {
                setSearchQuery('')
                setSelectedCuisine('All')
                setSortBy('recommended')
                setShowOpenOnly(false)
              }}
              className="text-sm text-primary-600 hover:text-primary-700 flex items-center gap-1"
            >
              <XMarkIcon className="h-4 w-4" />
              Clear all
            </button>
          </motion.div>
        )}
      </div>

      {/* Results count */}
      <p className="text-sm text-gray-500 mb-6">
        {filteredRestaurants.length} restaurants found
      </p>

      {/* Restaurant Grid */}
      {filteredRestaurants.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredRestaurants.map((restaurant, index) => (
            <motion.div
              key={restaurant.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
            >
              <RestaurantCard restaurant={restaurant} />
            </motion.div>
          ))}
        </div>
      ) : (
        <div className="text-center py-16">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <MagnifyingGlassIcon className="h-8 w-8 text-gray-400" />
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No restaurants found</h3>
          <p className="text-gray-500 mb-4">Try adjusting your search or filters</p>
          <button
            onClick={() => {
              setSearchQuery('')
              setSelectedCuisine('All')
              setShowOpenOnly(false)
            }}
            className="btn-primary"
          >
            Clear filters
          </button>
        </div>
      )}
    </div>
  )
}
