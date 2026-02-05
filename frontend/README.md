# FoodExpress Frontend

A modern, production-ready React frontend for the FoodExpress food delivery application.

## Tech Stack

- **React 18** - Modern UI library with hooks
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework
- **React Router v6** - Client-side routing
- **TanStack Query** - Server state management
- **Zustand** - Lightweight state management
- **React Hook Form** - Form handling with validation
- **Framer Motion** - Animations
- **Recharts** - Charts for analytics
- **Axios** - HTTP client
- **Heroicons** - Beautiful icons
- **React Hot Toast** - Toast notifications

## Prerequisites

1. **Node.js 18+** - Download from [https://nodejs.org/](https://nodejs.org/)
2. **npm** or **yarn** - Comes with Node.js

## Installation

1. Navigate to the frontend directory:
   ```bash
   cd "Food delivery application/frontend"
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

4. Open your browser and visit: **http://localhost:3000**

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server on port 3000 |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm run lint` | Run ESLint |

## Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── components/        # Reusable UI components
│   │   └── common/        # Common components (Modal, LoadingSpinner, etc.)
│   ├── layouts/           # Page layouts
│   │   ├── MainLayout.tsx     # Public pages layout
│   │   ├── DashboardLayout.tsx # Admin dashboard layout
│   │   └── AuthLayout.tsx     # Authentication pages layout
│   ├── pages/             # Page components
│   │   ├── auth/          # Login, Register, ForgotPassword
│   │   ├── customer/      # Cart, Checkout, Orders, Profile
│   │   └── admin/         # Dashboard, Orders, Users, Analytics
│   ├── services/          # API service functions
│   ├── store/             # Zustand state stores
│   ├── types/             # TypeScript type definitions
│   ├── App.tsx            # Main app with routing
│   ├── main.tsx           # React entry point
│   └── index.css          # Global styles
├── index.html             # HTML template
├── vite.config.ts         # Vite configuration
├── tailwind.config.js     # Tailwind CSS configuration
├── tsconfig.json          # TypeScript configuration
└── package.json           # Dependencies
```

## Features

### Customer Features
- 🏠 **Home Page** - Hero section, featured restaurants, cuisine categories
- 🍽️ **Restaurant Browsing** - Search, filter by cuisine, sort by rating
- 📋 **Menu Viewing** - Restaurant menus with categories
- 🛒 **Shopping Cart** - Add/remove items, quantity controls
- 💳 **Checkout** - Address selection, payment methods, order placement
- 📦 **Order Tracking** - Real-time order status, delivery progress
- 👤 **Profile Management** - Update profile, manage addresses
- 🔔 **Notifications** - Order updates, promotions

### Admin Features
- 📊 **Dashboard** - Revenue stats, order charts, recent orders
- 📋 **Order Management** - View, filter, update order status
- 👥 **User Management** - View users, edit roles, activate/deactivate
- 📈 **Analytics** - Revenue trends, order patterns, top restaurants
- 💰 **Payment Management** - View transactions, process refunds

## API Integration

The frontend is configured to proxy API requests to the backend microservices:

| Route | Backend Service | Port |
|-------|-----------------|------|
| `/api/auth/*` | Auth Service | 8081 |
| `/api/payments/*` | Payment Service | 8082 |
| `/api/orders/*` | Order Service | 8083 |
| `/api/users/*` | User Service | 8084 |
| `/api/notifications/*` | Notification Service | 8085 |
| `/api/analytics/*` | Analytics Service | 8086 |

## Demo Credentials

For testing without backend:

**Customer Account:**
- Email: `customer@foodexpress.com`
- Password: `Password123!`

**Admin Account:**
- Email: `admin@foodexpress.com`
- Password: `Admin123!`

## UI Components

### Common Components
- `LoadingSpinner` - Loading states
- `Modal` - Reusable modal dialog
- `EmptyState` - Empty state with icon and message
- `RestaurantCard` - Restaurant listing card
- `MenuItemCard` - Menu item with add to cart
- `OrderStatusBadge` - Order status badge with colors

### Custom CSS Classes
- `.btn-primary` - Primary orange button
- `.btn-secondary` - Secondary gray button
- `.btn-outline` - Outlined button
- `.btn-danger` - Red danger button
- `.input` - Styled input field
- `.card` - Card with shadow and rounded corners
- `.badge-primary` - Primary badge
- `.label` - Form label

## Color Scheme

| Color | Usage |
|-------|-------|
| Orange (#F97316) | Primary - CTAs, branding |
| Slate (#475569) | Secondary - text, backgrounds |
| Green | Success states |
| Red | Error states, danger actions |
| Yellow | Warning states |
| Blue | Info, links |

## Running with Backend

1. Start all backend microservices (ports 8081-8086)
2. Start the frontend (`npm run dev`)
3. The Vite proxy will forward API requests to the appropriate backend service

## Building for Production

```bash
# Build the application
npm run build

# Preview the build
npm run preview
```

The production build will be in the `dist/` directory.

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## License

MIT License - FoodExpress 2026
