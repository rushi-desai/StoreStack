import type { InputHTMLAttributes } from 'react'
import { FieldError } from './Feedback'

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export function Field({ label, error, id, ...inputProps }: FieldProps) {
  const fieldId = id ?? inputProps.name
  return (
    <div className="field">
      <label htmlFor={fieldId}>{label}</label>
      <input id={fieldId} aria-invalid={Boolean(error)} {...inputProps} />
      <FieldError>{error}</FieldError>
    </div>
  )
}
