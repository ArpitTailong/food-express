import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  MagnifyingGlassIcon,
  MapPinIcon,
  ClockIcon,
  TruckIcon,
  StarIcon,
  ShieldCheckIcon,
} from '@heroicons/react/24/outline'
import RestaurantCard from '../components/common/RestaurantCard'

// Mock data for featured restaurants
const featuredRestaurants = [
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
]

const cuisineCategories = [
  { name: 'Pizza', icon: '🍕', color: 'bg-red-100' },
  { name: 'Burgers', icon: '🍔', color: 'bg-yellow-100' },
  { name: 'Sushi', icon: '🍣', color: 'bg-pink-100' },
  { name: 'Chinese', icon: '🥡', color: 'bg-orange-100' },
  { name: 'Indian', icon: '🍛', color: 'bg-amber-100' },
  { name: 'Mexican', icon: '🌮', color: 'bg-green-100' },
  { name: 'Thai', icon: '🍜', color: 'bg-purple-100' },
  { name: 'Desserts', icon: '🍰', color: 'bg-blue-100' },
]

const features = [
  {
    icon: TruckIcon,
    title: 'Fast Delivery',
    description: 'Get your food delivered in 30 minutes or less',
  },
  {
    icon: ShieldCheckIcon,
    title: 'Safe & Secure',
    description: 'Your payments and data are always protected',
  },
  {
    icon: StarIcon,
    title: 'Best Quality',
    description: 'Only the best restaurants with top ratings',
  },
]

export default function HomePage() {
  return (
    <div>
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-primary-600 via-primary-500 to-secondary-500 overflow-hidden">
        {/* Background pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute inset-0" style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
          }} />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 py-20 sm:py-32 sm:px-6 lg:px-8">
          <div className="text-center">
            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-white mb-6"
            >
              Delicious Food,
              <br />
              <span className="text-yellow-300">Delivered Fast</span>
            </motion.h1>
            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="text-lg sm:text-xl text-white/90 mb-8 max-w-2xl mx-auto"
            >
              Order from hundreds of restaurants and get your favorite meals
              delivered right to your doorstep.
            </motion.p>

            {/* Search Bar */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="max-w-2xl mx-auto"
            >
              <div className="bg-white rounded-2xl shadow-2xl p-2 flex flex-col sm:flex-row gap-2">
                <div className="relative flex-1">
                  <MapPinIcon className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Enter your delivery address"
                    className="w-full pl-12 pr-4 py-3 rounded-xl border-0 focus:ring-2 focus:ring-primary-500 text-gray-900 placeholder:text-gray-400"
                  />
                </div>
                <Link
                  to="/restaurants"
                  className="btn-primary btn-lg flex items-center justify-center gap-2"
                >
                  <MagnifyingGlassIcon className="h-5 w-5" />
                  Find Food
                </Link>
              </div>
            </motion.div>

            {/* Stats */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.4 }}
              className="flex flex-wrap justify-center gap-8 mt-12 text-white"
            >
              <div>
                <div className="text-3xl font-bold">500+</div>
                <div className="text-white/80 text-sm">Restaurants</div>
              </div>
              <div>
                <div className="text-3xl font-bold">50K+</div>
                <div className="text-white/80 text-sm">Happy Customers</div>
              </div>
              <div>
                <div className="text-3xl font-bold">30 min</div>
                <div className="text-white/80 text-sm">Average Delivery</div>
              </div>
            </motion.div>
          </div>
        </div>

        {/* Wave decoration */}
        <div className="absolute bottom-0 left-0 right-0">
          <svg viewBox="0 0 1440 120" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path
              d="M0 120L60 105C120 90 240 60 360 45C480 30 600 30 720 37.5C840 45 960 60 1080 67.5C1200 75 1320 75 1380 75L1440 75V120H1380C1320 120 1200 120 1080 120C960 120 840 120 720 120C600 120 480 120 360 120C240 120 120 120 60 120H0Z"
              fill="#f9fafb"
            />
          </svg>
        </div>
      </section>

      {/* Cuisine Categories */}
      <section className="max-w-7xl mx-auto px-4 py-16 sm:px-6 lg:px-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-8">Explore Cuisines</h2>
        <div className="grid grid-cols-4 sm:grid-cols-8 gap-4">
          {cuisineCategories.map((category, index) => (
            <motion.div
              key={category.name}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
            >
              <Link
                to={`/restaurants?cuisine=${category.name.toLowerCase()}`}
                className="block text-center group"
              >
                <div
                  className={`${category.color} w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-2 text-3xl group-hover:scale-110 transition-transform`}
                >
                  {category.icon}
                </div>
                <span className="text-sm text-gray-600 group-hover:text-primary-600 transition-colors">
                  {category.name}
                </span>
              </Link>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Featured Restaurants */}
      <section className="max-w-7xl mx-auto px-4 py-16 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-2xl font-bold text-gray-900">Popular Restaurants</h2>
          <Link
            to="/restaurants"
            className="text-primary-600 hover:text-primary-700 font-medium text-sm"
          >
            View all →
          </Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredRestaurants.map((restaurant) => (
            <RestaurantCard key={restaurant.id} restaurant={restaurant} />
          ))}
        </div>
      </section>

      {/* Features Section */}
      <section className="bg-gray-900 py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-white mb-4">Why Choose FoodExpress?</h2>
            <p className="text-gray-400 max-w-2xl mx-auto">
              We're committed to providing the best food delivery experience
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.1 }}
                className="text-center"
              >
                <div className="w-16 h-16 bg-primary-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
                  <feature.icon className="h-8 w-8 text-white" />
                </div>
                <h3 className="text-xl font-semibold text-white mb-2">{feature.title}</h3>
                <p className="text-gray-400">{feature.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="max-w-7xl mx-auto px-4 py-16 sm:px-6 lg:px-8">
        <div className="bg-gradient-to-r from-primary-600 to-secondary-600 rounded-3xl p-8 sm:p-12 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">Ready to Order?</h2>
          <p className="text-white/90 mb-8 max-w-2xl mx-auto">
            Download our app and get exclusive discounts on your first order!
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/restaurants" className="btn bg-white text-primary-600 hover:bg-gray-100 btn-lg">
              Order Now
            </Link>
            <button className="btn bg-white/10 text-white hover:bg-white/20 btn-lg border border-white/20">
              Download App
            </button>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="max-w-7xl mx-auto px-4 py-16 sm:px-6 lg:px-8">
        <h2 className="text-2xl font-bold text-gray-900 text-center mb-12">How It Works</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {[
            { step: '1', title: 'Choose Your Food', desc: 'Browse menus from your favorite restaurants' },
            { step: '2', title: 'Place Your Order', desc: 'Customize and pay securely online' },
            { step: '3', title: 'Fast Delivery', desc: 'Track your order and enjoy your meal' },
          ].map((item, index) => (
            <motion.div
              key={item.step}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: index * 0.1 }}
              className="text-center"
            >
              <div className="w-12 h-12 bg-primary-100 text-primary-600 rounded-full flex items-center justify-center mx-auto mb-4 text-xl font-bold">
                {item.step}
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">{item.title}</h3>
              <p className="text-gray-500">{item.desc}</p>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  )
}
