import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ErrorNotice, LoadingState, SuccessNotice } from '../components/Feedback'
import type { Product } from '../types/api'
import { addCartItem } from '../services/cartService'
import { toAppError } from '../services/errors'
import { getProduct } from '../services/productService'
import { formatCurrency } from '../utils/format'

export function ProductDetailPage() {
  const { productId } = useParams()
  const id = Number(productId)
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    if (!Number.isInteger(id) || id < 1) {
      setError('That product could not be found.')
      setLoading(false)
      return
    }
    getProduct(id)
      .then((result) => { if (active) setProduct(result) })
      .catch((requestError: unknown) => { if (active) setError(toAppError(requestError).message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [id])

  async function handleAdd() {
    if (!product) return
    if (user?.role === 'ADMIN') {
      setError('Customer cart features are available to customer accounts.')
      return
    }
    if (!user) {
      navigate('/login', { state: { from: location } })
      return
    }
    setError('')
    try {
      await addCartItem(product.id)
      setNotice(`${product.name} was added to your cart.`)
    } catch (requestError) {
      setError(toAppError(requestError).message)
    }
  }

  if (loading) return <LoadingState label="Loading product…" />
  if (error && !product) return <div className="page-stack"><ErrorNotice>{error}</ErrorNotice><Link to="/" className="text-link">Back to collection</Link></div>
  if (!product) return null

  return (
    <div className="page-stack">
      <Link to="/" className="back-link">← Back to collection</Link>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
      <article className="detail-layout">
        <div className="detail-image">
          {product.imageUrl
            ? <img src={product.imageUrl} alt={product.name} />
            : <div className="detail-image-placeholder">{product.name.slice(0, 1).toUpperCase()}</div>}
          <span className="detail-image-caption">A StoreStack everyday essential</span>
        </div>
        <div className="detail-copy">
          <span className="eyebrow">Thoughtfully selected</span>
          <h1>{product.name}</h1>
          <p className="detail-price">{formatCurrency(product.price)}</p>
          <p className="detail-description">{product.description}</p>
          <div className="detail-stock">
            <span className={product.stock > 0 ? 'stock-dot' : 'stock-dot stock-dot--out'} />
            {product.stock > 0 ? `${product.stock} available` : 'Currently out of stock'}
          </div>
          <button
            className="button button--dark button--wide"
            type="button"
            disabled={product.stock < 1}
            onClick={handleAdd}
          >
            Add to cart <span aria-hidden="true">↗</span>
          </button>
          <div className="detail-promise">
            <span>01</span>
            <p>Good design, honest details, and everyday usefulness. That’s our kind of find.</p>
          </div>
        </div>
      </article>
    </div>
  )
}
