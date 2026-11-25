import type { Category, CategoryRequest } from '../types/api'
import { apiClient } from './http'

export async function getCategories(): Promise<Category[]> {
  const { data } = await apiClient.get<Category[]>('/categories')
  return data
}

export async function createCategory(category: CategoryRequest): Promise<Category> {
  const { data } = await apiClient.post<Category>('/categories', category)
  return data
}

export async function updateCategory(id: number, category: CategoryRequest): Promise<Category> {
  const { data } = await apiClient.put<Category>(`/categories/${id}`, category)
  return data
}

export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/categories/${id}`)
}
