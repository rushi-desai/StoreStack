import { useEffect, useState, type FormEvent } from 'react'
import { EmptyState, ErrorNotice, FieldError, LoadingState, SuccessNotice } from '../../components/Feedback'
import { Field } from '../../components/Field'
import type { Category, CategoryRequest } from '../../types/api'
import { createCategory, deleteCategory, getCategories, updateCategory } from '../../services/categoryService'
import { toAppError } from '../../services/errors'

export function AdminCategoriesPanel() {
  const [categories, setCategories] = useState<Category[]>([])
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  async function loadCategories() {
    setError('')
    try {
      setCategories(await getCategories())
    } catch (requestError) {
      setError(toAppError(requestError).message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadCategories() }, [])

  function resetForm() {
    setName('')
    setDescription('')
    setEditingId(null)
    setFieldErrors({})
  }

  function startEditing(category: Category) {
    setEditingId(category.id)
    setName(category.name)
    setDescription(category.description ?? '')
    setFieldErrors({})
    document.getElementById('admin-category-form')?.scrollIntoView({ behavior: 'smooth' })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setNotice('')
    setFieldErrors({})
    const request: CategoryRequest = { name: name.trim(), description: description.trim() || null }
    try {
      if (editingId === null) {
        await createCategory(request)
        setNotice('Category created.')
      } else {
        await updateCategory(editingId, request)
        setNotice('Category updated.')
      }
      resetForm()
      await loadCategories()
    } catch (requestError) {
      const appError = toAppError(requestError)
      setError(appError.message)
      setFieldErrors(appError.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(category: Category) {
    setError('')
    setNotice('')
    try {
      await deleteCategory(category.id)
      setCategories((current) => current.filter((item) => item.id !== category.id))
      if (editingId === category.id) resetForm()
      setNotice(`${category.name} was deleted.`)
    } catch (requestError) {
      setError(toAppError(requestError).message)
    }
  }

  if (loading) return <LoadingState label="Loading categories…" />

  return (
    <section className="admin-content">
      <div className="admin-section-heading">
        <div><span className="eyebrow">Organization</span><h2>Categories</h2></div>
        <span className="section-count">{categories.length} categories</span>
      </div>
      {error ? <ErrorNotice>{error}</ErrorNotice> : null}
      {notice ? <SuccessNotice>{notice}</SuccessNotice> : null}
      <div className="admin-category-layout">
        <form className="surface admin-form form-stack" id="admin-category-form" onSubmit={handleSubmit}>
          <div className="admin-form-heading">
            <span className="eyebrow">{editingId === null ? 'Make a new shelf' : `Editing category #${editingId}`}</span>
            <h3>{editingId === null ? 'Add a category' : 'Edit category'}</h3>
          </div>
          <Field label="Category name" name="name" required maxLength={100} value={name}
            error={fieldErrors.name} onChange={(event) => setName(event.target.value)} />
          <div className="field">
            <label htmlFor="categoryDescription">Description</label>
            <textarea id="categoryDescription" name="description" maxLength={500} rows={4}
              value={description} aria-invalid={Boolean(fieldErrors.description)}
              onChange={(event) => setDescription(event.target.value)} />
            <FieldError>{fieldErrors.description}</FieldError>
          </div>
          <div className="form-actions">
            <button className="button button--dark" type="submit" disabled={submitting}>
              {submitting ? 'Saving…' : editingId === null ? 'Add category' : 'Save changes'}
            </button>
            {editingId !== null ? <button className="button button--outline" type="button" onClick={resetForm}>Cancel edit</button> : null}
          </div>
        </form>
        <div className="admin-category-list">
          {!categories.length ? (
            <EmptyState title="No categories yet">Add a category to start organizing your products.</EmptyState>
          ) : categories.map((category, index) => (
            <article className="admin-category-row surface" key={category.id}>
              <span className="category-index">0{index + 1}</span>
              <div className="admin-category-info">
                <strong>{category.name}</strong>
                <span>{category.description || 'No description'}</span>
              </div>
              <button className="button button--outline button--small" type="button" onClick={() => startEditing(category)}>Edit</button>
              <button className="button button--danger button--small" type="button" onClick={() => handleDelete(category)}>Delete</button>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
