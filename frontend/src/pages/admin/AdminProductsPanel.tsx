import { useEffect, useState, type FormEvent } from 'react'
import { EmptyState, ErrorNotice, FieldError, LoadingState, SuccessNotice } from '../../components/Feedback'
import { Field } from '../../components/Field'
import type { Category, Product, ProductRequest } from '../../types/api'
import { getCategories } from '../../services/categoryService'
import { toAppError } from '../../services/errors'
import { createProduct, deleteProduct, getProducts, updateProduct } from '../../services/productService'
import { formatCurrency } from '../../utils/format'

interface ProductFormState {
  name: string
  description: string
  price: string
  imageUrl: string
  stock: string
  categoryId: string
}

const emptyForm: ProductFormState = {
  name: '',
  description: '',
  price: '',
  imageUrl: '',
  stock: '0',
  categoryId: '',
}

export function AdminProductsPanel() {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [form, setForm] = useState<ProductFormState>(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function loadData() {
    setError('')
    try {
      const [productPage, categoryList] = await Promise.all([
        getProducts({ page: 0, size: 100, sortBy: 'name', direction: 'asc' }),
        getCategories(),
      ])
      setProducts(productPage.content)
      setCategories(categoryList)
      setForm((current) => current.categoryId
        ? current
        : { ...current, categoryId: categoryList[0] ? String(categoryList[0].id) : '' })
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadData() }, [])

  function resetForm() {
    setForm({ ...emptyForm, categoryId: categories[0] ? String(categories[0].id) : '' })
    setEditingId(null)
    setFieldErrors({})
  }

  function startEditing(product: Product) {
    setEditingId(product.id)
    setFieldErrors({})
    setForm({
      name: product.name,
      description: product.description,
      price: String(product.price),
      imageUrl: product.imageUrl ?? '',
      stock: String(product.stock),
      categoryId: String(product.categoryId),
    })
    document.getElementById('admin-product-form')?.scrollIntoView({ behavior: 'smooth' })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setNotice('')
    setFieldErrors({})
    const request: ProductRequest = {
      name: form.name.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      imageUrl: form.imageUrl.trim() || null,
      stock: Number(form.stock),
      categoryId: Number(form.categoryId),
    }
    try {
      if (editingId === null) {
        await createProduct(request)
        setNotice('Product added to the catalog.')
      } else {
        await updateProduct(editingId, request)
        setNotice('Product details saved.')
      }
      resetForm()
      await loadData()
    } catch (requestError) {
      const appError = toAppError(requestError)
      setError(appError.message)
      setFieldErrors(appError.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(product: Product) {
    setError('')
    setNotice('')
    try {
      await deleteProduct(product.id)
      setProducts((current) => current.filter((item) => item.id !== product.id))
      if (editingId === product.id) resetForm()
      setNotice(`${product.name} was removed.`)
    } catch (requestError) {
      setError(toAppError(requestError).message)
    }
  }

  if (loading) return <LoadingState label="Loading your catalog…" />

  return (
    <section className="admin-content">
      <div className="admin-section-heading">
        <div><span className="eyebrow">Catalog</span><h2>Products</h2></div>
        <span className="section-count">{products.length} shown · up to 100</span>
      </div>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
      {categories.length === 0 ? (
        <ErrorNotice>Create a category before adding products.</ErrorNotice>
      ) : null}
      <div className="admin-product-layout">
        <form className="surface admin-form form-stack" id="admin-product-form" onSubmit={handleSubmit}>
          <div className="admin-form-heading">
            <span className="eyebrow">{editingId === null ? 'New arrival' : `Editing product #${editingId}`}</span>
            <h3>{editingId === null ? 'Add a product' : 'Edit product'}</h3>
          </div>
          <Field label="Product name" name="name" required maxLength={150} value={form.name}
            error={fieldErrors.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <div className="field">
            <label htmlFor="description">Description</label>
            <textarea id="description" name="description" required maxLength={1000} rows={3}
              value={form.description} aria-invalid={Boolean(fieldErrors.description)}
              onChange={(event) => setForm({ ...form, description: event.target.value })} />
            <FieldError>{fieldErrors.description}</FieldError>
          </div>
          <div className="form-row">
            <Field label="Price" name="price" type="number" min="0.01" step="0.01" required value={form.price}
              error={fieldErrors.price} onChange={(event) => setForm({ ...form, price: event.target.value })} />
            <Field label="Stock" name="stock" type="number" min="0" step="1" required value={form.stock}
              error={fieldErrors.stock} onChange={(event) => setForm({ ...form, stock: event.target.value })} />
          </div>
          <div className="field">
            <label htmlFor="categoryId">Category</label>
            <select id="categoryId" name="categoryId" required value={form.categoryId}
              aria-invalid={Boolean(fieldErrors.categoryId)}
              onChange={(event) => setForm({ ...form, categoryId: event.target.value })}>
              <option value="">Choose a category</option>
              {categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
            </select>
            <FieldError>{fieldErrors.categoryId}</FieldError>
          </div>
          <Field label="Image URL (optional)" name="imageUrl" type="url" maxLength={500}
            value={form.imageUrl} error={fieldErrors.imageUrl}
            onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} />
          <div className="form-actions">
            <button className="button button--dark" type="submit" disabled={submitting || categories.length === 0}>
              {submitting ? 'Saving…' : editingId === null ? 'Add product' : 'Save changes'}
            </button>
            {editingId !== null ? <button className="button button--outline" type="button" onClick={resetForm}>Cancel edit</button> : null}
          </div>
        </form>

        <div className="admin-product-list">
          {!products.length ? (
            <EmptyState title="Your catalog is ready for its first find">Use the form to add a product.</EmptyState>
          ) : products.map((product) => (
            <article className="admin-product-row surface" key={product.id}>
              <div className="admin-product-monogram">{product.name.slice(0, 1).toUpperCase()}</div>
              <div className="admin-product-info">
                <strong>{product.name}</strong>
                <span>{formatCurrency(product.price)} · {product.stock} in stock · Category {product.categoryId}</span>
              </div>
              <button className="button button--outline button--small" type="button" onClick={() => startEditing(product)}>Edit</button>
              <button className="button button--danger button--small" type="button" onClick={() => handleDelete(product)}>Delete</button>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
