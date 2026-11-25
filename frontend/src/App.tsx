import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { AppShell } from './components/AppShell'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminOrderManagementPage } from './pages/admin/AdminOrderManagementPage'
import { AdminPage } from './pages/admin/AdminPage'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { LoginPage } from './pages/LoginPage'
import { OrderDetailPage } from './pages/OrderDetailPage'
import { OrdersPage } from './pages/OrdersPage'
import { ProductDetailPage } from './pages/ProductDetailPage'
import { ProductsPage } from './pages/ProductsPage'
import { RegisterPage } from './pages/RegisterPage'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<ProductsPage />} />
            <Route path="products/:productId" element={<ProductDetailPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route
              path="cart"
              element={<ProtectedRoute userOnly><CartPage /></ProtectedRoute>}
            />
            <Route
              path="checkout"
              element={<ProtectedRoute userOnly><CheckoutPage /></ProtectedRoute>}
            />
            <Route
              path="orders"
              element={<ProtectedRoute userOnly><OrdersPage /></ProtectedRoute>}
            />
            <Route
              path="orders/:orderId"
              element={<ProtectedRoute userOnly><OrderDetailPage /></ProtectedRoute>}
            />
            <Route
              path="admin"
              element={<ProtectedRoute adminOnly><AdminPage /></ProtectedRoute>}
            />
            <Route
              path="admin/orders"
              element={<ProtectedRoute adminOnly><AdminOrderManagementPage /></ProtectedRoute>}
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
