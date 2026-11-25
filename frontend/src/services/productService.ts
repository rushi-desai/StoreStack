import type { Product, ProductPage, ProductRequest } from '../types/api'
import { apiClient } from './http'

export interface ProductQuery {
  name?: string
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  sortBy?: 'id' | 'name' | 'price' | 'stock'
  direction?: 'asc' | 'desc'
}

export async function getProducts(query: ProductQuery = {}): Promise<ProductPage> {
  const { data } = await apiClient.get<ProductPage>('/products', { params: query })
  return data
}

export async function getProduct(productId: number): Promise<Product> {
  const { data } = await apiClient.get<Product>(`/products/${productId}`)
  return data
}

export async function createProduct(product: ProductRequest): Promise<Product> {
  const { data } = await apiClient.post<Product>('/products', product)
  return data
}

export async function updateProduct(productId: number, product: ProductRequest): Promise<Product> {
  const { data } = await apiClient.put<Product>(`/products/${productId}`, product)
  return data
}

export async function deleteProduct(productId: number): Promise<void> {
  await apiClient.delete(`/products/${productId}`)
}
