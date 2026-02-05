import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { motion } from 'framer-motion'
import { CameraIcon } from '@heroicons/react/24/outline'
import toast from 'react-hot-toast'
import { ButtonLoader } from '../../components/common/LoadingSpinner'
import { useAuthStore } from '../../store/authStore'

interface ProfileFormData {
  firstName: string
  lastName: string
  email: string
  phoneNumber: string
}

export default function ProfilePage() {
  const { user } = useAuthStore()
  const [isLoading, setIsLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<ProfileFormData>({
    defaultValues: {
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      email: user?.email || '',
      phoneNumber: user?.phoneNumber || '',
    },
  })

  const onSubmit = async (data: ProfileFormData) => {
    setIsLoading(true)
    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 1000))
    toast.success('Profile updated successfully!')
    setIsLoading(false)
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">My Profile</h1>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="card p-6"
      >
        {/* Avatar */}
        <div className="flex items-center gap-6 mb-8 pb-8 border-b">
          <div className="relative">
            <div className="w-24 h-24 bg-primary-100 rounded-full flex items-center justify-center text-primary-600 text-3xl font-bold">
              {user?.firstName?.charAt(0) || 'U'}
            </div>
            <button className="absolute bottom-0 right-0 p-2 bg-white rounded-full shadow-md border border-gray-200 hover:bg-gray-50">
              <CameraIcon className="h-4 w-4 text-gray-600" />
            </button>
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-900">
              {user?.firstName} {user?.lastName}
            </h2>
            <p className="text-gray-500">{user?.email}</p>
            <span className="badge-primary mt-2">{user?.role}</span>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div>
              <label className="label">First Name</label>
              <input
                type="text"
                className={`input ${errors.firstName ? 'input-error' : ''}`}
                {...register('firstName', { required: 'First name is required' })}
              />
              {errors.firstName && (
                <p className="mt-1 text-sm text-red-500">{errors.firstName.message}</p>
              )}
            </div>
            <div>
              <label className="label">Last Name</label>
              <input
                type="text"
                className={`input ${errors.lastName ? 'input-error' : ''}`}
                {...register('lastName', { required: 'Last name is required' })}
              />
              {errors.lastName && (
                <p className="mt-1 text-sm text-red-500">{errors.lastName.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="label">Email</label>
            <input
              type="email"
              className="input bg-gray-50"
              disabled
              {...register('email')}
            />
            <p className="mt-1 text-xs text-gray-400">Email cannot be changed</p>
          </div>

          <div>
            <label className="label">Phone Number</label>
            <input
              type="tel"
              className={`input ${errors.phoneNumber ? 'input-error' : ''}`}
              placeholder="+1 (555) 000-0000"
              {...register('phoneNumber')}
            />
          </div>

          <div className="flex items-center justify-between pt-6 border-t">
            <button type="button" className="btn-outline">
              Change Password
            </button>
            <button
              type="submit"
              disabled={isLoading || !isDirty}
              className="btn-primary"
            >
              {isLoading ? <ButtonLoader /> : 'Save Changes'}
            </button>
          </div>
        </form>
      </motion.div>

      {/* Danger Zone */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="card p-6 mt-6 border-red-200"
      >
        <h3 className="font-semibold text-red-600 mb-2">Danger Zone</h3>
        <p className="text-sm text-gray-500 mb-4">
          Once you delete your account, there is no going back. Please be certain.
        </p>
        <button className="btn-danger btn-sm">Delete Account</button>
      </motion.div>
    </div>
  )
}
