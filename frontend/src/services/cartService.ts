import type { Cart } from '../types/api'
import { apiClient } from './http'

export async function getCart(): Promise<Cart> {
  const { data } = await apiClient.get<Cart>('/carts')
  return data
}

export async function addCartItem(productId: number, quantity = 1): Promise<string> {
  const { data } = await apiClient.post<string>('/carts/add', { productId, quantity })
  return data
}

export async function updateCartItem(itemId: number, quantity: number): Promise<Cart> {
  const { data } = await apiClient.put<Cart>(`/carts/items/${itemId}`, { quantity })
  return data
}

export async function removeCartItem(itemId: number): Promise<void> {
  await apiClient.delete(`/carts/remove/${itemId}`)
}

export async function clearCart(): Promise<void> {
  await apiClient.delete('/carts')
}
