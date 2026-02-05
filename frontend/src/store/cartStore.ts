import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { CartItem, MenuItem } from '../types'

interface CartState {
  restaurantId: string | null
  restaurantName: string | null
  items: CartItem[]
  
  // Computed
  itemCount: number
  subtotal: number
  deliveryFee: number
  tax: number
  total: number
  
  // Actions
  addItem: (menuItem: MenuItem, quantity: number, specialInstructions?: string) => void
  updateQuantity: (itemId: string, quantity: number) => void
  removeItem: (itemId: string) => void
  clearCart: () => void
  setRestaurant: (restaurantId: string, restaurantName: string) => void
}

const calculateTotals = (items: CartItem[]) => {
  const subtotal = items.reduce((sum, item) => sum + item.totalPrice, 0)
  const deliveryFee = items.length > 0 ? 2.99 : 0
  const tax = subtotal * 0.0825 // 8.25% tax
  const total = subtotal + deliveryFee + tax
  
  return { subtotal, deliveryFee, tax, total }
}

export const useCartStore = create<CartState>()(
  persist(
    (set, get) => ({
      restaurantId: null,
      restaurantName: null,
      items: [],
      itemCount: 0,
      subtotal: 0,
      deliveryFee: 0,
      tax: 0,
      total: 0,
      
      addItem: (menuItem, quantity, specialInstructions) => {
        const state = get()
        
        // Check if adding from different restaurant
        if (state.restaurantId && state.restaurantId !== menuItem.restaurantId) {
          // Clear cart if different restaurant
          set({
            restaurantId: menuItem.restaurantId,
            restaurantName: null, // Will be set separately
            items: [],
          })
        }
        
        const existingItemIndex = state.items.findIndex(
          item => item.menuItem.id === menuItem.id && 
                  item.specialInstructions === specialInstructions
        )
        
        let newItems: CartItem[]
        
        if (existingItemIndex >= 0) {
          // Update existing item quantity
          newItems = state.items.map((item, index) => 
            index === existingItemIndex 
              ? { 
                  ...item, 
                  quantity: item.quantity + quantity,
                  totalPrice: (item.quantity + quantity) * item.menuItem.price
                }
              : item
          )
        } else {
          // Add new item
          const newItem: CartItem = {
            id: `${menuItem.id}-${Date.now()}`,
            menuItem,
            quantity,
            specialInstructions,
            totalPrice: quantity * menuItem.price,
          }
          newItems = [...state.items, newItem]
        }
        
        const totals = calculateTotals(newItems)
        
        set({
          restaurantId: menuItem.restaurantId,
          items: newItems,
          itemCount: newItems.reduce((sum, item) => sum + item.quantity, 0),
          ...totals,
        })
      },
      
      updateQuantity: (itemId, quantity) => {
        const state = get()
        
        if (quantity <= 0) {
          get().removeItem(itemId)
          return
        }
        
        const newItems = state.items.map(item =>
          item.id === itemId
            ? { ...item, quantity, totalPrice: quantity * item.menuItem.price }
            : item
        )
        
        const totals = calculateTotals(newItems)
        
        set({
          items: newItems,
          itemCount: newItems.reduce((sum, item) => sum + item.quantity, 0),
          ...totals,
        })
      },
      
      removeItem: (itemId) => {
        const state = get()
        const newItems = state.items.filter(item => item.id !== itemId)
        const totals = calculateTotals(newItems)
        
        set({
          items: newItems,
          itemCount: newItems.reduce((sum, item) => sum + item.quantity, 0),
          restaurantId: newItems.length > 0 ? state.restaurantId : null,
          restaurantName: newItems.length > 0 ? state.restaurantName : null,
          ...totals,
        })
      },
      
      clearCart: () => set({
        restaurantId: null,
        restaurantName: null,
        items: [],
        itemCount: 0,
        subtotal: 0,
        deliveryFee: 0,
        tax: 0,
        total: 0,
      }),
      
      setRestaurant: (restaurantId, restaurantName) => set({
        restaurantId,
        restaurantName,
      }),
    }),
    {
      name: 'food-express-cart',
    }
  )
)
