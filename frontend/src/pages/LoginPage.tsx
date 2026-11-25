import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'
import { ErrorNotice, SuccessNotice } from '../components/Feedback'
import { toAppError } from '../services/errors'

interface LoginLocationState {
  from?: { pathname?: string }
  notice?: string
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LoginLocationState | null
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setFieldErrors({})
    try {
      await login(email, password)
      navigate(state?.from?.pathname ?? '/', { replace: true })
    } catch (requestError) {
      const appError = toAppError(requestError)
      setError(appError.message)
      setFieldErrors(appError.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="auth-layout">
      <div className="auth-aside">
        <span className="eyebrow eyebrow--light">Good things, well found</span>
        <h1>A little more joy in every day.</h1>
        <p>Sign in to pick up where you left off and keep your favorite finds close.</p>
        <div className="auth-aside-stamp">SS<br />01</div>
      </div>
      <div className="auth-card">
        <span className="eyebrow">Welcome back</span>
        <h2>Sign in to StoreStack</h2>
        <p className="muted">Your next favorite thing is just around the corner.</p>
        {state?.notice ? <SuccessNotice>{state.notice}</SuccessNotice> : null}
        {error ? <ErrorNotice>{error}</ErrorNotice> : null}
        <form className="form-stack" onSubmit={handleSubmit}>
          <Field
            label="Email address"
            name="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            error={fieldErrors.email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <Field
            label="Password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            error={fieldErrors.password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <button className="button button--dark button--wide" type="submit" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="auth-switch">New to StoreStack? <Link to="/register">Create an account</Link></p>
      </div>
    </section>
  )
}
