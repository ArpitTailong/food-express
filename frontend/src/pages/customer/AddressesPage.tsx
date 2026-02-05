import { useState } from 'react'
import { motion } from 'framer-motion'
import {
  PlusIcon,
  PencilIcon,
  TrashIcon,
  MapPinIcon,
  HomeIcon,
  BuildingOfficeIcon,
} from '@heroicons/react/24/outline'
import toast from 'react-hot-toast'
import Modal from '../../components/common/Modal'
import { ButtonLoader } from '../../components/common/LoadingSpinner'
import type { Address } from '../../types'

// Mock addresses
const initialAddresses: Address[] = [
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
    street: '456 Business Avenue',
    city: 'New York',
    state: 'NY',
    zipCode: '10002',
    country: 'USA',
    isDefault: false,
  },
]

export default function AddressesPage() {
  const [addresses, setAddresses] = useState<Address[]>(initialAddresses)
  const [showModal, setShowModal] = useState(false)
  const [editingAddress, setEditingAddress] = useState<Address | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [formData, setFormData] = useState({
    label: '',
    street: '',
    city: '',
    state: '',
    zipCode: '',
    country: 'USA',
  })

  const handleOpenModal = (address?: Address) => {
    if (address) {
      setEditingAddress(address)
      setFormData({
        label: address.label,
        street: address.street,
        city: address.city,
        state: address.state,
        zipCode: address.zipCode,
        country: address.country,
      })
    } else {
      setEditingAddress(null)
      setFormData({
        label: '',
        street: '',
        city: '',
        state: '',
        zipCode: '',
        country: 'USA',
      })
    }
    setShowModal(true)
  }

  const handleSave = async () => {
    setIsLoading(true)
    await new Promise((resolve) => setTimeout(resolve, 1000))

    if (editingAddress) {
      setAddresses(addresses.map((a) =>
        a.id === editingAddress.id ? { ...a, ...formData } : a
      ))
      toast.success('Address updated!')
    } else {
      const newAddress: Address = {
        id: Date.now().toString(),
        ...formData,
        isDefault: addresses.length === 0,
      }
      setAddresses([...addresses, newAddress])
      toast.success('Address added!')
    }

    setShowModal(false)
    setIsLoading(false)
  }

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this address?')) {
      setAddresses(addresses.filter((a) => a.id !== id))
      toast.success('Address deleted!')
    }
  }

  const handleSetDefault = async (id: string) => {
    setAddresses(addresses.map((a) => ({
      ...a,
      isDefault: a.id === id,
    })))
    toast.success('Default address updated!')
  }

  const getLabelIcon = (label: string) => {
    switch (label.toLowerCase()) {
      case 'home':
        return HomeIcon
      case 'work':
        return BuildingOfficeIcon
      default:
        return MapPinIcon
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Saved Addresses</h1>
        <button
          onClick={() => handleOpenModal()}
          className="btn-primary flex items-center gap-2"
        >
          <PlusIcon className="h-5 w-5" />
          Add Address
        </button>
      </div>

      <div className="space-y-4">
        {addresses.map((address, index) => {
          const Icon = getLabelIcon(address.label)
          return (
            <motion.div
              key={address.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              className={`card p-4 ${address.isDefault ? 'border-primary-500 border-2' : ''}`}
            >
              <div className="flex items-start gap-4">
                <div className="p-3 bg-gray-100 rounded-lg">
                  <Icon className="h-6 w-6 text-gray-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-gray-900">{address.label}</h3>
                    {address.isDefault && (
                      <span className="badge-primary text-xs">Default</span>
                    )}
                  </div>
                  <p className="text-gray-600 mt-1">
                    {address.street}<br />
                    {address.city}, {address.state} {address.zipCode}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handleOpenModal(address)}
                    className="p-2 text-gray-400 hover:text-primary-600 transition-colors"
                  >
                    <PencilIcon className="h-5 w-5" />
                  </button>
                  <button
                    onClick={() => handleDelete(address.id)}
                    className="p-2 text-gray-400 hover:text-red-600 transition-colors"
                  >
                    <TrashIcon className="h-5 w-5" />
                  </button>
                </div>
              </div>
              {!address.isDefault && (
                <button
                  onClick={() => handleSetDefault(address.id)}
                  className="mt-4 text-sm text-primary-600 hover:text-primary-700 font-medium"
                >
                  Set as default
                </button>
              )}
            </motion.div>
          )
        })}

        {addresses.length === 0 && (
          <div className="text-center py-12">
            <MapPinIcon className="h-12 w-12 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 mb-4">No saved addresses</p>
            <button
              onClick={() => handleOpenModal()}
              className="btn-primary"
            >
              Add your first address
            </button>
          </div>
        )}
      </div>

      {/* Add/Edit Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={editingAddress ? 'Edit Address' : 'Add New Address'}
        size="md"
      >
        <div className="p-6 space-y-4">
          <div>
            <label className="label">Label</label>
            <input
              type="text"
              className="input"
              placeholder="Home, Work, etc."
              value={formData.label}
              onChange={(e) => setFormData({ ...formData, label: e.target.value })}
            />
          </div>
          <div>
            <label className="label">Street Address</label>
            <input
              type="text"
              className="input"
              placeholder="123 Main Street"
              value={formData.street}
              onChange={(e) => setFormData({ ...formData, street: e.target.value })}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">City</label>
              <input
                type="text"
                className="input"
                placeholder="New York"
                value={formData.city}
                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
              />
            </div>
            <div>
              <label className="label">State</label>
              <input
                type="text"
                className="input"
                placeholder="NY"
                value={formData.state}
                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="label">ZIP Code</label>
              <input
                type="text"
                className="input"
                placeholder="10001"
                value={formData.zipCode}
                onChange={(e) => setFormData({ ...formData, zipCode: e.target.value })}
              />
            </div>
            <div>
              <label className="label">Country</label>
              <input
                type="text"
                className="input"
                value={formData.country}
                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
              />
            </div>
          </div>
          <div className="flex gap-3 pt-4">
            <button
              onClick={() => setShowModal(false)}
              className="btn-outline flex-1"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={isLoading || !formData.label || !formData.street}
              className="btn-primary flex-1"
            >
              {isLoading ? <ButtonLoader /> : 'Save Address'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
