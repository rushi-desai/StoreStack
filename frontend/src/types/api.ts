export type Role = 'USER' | 'ADMIN'

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'

export interface AuthUser {
  id: number
  email: string
  role: Role
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  role: Role
  email: string
  userId: number
}

export interface TokenRefreshResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface RegisterRequest {
  name: string
  password: string
  email: string
  phone: string
  address: string
}

export interface Product {
  id: number
  name: string
  description: string
  price: number
  imageUrl: string | null
  stock: number
  categoryId: number
}

export type ProductRequest = Omit<Product, 'id'>

export interface Category {
  id: number
  name: string
  description: string | null
}

export type CategoryRequest = Omit<Category, 'id'>

export interface ProductPage {
  content: Product[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface CartItem {
  id: number
  productId: number
  productName: string
  quantity: number
  price: number
  lineTotal: number
}

export interface Cart {
  id: number
  userId: number
  cartItems: CartItem[]
  totalAmount: number
}

export interface OrderItem {
  id: number
  productId: number
  quantity: number
  price: number
}

export interface Order {
  id: number
  orderNumber: string
  totalAmount: number
  status: OrderStatus
  userId: number
  orderItems: OrderItem[]
}

export interface UserProfile {
  id: number
  name: string
  email: string
  role: Role
  phoneNumber: string | null
  address: string | null
}

export interface ApiErrorResponse {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
  fieldErrors?: Record<string, string>
}

export interface StoredSession {
  accessToken: string
  refreshToken: string
  user: AuthUser
}
