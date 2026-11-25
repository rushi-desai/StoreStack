import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'
import { ErrorNotice } from '../components/Feedback'
import { toAppError } from '../services/errors'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
    address: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  function updateField(name: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' })
      return
    }

    setSubmitting(true)
    try {
      const message = await register({
        name: form.name,
        email: form.email,
        password: form.password,
        phone: form.phone,
        address: form.address,
      })
      navigate('/login', { replace: true, state: { notice: message } })
    } catch (requestError) {
      const appError = toAppError(requestError)
      setError(appError.message)
      setFieldErrors(appError.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-layout auth-layout--register">
      <div className="auth-aside">
        <span className="eyebrow eyebrow--light">Make room for good finds</span>
        <h1>Your next favorite starts here.</h1>
        <p>Create an account to save time at checkout and keep every order in one place.</p>
        <div className="auth-aside-stamp">SE<br />02</div>
      </div>
      <div className="auth-card">
        <span className="eyebrow">Join the good list</span>
        <h2>Create your account</h2>
        <p className="muted">A few details and you’re all set.</p>
        {error ? <ErrorNotice>{error}</ErrorNotice> : null}
        <form className="form-stack" onSubmit={handleSubmit}>
          <Field label="Full name" name="name" autoComplete="name" required value={form.name}
            error={fieldErrors.name} onChange={(event) => updateField('name', event.target.value)} />
          <Field label="Email address" name="email" type="email" autoComplete="email" required value={form.email}
            error={fieldErrors.email} onChange={(event) => updateField('email', event.target.value)} />
          <div className="form-row">
            <Field label="Phone" name="phone" type="tel" autoComplete="tel" required value={form.phone}
              error={fieldErrors.phone} onChange={(event) => updateField('phone', event.target.value)} />
            <Field label="Address" name="address" autoComplete="street-address" required value={form.address}
              error={fieldErrors.address} onChange={(event) => updateField('address', event.target.value)} />
          </div>
          <div className="form-row">
            <Field label="Password" name="password" type="password" autoComplete="new-password"
              minLength={8} required value={form.password} error={fieldErrors.password}
              onChange={(event) => updateField('password', event.target.value)} />
            <Field label="Confirm password" name="confirmPassword" type="password" autoComplete="new-password"
              required value={form.confirmPassword} error={fieldErrors.confirmPassword}
              onChange={(event) => updateField('confirmPassword', event.target.value)} />
          </div>
          <p className="field-hint">Use at least 8 characters, including an uppercase letter, a lowercase letter, and a number.</p>
          <button className="button button--dark button--wide" type="submit" disabled={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
      </div>
    </section>
  )
}
