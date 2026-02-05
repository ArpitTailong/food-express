import { useState } from 'react'
import { motion } from 'framer-motion'
import {
  CalendarIcon,
  ArrowDownTrayIcon,
} from '@heroicons/react/24/outline'
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
  PieChart,
  Pie,
  Cell,
} from 'recharts'

const revenueData = [
  { date: 'Jan 1', revenue: 4200, orders: 145 },
  { date: 'Jan 2', revenue: 3800, orders: 132 },
  { date: 'Jan 3', revenue: 5100, orders: 178 },
  { date: 'Jan 4', revenue: 4600, orders: 156 },
  { date: 'Jan 5', revenue: 5400, orders: 189 },
  { date: 'Jan 6', revenue: 6200, orders: 215 },
  { date: 'Jan 7', revenue: 7100, orders: 248 },
]

const ordersByHour = [
  { hour: '6 AM', orders: 12 },
  { hour: '8 AM', orders: 45 },
  { hour: '10 AM', orders: 78 },
  { hour: '12 PM', orders: 145 },
  { hour: '2 PM', orders: 98 },
  { hour: '4 PM', orders: 67 },
  { hour: '6 PM', orders: 123 },
  { hour: '8 PM', orders: 189 },
  { hour: '10 PM', orders: 134 },
]

const categoryData = [
  { name: 'Pizza', value: 32, color: '#F97316' },
  { name: 'Burgers', value: 25, color: '#EAB308' },
  { name: 'Sushi', value: 18, color: '#22C55E' },
  { name: 'Mexican', value: 14, color: '#3B82F6' },
  { name: 'Chinese', value: 11, color: '#8B5CF6' },
]

const topRestaurants = [
  { name: 'Pizza Palace', orders: 342, revenue: 12840 },
  { name: 'Burger Barn', orders: 287, revenue: 9650 },
  { name: 'Sushi Supreme', orders: 245, revenue: 14320 },
  { name: 'Taco Town', orders: 198, revenue: 6540 },
  { name: 'Thai Delight', orders: 156, revenue: 7890 },
]

const customerGrowth = [
  { month: 'Aug', new: 245, returning: 890 },
  { month: 'Sep', new: 312, returning: 1023 },
  { month: 'Oct', new: 387, returning: 1156 },
  { month: 'Nov', new: 421, returning: 1345 },
  { month: 'Dec', new: 489, returning: 1512 },
  { month: 'Jan', new: 534, returning: 1678 },
]

