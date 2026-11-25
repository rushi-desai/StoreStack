import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState, ErrorNotice, LoadingState, SuccessNotice } from '../../components/Feedback'
import type { Order, OrderStatus } from '../../types/api'
import { toAppError } from '../../services/errors'
import { getAllOrders, updateOrderStatus } from '../../services/adminOrderService'
import { formatCurrency } from '../../utils/format'

const nextStatuses: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['DELIVERED'],
  DELIVERED: [],
  CANCELLED: [],
}

export function AdminOrderManagementPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [selectedStatuses, setSelectedStatuses] = useState<Record<number, OrderStatus>>({})
  const [loading, setLoading] = useState(true)
  const [busyOrderId, setBusyOrderId] = useState<number | null>(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    getAllOrders()
      .then((data) => { if (active) setOrders(data) })
      .catch((requestError: unknown) => { if (active) setError(toAppError(requestError).message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [])

  async function handleStatusChange(order: Order, status: OrderStatus) {
    setBusyOrderId(order.id)
    setError('')
    setNotice('')
    try {
      const updated = await updateOrderStatus(order.id, status)
      setOrders((current) => current.map((item) => item.id === order.id ? updated : item))
      setSelectedStatuses((current) => ({ ...current, [order.id]: updated.status }))
      setNotice(`Order ${updated.orderNumber} is now ${updated.status.toLowerCase()}.`)
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setBusyOrderId(null)
    }
  }

  if (loading) return <LoadingState label="Loading customer orders…" />

  return (
    <section className="admin-content">
      <div className="admin-section-heading">
        <div><span className="eyebrow">Fulfillment</span><h2>Customer orders</h2></div>
        <span className="section-count">{orders.length} orders</span>
      </div>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
      {!orders.length && !error ? (
        <EmptyState title="No orders to pack yet">New orders will show up here.</EmptyState>
      ) : null}
      <div className="admin-order-list">
        {orders.map((order) => {
          const options = nextStatuses[order.status]
          const selected = selectedStatuses[order.id] ?? order.status
          return (
            <article className="admin-order-card surface" key={order.id}>
              <div className="admin-order-topline">
                <div>
                  <span className="eyebrow">{order.orderNumber}</span>
                  <h3>Order #{order.id}</h3>
                </div>
                <span className={`status-pill status-pill--${order.status.toLowerCase()}`}>{order.status}</span>
              </div>
              <div className="summary-row"><span>Customer account</span><strong>#{order.userId}</strong></div>
              <div className="summary-row"><span>{order.orderItems.length} line items</span><strong>{formatCurrency(order.totalAmount)}</strong></div>
              <div className="admin-order-items">
                {order.orderItems.map((item) => (
                  <div className="summary-row" key={item.id}>
                    <Link to={`/products/${item.productId}`}>Product #{item.productId} × {item.quantity}</Link>
                    <span>{formatCurrency(item.price * item.quantity)}</span>
                  </div>
                ))}
              </div>
              {options.length ? (
                <div className="admin-status-form">
                  <label className="filter-control" htmlFor={`status-${order.id}`}>
                    <span>Move order to</span>
                    <select id={`status-${order.id}`} value={selected}
                      disabled={busyOrderId === order.id}
                      onChange={(event) => setSelectedStatuses((current) => ({
                        ...current,
                        [order.id]: event.target.value as OrderStatus,
                      }))}>
                      <option value={order.status}>{order.status}</option>
                      {options.map((status) => <option value={status} key={status}>{status}</option>)}
                    </select>
                  </label>
                  <button className="button button--dark" type="button"
                    disabled={busyOrderId === order.id || selected === order.status}
                    onClick={() => handleStatusChange(order, selected)}>
                    {busyOrderId === order.id ? 'Saving…' : 'Update status'}
                  </button>
                </div>
              ) : <p className="summary-note">This order is in its final status.</p>}
            </article>
          )
        })}
      </div>
    </section>
  )
}
