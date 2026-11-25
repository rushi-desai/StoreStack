import type { ReactNode } from 'react'

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="state-card" role="status">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="state-card state-card--empty">
      <span className="state-mark" aria-hidden="true">—</span>
      <h2>{title}</h2>
      {children ? <div className="muted">{children}</div> : null}
    </div>
  )
}

export function ErrorNotice({ children }: { children: ReactNode }) {
  return <div className="notice notice--error" role="alert">{children}</div>
}

export function SuccessNotice({ children }: { children: ReactNode }) {
  return <div className="notice notice--success" role="status">{children}</div>
}

export function FieldError({ children }: { children?: string }) {
  return children ? <span className="field-error">{children}</span> : null
}