export default function AdminAnalytics() {
  const [dateRange, setDateRange] = useState('7d')

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
        <div className="flex items-center gap-3">
          <select
            className="input w-auto"
            value={dateRange}
            onChange={(e) => setDateRange(e.target.value)}
          >
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
            <option value="90d">Last 3 months</option>
            <option value="1y">Last year</option>
          </select>
          <button className="btn-outline flex items-center gap-2">
            <ArrowDownTrayIcon className="h-5 w-5" />
            Export
          </button>
        </div>
      </div>

      {/* Revenue & Orders Chart */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card p-6"
      >
        <h3 className="font-semibold text-gray-900 mb-4">Revenue & Orders</h3>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={revenueData}>
              <defs>
                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#F97316" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#F97316" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="colorOrders" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#3B82F6" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
              <XAxis dataKey="date" stroke="#9CA3AF" fontSize={12} />
              <YAxis yAxisId="left" stroke="#F97316" fontSize={12} tickFormatter={(v) => `$${v}`} />
              <YAxis yAxisId="right" orientation="right" stroke="#3B82F6" fontSize={12} />
              <Tooltip
                contentStyle={{
                  borderRadius: '8px',
                  border: '1px solid #E5E7EB',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                }}
              />
              <Legend />
              <Area
                yAxisId="left"
                type="monotone"
                dataKey="revenue"
                name="Revenue"
                stroke="#F97316"
                strokeWidth={2}
                fill="url(#colorRevenue)"
              />
              <Area
                yAxisId="right"
                type="monotone"
                dataKey="orders"
                name="Orders"
                stroke="#3B82F6"
                strokeWidth={2}
                fill="url(#colorOrders)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </motion.div>

      {/* Grid of Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Orders by Hour */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card p-6"
        >
          <h3 className="font-semibold text-gray-900 mb-4">Orders by Hour</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={ordersByHour}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="hour" stroke="#9CA3AF" fontSize={11} />
                <YAxis stroke="#9CA3AF" fontSize={12} />
                <Tooltip
                  contentStyle={{
                    borderRadius: '8px',
                    border: '1px solid #E5E7EB',
                  }}
                />
                <Bar dataKey="orders" fill="#F97316" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </motion.div>

        {/* Orders by Category */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="card p-6"
        >
          <h3 className="font-semibold text-gray-900 mb-4">Orders by Category</h3>
          <div className="flex items-center gap-8">
            <div className="h-48 w-48">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={70}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="flex-1 space-y-3">
              {categoryData.map((item) => (
                <div key={item.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: item.color }}
                    />
                    <span className="text-sm text-gray-600">{item.name}</span>
                  </div>
                  <span className="text-sm font-medium text-gray-900">{item.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </motion.div>

        {/* Customer Growth */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card p-6"
        >
          <h3 className="font-semibold text-gray-900 mb-4">Customer Growth</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={customerGrowth}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis dataKey="month" stroke="#9CA3AF" fontSize={12} />
                <YAxis stroke="#9CA3AF" fontSize={12} />
                <Tooltip
                  contentStyle={{
                    borderRadius: '8px',
                    border: '1px solid #E5E7EB',
                  }}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="new"
                  name="New Customers"
                  stroke="#10B981"
                  strokeWidth={2}
                  dot={{ fill: '#10B981', strokeWidth: 2 }}
                />
                <Line
                  type="monotone"
                  dataKey="returning"
                  name="Returning"
                  stroke="#6366F1"
                  strokeWidth={2}
                  dot={{ fill: '#6366F1', strokeWidth: 2 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </motion.div>

        {/* Top Restaurants */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
          className="card p-6"
        >
          <h3 className="font-semibold text-gray-900 mb-4">Top Restaurants</h3>
          <div className="space-y-4">
            {topRestaurants.map((restaurant, index) => (
              <div key={restaurant.name} className="flex items-center gap-4">
                <div className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center text-sm font-medium text-gray-600">
                  {index + 1}
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-gray-900">{restaurant.name}</span>
                    <span className="text-sm font-medium text-gray-900">
                      ${restaurant.revenue.toLocaleString()}
                    </span>
                  </div>
                  <div className="flex items-center justify-between mt-1">
                    <div className="flex-1 bg-gray-200 rounded-full h-1.5 mr-4">
                      <div
                        className="bg-primary-500 h-1.5 rounded-full"
                        style={{ width: `${(restaurant.orders / topRestaurants[0].orders) * 100}%` }}
                      />
                    </div>
                    <span className="text-xs text-gray-500">{restaurant.orders} orders</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Summary Stats */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="grid grid-cols-2 md:grid-cols-4 gap-4"
      >
        {[
          { label: 'Avg Order Value', value: '$27.45', change: '+4.2%' },
          { label: 'Delivery Time', value: '32 min', change: '-8.5%' },
          { label: 'Customer Rating', value: '4.8', change: '+0.3' },
          { label: 'Return Rate', value: '68%', change: '+12%' },
        ].map((stat) => (
          <div key={stat.label} className="card p-4 text-center">
            <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
            <p className="text-sm text-gray-500 mt-1">{stat.label}</p>
            <span className="text-xs text-green-600 font-medium">{stat.change}</span>
          </div>
        ))}
      </motion.div>
    </div>
  )
}
