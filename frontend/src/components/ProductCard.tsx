import { Link } from 'react-router-dom'
import type { Product } from '../types/api'
import { formatCurrency } from '../utils/format'

export function ProductCard({
  product,
  onAdd,
}: {
  product: Product
  onAdd?: (product: Product) => void
}) {
  return (
    <article className="product-card">
      <Link className="product-image-wrap" to={`/products/${product.id}`}>
        {product.imageUrl ? (
          <img className="product-image" src={product.imageUrl} alt={product.name} loading="lazy" />
        ) : (
          <div className="product-image-placeholder" aria-hidden="true">
            <span>{product.name.slice(0, 1).toUpperCase()}</span>
          </div>
        )}
        {product.stock === 0 ? <span className="image-badge">Sold out</span> : null}
      </Link>
      <div className="product-card-content">
        <div className="product-card-meta">
          <span className="eyebrow">StoreStack pick</span>
          <span className="product-stock">{product.stock > 0 ? 'In stock' : 'Unavailable'}</span>
        </div>
        <Link className="product-title" to={`/products/${product.id}`}>{product.name}</Link>
        <p className="product-description">{product.description}</p>
        <div className="product-card-bottom">
          <strong className="price">{formatCurrency(product.price)}</strong>
          {onAdd ? (
            <button
              className="icon-button"
              type="button"
              aria-label={`Add ${product.name} to cart`}
              disabled={product.stock < 1}
              onClick={() => onAdd(product)}
            >
              +
            </button>
          ) : null}
        </div>
      </div>
    </article>
  )
}
