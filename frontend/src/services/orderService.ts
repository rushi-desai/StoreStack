import type { Order } from '../types/api'
import { apiClient } from './http'

export async function getMyOrders(): Promise<Order[]> {
  const { data } = await apiClient.get<Order[]>('/orders')
  return data
}

export async function getOrder(id: number): Promise<Order> {
  const { data } = await apiClient.get<Order>(`/orders/${id}`)
  return data
}

export async function placeOrder(): Promise<string> {
  const { data } = await apiClient.post<string>('/orders/place')
  return data
}

export async function cancelOrder(id: number): Promise<Order> {
  const { data } = await apiClient.post<Order>(`/orders/${id}/cancel`)
  return data
}

