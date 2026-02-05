import { Outlet, Link, useNavigate } from 'react-router-dom'
import { Fragment, useState } from 'react'
import { Menu, Transition, Popover } from '@headlessui/react'
import {
  ShoppingCartIcon,
  UserCircleIcon,
  Bars3Icon,
  XMarkIcon,
  BellIcon,
  MapPinIcon,
  MagnifyingGlassIcon,
} from '@heroicons/react/24/outline'
import { useAuthStore } from '../store/authStore'
import { useCartStore } from '../store/cartStore'

const navigation = [
  { name: 'Home', href: '/' },
  { name: 'Restaurants', href: '/restaurants' },
]

export default function MainLayout() {
  const navigate = useNavigate()
  const { isAuthenticated, user, logout } = useAuthStore()
  const { itemCount } = useCartStore()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm sticky top-0 z-50">
        <nav className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between">
            {/* Logo */}
            <div className="flex items-center">
              <Link to="/" className="flex items-center gap-2">
                <div className="w-10 h-10 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center">
                  <span className="text-white text-xl font-bold">F</span>
                </div>
                <span className="text-xl font-bold text-gray-900">
                  Food<span className="text-primary-600">Express</span>
                </span>
              </Link>
            </div>

            {/* Desktop Navigation */}
            <div className="hidden md:flex md:items-center md:gap-8">
              {navigation.map((item) => (
                <Link
                  key={item.name}
                  to={item.href}
                  className="text-sm font-medium text-gray-700 hover:text-primary-600 transition-colors"
                >
                  {item.name}
                </Link>
              ))}
            </div>

            {/* Search Bar */}
            <div className="hidden lg:flex flex-1 max-w-md mx-8">
              <div className="relative w-full">
                <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search restaurants, cuisines..."
                  className="input pl-10"
                />
              </div>
            </div>

            {/* Right side actions */}
            <div className="flex items-center gap-4">
              {/* Location */}
              <button className="hidden sm:flex items-center gap-1 text-sm text-gray-600 hover:text-primary-600">
                <MapPinIcon className="h-5 w-5" />
                <span className="max-w-[100px] truncate">New York</span>
              </button>

              {/* Cart */}
              <Link to="/cart" className="relative p-2 text-gray-600 hover:text-primary-600">
                <ShoppingCartIcon className="h-6 w-6" />
                {itemCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-primary-600 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
                    {itemCount > 99 ? '99+' : itemCount}
                  </span>
                )}
              </Link>

              {isAuthenticated ? (
                <>
                  {/* Notifications */}
                  <Popover className="relative">
                    <Popover.Button className="p-2 text-gray-600 hover:text-primary-600">
                      <BellIcon className="h-6 w-6" />
                    </Popover.Button>
                    <Transition
                      as={Fragment}
                      enter="transition ease-out duration-200"
                      enterFrom="opacity-0 translate-y-1"
                      enterTo="opacity-100 translate-y-0"
                      leave="transition ease-in duration-150"
                      leaveFrom="opacity-100 translate-y-0"
                      leaveTo="opacity-0 translate-y-1"
                    >
                      <Popover.Panel className="absolute right-0 z-10 mt-3 w-80 bg-white rounded-xl shadow-lg ring-1 ring-black/5 p-4">
                        <div className="text-center text-gray-500 py-8">
                          No new notifications
                        </div>
                        <Link
                          to="/notifications"
                          className="block text-center text-sm text-primary-600 hover:text-primary-700 font-medium"
                        >
                          View all notifications
                        </Link>
                      </Popover.Panel>
                    </Transition>
                  </Popover>

                  {/* User Menu */}
                  <Menu as="div" className="relative">
                    <Menu.Button className="flex items-center gap-2 p-2 text-gray-600 hover:text-primary-600">
                      <UserCircleIcon className="h-6 w-6" />
                      <span className="hidden sm:block text-sm font-medium">
                        {user?.firstName || 'Account'}
                      </span>
                    </Menu.Button>
                    <Transition
                      as={Fragment}
                      enter="transition ease-out duration-100"
                      enterFrom="transform opacity-0 scale-95"
                      enterTo="transform opacity-100 scale-100"
                      leave="transition ease-in duration-75"
                      leaveFrom="transform opacity-100 scale-100"
                      leaveTo="transform opacity-0 scale-95"
                    >
                      <Menu.Items className="absolute right-0 z-10 mt-2 w-48 origin-top-right bg-white rounded-xl shadow-lg ring-1 ring-black/5 py-1">
                        <Menu.Item>
                          {({ active }) => (
                            <Link
                              to="/profile"
                              className={`${active ? 'bg-gray-50' : ''} block px-4 py-2 text-sm text-gray-700`}
                            >
                              My Profile
                            </Link>
                          )}
                        </Menu.Item>
                        <Menu.Item>
                          {({ active }) => (
                            <Link
                              to="/orders"
                              className={`${active ? 'bg-gray-50' : ''} block px-4 py-2 text-sm text-gray-700`}
                            >
                              My Orders
                            </Link>
                          )}
                        </Menu.Item>
                        <Menu.Item>
                          {({ active }) => (
                            <Link
                              to="/addresses"
                              className={`${active ? 'bg-gray-50' : ''} block px-4 py-2 text-sm text-gray-700`}
                            >
                              Saved Addresses
                            </Link>
                          )}
                        </Menu.Item>
                        {user?.role === 'ADMIN' && (
                          <Menu.Item>
                            {({ active }) => (
                              <Link
                                to="/admin"
                                className={`${active ? 'bg-gray-50' : ''} block px-4 py-2 text-sm text-primary-600 font-medium`}
                              >
                                Admin Dashboard
                              </Link>
                            )}
                          </Menu.Item>
                        )}
                        <div className="border-t border-gray-100 my-1" />
                        <Menu.Item>
                          {({ active }) => (
                            <button
                              onClick={handleLogout}
                              className={`${active ? 'bg-gray-50' : ''} block w-full text-left px-4 py-2 text-sm text-red-600`}
                            >
                              Sign out
                            </button>
                          )}
                        </Menu.Item>
                      </Menu.Items>
                    </Transition>
                  </Menu>
                </>
              ) : (
                <div className="flex items-center gap-2">
                  <Link to="/login" className="btn-ghost">
                    Sign in
                  </Link>
                  <Link to="/register" className="btn-primary hidden sm:inline-flex">
                    Sign up
                  </Link>
                </div>
              )}

              {/* Mobile menu button */}
              <button
                type="button"
                className="md:hidden p-2 text-gray-600"
                onClick={() => setMobileMenuOpen(true)}
              >
                <Bars3Icon className="h-6 w-6" />
              </button>
            </div>
          </div>
        </nav>

        {/* Mobile menu */}
        <Transition show={mobileMenuOpen} as={Fragment}>
          <div className="md:hidden">
            <Transition.Child
              as={Fragment}
              enter="transition-opacity ease-linear duration-300"
              enterFrom="opacity-0"
              enterTo="opacity-100"
              leave="transition-opacity ease-linear duration-300"
              leaveFrom="opacity-100"
              leaveTo="opacity-0"
            >
              <div className="fixed inset-0 bg-black/25 z-40" onClick={() => setMobileMenuOpen(false)} />
            </Transition.Child>
            <Transition.Child
              as={Fragment}
              enter="transition ease-in-out duration-300 transform"
              enterFrom="-translate-x-full"
              enterTo="translate-x-0"
              leave="transition ease-in-out duration-300 transform"
              leaveFrom="translate-x-0"
              leaveTo="-translate-x-full"
            >
              <div className="fixed inset-y-0 left-0 z-50 w-full max-w-xs bg-white shadow-xl">
                <div className="flex items-center justify-between p-4 border-b">
                  <span className="text-xl font-bold text-gray-900">Menu</span>
                  <button onClick={() => setMobileMenuOpen(false)}>
                    <XMarkIcon className="h-6 w-6 text-gray-600" />
                  </button>
                </div>
                <div className="p-4 space-y-4">
                  {navigation.map((item) => (
                    <Link
                      key={item.name}
                      to={item.href}
                      className="block text-lg font-medium text-gray-700 hover:text-primary-600"
                      onClick={() => setMobileMenuOpen(false)}
                    >
                      {item.name}
                    </Link>
                  ))}
                </div>
              </div>
            </Transition.Child>
          </div>
        </Transition>
      </header>

      {/* Main Content */}
      <main>
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-white mt-16">
        <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            <div className="col-span-2 md:col-span-1">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-10 h-10 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center">
                  <span className="text-white text-xl font-bold">F</span>
                </div>
                <span className="text-xl font-bold">FoodExpress</span>
              </div>
              <p className="text-gray-400 text-sm">
                Delicious food delivered fast to your doorstep.
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-4">Company</h3>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><a href="#" className="hover:text-white">About Us</a></li>
                <li><a href="#" className="hover:text-white">Careers</a></li>
                <li><a href="#" className="hover:text-white">Blog</a></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">Support</h3>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><a href="#" className="hover:text-white">Help Center</a></li>
                <li><a href="#" className="hover:text-white">Contact Us</a></li>
                <li><a href="#" className="hover:text-white">FAQs</a></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">Legal</h3>
              <ul className="space-y-2 text-sm text-gray-400">
                <li><a href="#" className="hover:text-white">Privacy Policy</a></li>
                <li><a href="#" className="hover:text-white">Terms of Service</a></li>
                <li><a href="#" className="hover:text-white">Cookie Policy</a></li>
              </ul>
            </div>
          </div>
          <div className="border-t border-gray-800 mt-8 pt-8 text-center text-sm text-gray-400">
            © 2026 FoodExpress. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  )
}
