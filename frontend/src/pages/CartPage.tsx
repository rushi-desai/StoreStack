import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorNotice, LoadingState, EmptyState } from '../components/Feedback'
import type { Cart } from '../types/api'
import { clearCart, getCart, removeCartItem, updateCartItem } from '../services/cartService'
import { toAppError } from '../services/errors'
import { formatCurrency } from '../utils/format'

export function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState(true)
  const [busyItem, setBusyItem] = useState<number | null>(null)
  const [error, setError] = useState('')

  async function loadCart() {
    setError('')
    try {
      setCart(await getCart())
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadCart() }, [])

  async function changeQuantity(itemId: number, quantity: number) {
    setBusyItem(itemId)
    setError('')
    try {
      setCart(await updateCartItem(itemId, quantity))
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setBusyItem(null)
    }
  }

  async function removeItem(itemId: number) {
    setBusyItem(itemId)
    setError('')
    try {
      await removeCartItem(itemId)
      await loadCart()
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setBusyItem(null)
    }
  }

  async function handleClearCart() {
    setError('')
    try {
      await clearCart()
      await loadCart()
    } catch (requestError) {
      setError(toAppError(requestError).message)
    }
  }

  if (loading) return <LoadingState label="Loading your cart…" />
  if (error && !cart) return <div className="page-stack"><ErrorNotice>{error}</ErrorNotice><Link to="/" className="text-link">Browse the collection</Link></div>

  return (
    <div className="page-stack">
      <div className="page-heading">
        <div><span className="eyebrow">Saved for later? Not quite.</span><h1>Your cart</h1></div>
        <Link className="text-link" to="/">Continue browsing →</Link>
      </div>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {!cart?.cartItems.length ? (
        <EmptyState title="Your cart is taking a little break">
          Find something you love and it’ll show up here.
          <div className="empty-action"><Link className="button button--dark" to="/">Browse the collection</Link></div>
        </EmptyState>
      ) : (
        <div className="cart-layout">
          <section className="cart-items surface">
            {cart.cartItems.map((item) => (
              <article className="cart-item" key={item.id}>
                <div className="cart-item-monogram" aria-hidden="true">{item.productName.slice(0, 1).toUpperCase()}</div>
                <div className="cart-item-copy">
                  <Link to={`/products/${item.productId}`} className="cart-item-title">{item.productName}</Link>
                  <span className="muted">{formatCurrency(item.price)} each</span>
                </div>
                <div className="quantity-control" aria-label={`Quantity for ${item.productName}`}>
                  <button type="button" aria-label="Decrease quantity" disabled={busyItem === item.id || item.quantity <= 1}
                    onClick={() => changeQuantity(item.id, item.quantity - 1)}>−</button>
                  <span>{item.quantity}</span>
                  <button type="button" aria-label="Increase quantity" disabled={busyItem === item.id}
                    onClick={() => changeQuantity(item.id, item.quantity + 1)}>+</button>
                </div>
                <strong className="cart-line-total">{formatCurrency(item.lineTotal)}</strong>
                <button className="remove-link" type="button" disabled={busyItem === item.id}
                  onClick={() => removeItem(item.id)}>Remove</button>
              </article>
            ))}
            <button className="text-link clear-cart" type="button" onClick={handleClearCart}>Clear cart</button>
          </section>
          <aside className="order-summary surface">
            <span className="eyebrow">The little details</span>
            <h2>Order summary</h2>
            <div className="summary-row"><span>Subtotal</span><strong>{formatCurrency(cart.totalAmount)}</strong></div>
            <div className="summary-row"><span>Shipping</span><span className="muted">Calculated at checkout</span></div>
            <div className="summary-total"><span>Total</span><strong>{formatCurrency(cart.totalAmount)}</strong></div>
            <Link className="button button--dark button--wide" to="/checkout">Continue to checkout <span aria-hidden="true">→</span></Link>
            <p className="summary-note">Your order is confirmed only after checkout.</p>
          </aside>
        </div>
      )}
    </div>
  )
}
