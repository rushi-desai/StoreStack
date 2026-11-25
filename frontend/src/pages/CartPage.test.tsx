import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CartPage } from './CartPage'
import * as cartService from '../services/cartService'
import type { Cart } from '../types/api'

vi.mock('../services/cartService', () => ({
  clearCart: vi.fn(),
  getCart: vi.fn(),
  removeCartItem: vi.fn(),
  updateCartItem: vi.fn(),
}))

const fullCart: Cart = {
  id: 3,
  userId: 7,
  totalAmount: 399.98,
  cartItems: [{
    id: 12,
    productId: 4,
    productName: 'Desk lamp',
    quantity: 2,
    price: 199.99,
    lineTotal: 399.98,
  }],
}

describe('CartPage', () => {
  beforeEach(() => {
    vi.mocked(cartService.getCart).mockResolvedValue(fullCart)
    vi.mocked(cartService.updateCartItem).mockResolvedValue({
      ...fullCart,
      totalAmount: 599.97,
      cartItems: [{ ...fullCart.cartItems[0], quantity: 3, lineTotal: 599.97 }],
    })
    vi.mocked(cartService.clearCart).mockResolvedValue()
  })

  it('updates item quantity and displays the returned cart total', async () => {
    render(<MemoryRouter><CartPage /></MemoryRouter>)

    expect(await screen.findByText('Desk lamp')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Increase quantity' }))

    await waitFor(() => expect(cartService.updateCartItem).toHaveBeenCalledWith(12, 3))
    expect(await screen.findByText('3')).toBeInTheDocument()
    expect(screen.getAllByText('$599.97')).toHaveLength(3)
  })

  it('clears the cart and renders the empty state', async () => {
    vi.mocked(cartService.getCart)
      .mockResolvedValueOnce(fullCart)
      .mockResolvedValueOnce({ ...fullCart, totalAmount: 0, cartItems: [] })
    render(<MemoryRouter><CartPage /></MemoryRouter>)

    fireEvent.click(await screen.findByRole('button', { name: 'Clear cart' }))

    expect(cartService.clearCart).toHaveBeenCalledOnce()
    expect(await screen.findByText('Your cart is taking a little break')).toBeInTheDocument()
  })
})
