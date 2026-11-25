import type { Order, OrderStatus } from '../types/api'
import { apiClient } from './http'

export async function getAllOrders(): Promise<Order[]> {
  const { data } = await apiClient.get<Order[]>('/admin/orders')
  return data
}

export async function updateOrderStatus(orderId: number, status: OrderStatus): Promise<Order> {
  const { data } = await apiClient.put<Order>(`/admin/orders/${orderId}/status`, { status })
  return data
}
