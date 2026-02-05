import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { StarIcon, ClockIcon, MapPinIcon } from '@heroicons/react/24/solid'
import { HeartIcon, ShareIcon, InformationCircleIcon } from '@heroicons/react/24/outline'
import toast from 'react-hot-toast'
import MenuItemCard from '../components/common/MenuItemCard'
import { useCartStore } from '../store/cartStore'
import type { MenuItem } from '../types'

// Mock restaurant data
const restaurantData = {
  id: '1',
  name: 'Pizza Palace',
  description: 'Authentic Italian pizzas made with fresh ingredients and traditional recipes passed down through generations.',
  image: 'https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=500',
  coverImage: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=1200',
  cuisine: ['Italian', 'Pizza'],
  rating: 4.8,
  reviewCount: 243,
  deliveryTime: '25-35 min',
  deliveryFee: 2.99,
  minOrder: 15,
  isOpen: true,
  address: {
    id: '1',
    label: 'Restaurant',
    street: '123 Main Street',
    city: 'New York',
    state: 'NY',
    zipCode: '10001',
    country: 'USA',
    isDefault: false,
  },
  openingHours: [
    { day: 'Mon-Thu', open: '11:00 AM', close: '10:00 PM' },
    { day: 'Fri-Sat', open: '11:00 AM', close: '11:00 PM' },
    { day: 'Sunday', open: '12:00 PM', close: '9:00 PM' },
  ],
}

// Mock menu data
const menuCategories = [
  { id: 'popular', name: 'Popular' },
  { id: 'pizzas', name: 'Pizzas' },
  { id: 'sides', name: 'Sides' },
  { id: 'drinks', name: 'Drinks' },
  { id: 'desserts', name: 'Desserts' },
]

const menuItems: MenuItem[] = [
  {
    id: '1',
    restaurantId: '1',
    name: 'Margherita Pizza',
    description: 'Classic pizza with fresh mozzarella, tomatoes, and basil',
    price: 14.99,
    image: 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=300',
    category: 'pizzas',
    isAvailable: true,
    isPopular: true,
    calories: 850,
  },
  {
    id: '2',
    restaurantId: '1',
    name: 'Pepperoni Pizza',
    description: 'Loaded with pepperoni slices and mozzarella cheese',
    price: 16.99,
    image: 'https://images.unsplash.com/photo-1628840042765-356cda07504e?w=300',
    category: 'pizzas',
    isAvailable: true,
    isPopular: true,
    calories: 920,
  },
  {
    id: '3',
    restaurantId: '1',
    name: 'BBQ Chicken Pizza',
    description: 'Grilled chicken, BBQ sauce, red onions, and cilantro',
    price: 18.99,
    image: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=300',
    category: 'pizzas',
    isAvailable: true,
    calories: 880,
  },
  {
    id: '4',
    restaurantId: '1',
    name: 'Garlic Bread',
    description: 'Crispy bread with garlic butter and herbs',
    price: 5.99,
    image: 'https://images.unsplash.com/photo-1619535860434-ba1d8fa12536?w=300',
    category: 'sides',
    isAvailable: true,
    calories: 320,
  },
  {
    id: '5',
    restaurantId: '1',
    name: 'Caesar Salad',
    description: 'Fresh romaine lettuce with caesar dressing and croutons',
    price: 8.99,
    category: 'sides',
    isAvailable: true,
    calories: 280,
  },
  {
    id: '6',
    restaurantId: '1',
    name: 'Coca-Cola',
    description: 'Classic Coca-Cola (330ml)',
    price: 2.49,
    category: 'drinks',
    isAvailable: true,
    calories: 140,
  },
  {
    id: '7',
    restaurantId: '1',
    name: 'Tiramisu',
    description: 'Classic Italian dessert with coffee and mascarpone',
    price: 7.99,
    image: 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=300',
    category: 'desserts',
    isAvailable: true,
    isPopular: true,
    calories: 450,
  },
  {
    id: '8',
    restaurantId: '1',
    name: 'Vegetarian Pizza',
    description: 'Bell peppers, mushrooms, olives, onions, and tomatoes',
    price: 15.99,
    category: 'pizzas',
    isAvailable: false,
    calories: 780,
  },
]

