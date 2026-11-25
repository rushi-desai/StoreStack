import axios from 'axios'
import type { ApiErrorResponse } from '../types/api'

export class AppError extends Error {
  readonly fieldErrors: Record<string, string>
  readonly status?: number

  constructor(message: string, fieldErrors: Record<string, string> = {}, status?: number) {
    super(message)
    this.name = 'AppError'
    this.fieldErrors = fieldErrors
    this.status = status
  }
}

export function toAppError(error: unknown): AppError {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const response = error.response
    return new AppError(
      response?.data.message || error.message || 'The request could not be completed.',
      response?.data.fieldErrors ?? {},
      response?.status,
    )
  }
  if (error instanceof Error) return new AppError(error.message)
  return new AppError('The request could not be completed.')
}
