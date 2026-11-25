import { useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { EmptyState, ErrorNotice, LoadingState, SuccessNotice } from '../components/Feedback'
import { Pagination } from '../components/Pagination'
import { ProductCard } from '../components/ProductCard'
import type { Category, Product, ProductPage } from '../types/api'
import { addCartItem } from '../services/cartService'
import { getCategories } from '../services/categoryService'
import { toAppError } from '../services/errors'
import { getProducts } from '../services/productService'

interface Filters {
  name: string
  categoryId: string
  minPrice: string
  maxPrice: string
  sortBy: 'id' | 'name' | 'price'
  direction: 'asc' | 'desc'
}

const initialFilters: Filters = {
  name: '',
  categoryId: '',
  minPrice: '',
  maxPrice: '',
  sortBy: 'id',
  direction: 'asc',
}

export function ProductsPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [categories, setCategories] = useState<Category[]>([])
  const [filters, setFilters] = useState<Filters>(initialFilters)
  const [appliedFilters, setAppliedFilters] = useState<Filters>(initialFilters)
  const [products, setProducts] = useState<ProductPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    const query = {
      name: appliedFilters.name || undefined,
      categoryId: appliedFilters.categoryId ? Number(appliedFilters.categoryId) : undefined,
      minPrice: appliedFilters.minPrice ? Number(appliedFilters.minPrice) : undefined,
      maxPrice: appliedFilters.maxPrice ? Number(appliedFilters.maxPrice) : undefined,
      page,
      size: 12,
      sortBy: appliedFilters.sortBy,
      direction: appliedFilters.direction,
    }

    Promise.all([getProducts(query), getCategories()])
      .then(([productPage, categoryList]) => {
        if (!active) return
        setProducts(productPage)
        setCategories(categoryList)
      })
      .catch((requestError: unknown) => {
        if (active) setError(toAppError(requestError).message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [appliedFilters, page])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setPage(0)
    setNotice('')
    setAppliedFilters({ ...filters, name: filters.name.trim() })
  }

  async function handleAdd(product: Product) {
    if (user?.role === 'ADMIN') {
      setError('Customer cart features are available to customer accounts.')
      return
    }
    if (!user) {
      navigate('/login', { state: { from: location } })
      return
    }
    setError('')
    setNotice('')
    try {
      await addCartItem(product.id)
      setNotice(`${product.name} was added to your cart.`)
    } catch (requestError) {
      setError(toAppError(requestError).message)
    }
  }

  return (
    <div className="page-stack">
      <section className="hero-panel">
        <div className="hero-copy">
          <span className="eyebrow eyebrow--light">The StoreStack edit</span>
          <h1>Small discoveries.<br /><em>Big everyday joy.</em></h1>
          <p>Thoughtful picks for the spaces, routines, and little moments that make a day yours.</p>
          <a className="button button--cream" href="#collection">Explore the collection <span aria-hidden="true">↓</span></a>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-sun" />
          <div className="hero-orbit hero-orbit--one" />
          <div className="hero-orbit hero-orbit--two" />
          <div className="hero-cube"><span>S</span></div>
          <span className="hero-art-note">GOOD<br />THINGS<br />INSIDE</span>
        </div>
        <span className="hero-index">CURATED FOR YOU · NO. 01</span>
      </section>

      <section className="collection-section" id="collection">
        <div className="section-heading">
          <div>
            <span className="eyebrow">Find your something</span>
            <h2>Made for your everyday</h2>
          </div>
          <span className="section-count">{products?.totalElements ?? 0} items to explore</span>
        </div>

        <form className="filter-panel" onSubmit={applyFilters}>
          <label className="filter-search">
            <span className="sr-only">Search products</span>
            <span className="search-icon" aria-hidden="true">⌕</span>
            <input
              aria-label="Search products"
              placeholder="Try “desk lamp” or “weekend bag”"
              value={filters.name}
              onChange={(event) => setFilters({ ...filters, name: event.target.value })}
            />
          </label>
          <label className="filter-control">
            <span>Category</span>
            <select value={filters.categoryId} onChange={(event) => setFilters({ ...filters, categoryId: event.target.value })}>
              <option value="">All categories</option>
              {categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
            </select>
          </label>
          <label className="filter-control">
            <span>Price from</span>
            <input type="number" min="0" step="0.01" value={filters.minPrice}
              onChange={(event) => setFilters({ ...filters, minPrice: event.target.value })} placeholder="$0" />
          </label>
          <label className="filter-control">
            <span>Price to</span>
            <input type="number" min="0" step="0.01" value={filters.maxPrice}
              onChange={(event) => setFilters({ ...filters, maxPrice: event.target.value })} placeholder="Any" />
          </label>
          <label className="filter-control filter-control--sort">
            <span>Sort by</span>
            <select
              value={`${filters.sortBy}:${filters.direction}`}
              onChange={(event) => {
                const [sortBy, direction] = event.target.value.split(':') as [Filters['sortBy'], Filters['direction']]
                setFilters({ ...filters, sortBy, direction })
              }}
            >
              <option value="id:asc">Featured</option>
              <option value="name:asc">Name, A–Z</option>
              <option value="price:asc">Price, low to high</option>
              <option value="price:desc">Price, high to low</option>
            </select>
          </label>
          <button className="button button--dark filter-submit" type="submit">Apply filters</button>
        </form>

        {error ? <ErrorNotice>{error}</ErrorNotice> : null}
        {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
        {loading ? <LoadingState label="Finding your next favorite…" /> : null}
        {!loading && !error && products?.empty ? (
          <EmptyState title="Nothing in this collection just yet">
            Try a different search or clear a filter to see more finds.
          </EmptyState>
        ) : null}
        {!loading && !error && products && products.content.length > 0 ? (
          <>
            <div className="product-grid">
              {products.content.map((product) => (
                <ProductCard key={product.id} product={product} onAdd={handleAdd} />
              ))}
            </div>
            <Pagination page={page} totalPages={products.totalPages} onPageChange={setPage} />
          </>
        ) : null}
      </section>
    </div>
  )
}
