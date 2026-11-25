import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { EmptyState, ErrorNotice, LoadingState, SuccessNotice } from '../components/Feedback'
import type { Order } from '../types/api'
import { toAppError } from '../services/errors'
import { getMyOrders } from '../services/orderService'
import { formatCurrency } from '../utils/format'

interface OrdersLocationState {
  notice?: string
}

export function OrdersPage() {
  const location = useLocation()
  const notice = (location.state as OrdersLocationState | null)?.notice
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyOrders()
      .then(setOrders)
      .catch((requestError: unknown) => setError(toAppError(requestError).message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState label="Gathering your orders…" />

  return (
    <div className="page-stack">
      <div className="page-heading"><div><span className="eyebrow">The story so far</span><h1>Your orders</h1></div></div>
      {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {!error && orders.length === 0 ? (
        <EmptyState title="No orders just yet">
          Your next good find is waiting.
          <div className="empty-action"><Link className="button button--dark" to="/">Explore the collection</Link></div>
        </EmptyState>
      ) : null}
      {!error && orders.length > 0 ? (
        <div className="order-list">
          {orders.map((order) => (
            <Link className="order-list-card surface" to={`/orders/${order.id}`} key={order.id}>
              <div>
                <span className="eyebrow">Order {order.orderNumber}</span>
                <strong>{order.orderItems.length} {order.orderItems.length === 1 ? 'item' : 'items'}</strong>
              </div>
              <span className={`status-pill status-pill--${order.status.toLowerCase()}`}>{order.status}</span>
              <strong>{formatCurrency(order.totalAmount)}</strong>
              <span className="order-arrow" aria-hidden="true">↗</span>
            </Link>
          ))}
        </div>
      ) : null}
    </div>
  )
}