export default function RestaurantDetailPage() {
  const { id } = useParams()
  const [selectedCategory, setSelectedCategory] = useState('popular')
  const { addItem, setRestaurant } = useCartStore()

  const restaurant = restaurantData // In real app, fetch by id

  const filteredItems = selectedCategory === 'popular'
    ? menuItems.filter(item => item.isPopular)
    : menuItems.filter(item => item.category === selectedCategory)

  const handleAddToCart = (item: MenuItem) => {
    setRestaurant(restaurant.id, restaurant.name)
    addItem(item, 1)
    toast.success(`${item.name} added to cart!`)
  }

  return (
    <div>
      {/* Cover Image */}
      <div className="relative h-64 sm:h-80 bg-gray-200">
        <img
          src={restaurant.coverImage}
          alt={restaurant.name}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
      </div>

      {/* Restaurant Info */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 -mt-24 relative z-10">
        <div className="bg-white rounded-2xl shadow-lg p-6 mb-6">
          <div className="flex flex-col sm:flex-row sm:items-start gap-6">
            {/* Logo */}
            <img
              src={restaurant.image}
              alt={restaurant.name}
              className="w-24 h-24 rounded-xl object-cover shadow-md"
            />

            {/* Info */}
            <div className="flex-1">
              <div className="flex items-start justify-between">
                <div>
                  <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">
                    {restaurant.name}
                  </h1>
                  <p className="text-gray-500 mb-4">{restaurant.cuisine.join(' • ')}</p>
                </div>
                <div className="flex gap-2">
                  <button className="p-2 rounded-full bg-gray-100 text-gray-600 hover:bg-gray-200">
                    <HeartIcon className="h-5 w-5" />
                  </button>
                  <button className="p-2 rounded-full bg-gray-100 text-gray-600 hover:bg-gray-200">
                    <ShareIcon className="h-5 w-5" />
                  </button>
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-4 text-sm">
                <div className="flex items-center gap-1">
                  <StarIcon className="h-5 w-5 text-yellow-400" />
                  <span className="font-semibold">{restaurant.rating}</span>
                  <span className="text-gray-400">({restaurant.reviewCount} reviews)</span>
                </div>
                <div className="flex items-center gap-1 text-gray-500">
                  <ClockIcon className="h-5 w-5" />
                  <span>{restaurant.deliveryTime}</span>
                </div>
                <div className="flex items-center gap-1 text-gray-500">
                  <MapPinIcon className="h-5 w-5" />
                  <span>{restaurant.address.city}</span>
                </div>
                <span className={`badge ${restaurant.isOpen ? 'badge-secondary' : 'badge-danger'}`}>
                  {restaurant.isOpen ? 'Open' : 'Closed'}
                </span>
              </div>

              <p className="text-gray-600 mt-4 hidden sm:block">{restaurant.description}</p>
            </div>
          </div>

          {/* Delivery Info */}
          <div className="mt-6 pt-6 border-t border-gray-100 flex flex-wrap gap-6">
            <div>
              <span className="text-sm text-gray-500">Delivery fee</span>
              <p className="font-semibold">
                {restaurant.deliveryFee === 0 ? 'Free' : `$${restaurant.deliveryFee.toFixed(2)}`}
              </p>
            </div>
            <div>
              <span className="text-sm text-gray-500">Minimum order</span>
              <p className="font-semibold">${restaurant.minOrder.toFixed(2)}</p>
            </div>
            <div>
              <span className="text-sm text-gray-500">Delivery time</span>
              <p className="font-semibold">{restaurant.deliveryTime}</p>
            </div>
          </div>
        </div>

        {/* Menu Section */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 pb-24">
          {/* Categories Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm p-4 sticky top-20">
              <h3 className="font-semibold text-gray-900 mb-4">Menu</h3>
              <nav className="space-y-1">
                {menuCategories.map((category) => (
                  <button
                    key={category.id}
                    onClick={() => setSelectedCategory(category.id)}
                    className={`w-full text-left px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      selectedCategory === category.id
                        ? 'bg-primary-50 text-primary-600'
                        : 'text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    {category.name}
                  </button>
                ))}
              </nav>
            </div>
          </div>

          {/* Menu Items */}
          <div className="lg:col-span-3">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-gray-900">
                {menuCategories.find(c => c.id === selectedCategory)?.name}
              </h2>
              <span className="text-sm text-gray-500">
                {filteredItems.length} items
              </span>
            </div>

            <div className="space-y-4">
              {filteredItems.map((item, index) => (
                <motion.div
                  key={item.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                >
                  <MenuItemCard item={item} onAddToCart={handleAddToCart} />
                </motion.div>
              ))}
            </div>

            {filteredItems.length === 0 && (
              <div className="text-center py-12">
                <InformationCircleIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500">No items in this category</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
