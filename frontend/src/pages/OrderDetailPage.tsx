import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ErrorNotice, LoadingState, SuccessNotice } from '../components/Feedback'
import type { Order } from '../types/api'
import { toAppError } from '../services/errors'
import { cancelOrder, getOrder } from '../services/orderService'
import { formatCurrency } from '../utils/format'

export function OrderDetailPage() {
  const { orderId } = useParams()
  const id = Number(orderId)
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (!Number.isInteger(id) || id < 1) {
      setError('That order could not be found.')
      setLoading(false)
      return
    }
    getOrder(id)
      .then(setOrder)
      .catch((requestError: unknown) => setError(toAppError(requestError).message))
      .finally(() => setLoading(false))
  }, [id])

  async function handleCancel() {
    setSubmitting(true)
    setError('')
    setNotice('')
    try {
      setOrder(await cancelOrder(id))
      setNotice('Your order has been cancelled.')
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <LoadingState label="Loading order details…" />
  if (!order) return <div className="page-stack"><ErrorNotice>{error || 'That order could not be found.'}</ErrorNotice><Link to="/orders" className="text-link">Back to orders</Link></div>

  const canCancel = order.status === 'PENDING' || order.status === 'CONFIRMED'
  return (
    <div className="page-stack narrow-page">
      <Link to="/orders" className="back-link">← Back to orders</Link>
      <div className="page-heading">
        <div><span className="eyebrow">Order {order.orderNumber}</span><h1>Order details</h1></div>
        <span className={`status-pill status-pill--${order.status.toLowerCase()}`}>{order.status}</span>
      </div>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
      <section className="surface checkout-card">
        <div className="checkout-items">
          {order.orderItems.map((item) => (
            <div className="summary-row" key={item.id}>
              <span>Product #{item.productId} <span className="muted">× {item.quantity}</span></span>
              <strong>{formatCurrency(item.price * item.quantity)}</strong>
            </div>
          ))}
        </div>
        <div className="summary-total"><span>Total paid</span><strong>{formatCurrency(order.totalAmount)}</strong></div>
        {canCancel ? (
          <button className="button button--danger button--wide" type="button" disabled={submitting} onClick={handleCancel}>
            {submitting ? 'Cancelling…' : 'Cancel order'}
          </button>
        ) : (
          <p className="summary-note">This order can no longer be cancelled.</p>
        )}
      </section>
    </div>
  )
}
