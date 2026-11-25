import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { EmptyState, ErrorNotice, LoadingState } from '../components/Feedback'
import type { Cart } from '../types/api'
import { getCart } from '../services/cartService'
import { toAppError } from '../services/errors'
import { placeOrder } from '../services/orderService'
import { formatCurrency } from '../utils/format'

export function CheckoutPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    getCart()
      .then(setCart)
      .catch((requestError: unknown) => setError(toAppError(requestError).message))
      .finally(() => setLoading(false))
  }, [])

  async function handleCheckout() {
    setSubmitting(true)
    setError('')
    try {
      const notice = await placeOrder()
      navigate('/orders', { replace: true, state: { notice } })
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingState label="Preparing checkout…" />
  if (error && !cart) return <div className="page-stack"><ErrorNotice>{error}</ErrorNotice><Link to="/cart" className="text-link">Back to cart</Link></div>
  if (!cart?.cartItems.length) {
    return (
      <EmptyState title="There’s nothing to check out yet">
        Add a few finds to your cart first.
        <div className="empty-action"><Link className="button button--dark" to="/">Browse the collection</Link></div>
      </EmptyState>
    )
  }

  return (
    <div className="page-stack narrow-page">
      <Link to="/cart" className="back-link">← Back to cart</Link>
      <div className="page-heading"><div><span className="eyebrow">One last look</span><h1>Review your order</h1></div></div>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      <section className="surface checkout-card">
        <div className="checkout-items">
          {cart.cartItems.map((item) => (
            <div className="summary-row" key={item.id}>
              <span>{item.productName} <span className="muted">× {item.quantity}</span></span>
              <strong>{formatCurrency(item.lineTotal)}</strong>
            </div>
          ))}
        </div>
        <div className="summary-total"><span>Order total</span><strong>{formatCurrency(cart.totalAmount)}</strong></div>
        <p className="summary-note">Inventory is checked again when you place the order.</p>
        <button className="button button--dark button--wide" type="button" disabled={submitting} onClick={handleCheckout}>
          {submitting ? 'Placing order…' : 'Place order'} <span aria-hidden="true">→</span>
        </button>
      </section>
    </div>
  )
}
