import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error.model';

export function apiErrorMessage(
  error: unknown,
  fallback = 'Something went wrong. Please try again.',
): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  const body = apiErrorBody(error);
  return body?.message || (error.status === 0 ? 'Unable to connect to the server.' : fallback);
}

export function apiFieldErrors(error: unknown): Record<string, string> {
  return error instanceof HttpErrorResponse ? (apiErrorBody(error)?.fieldErrors ?? {}) : {};
}

function apiErrorBody(error: HttpErrorResponse): ApiError | null {
  const value: unknown = error.error;
  if (!value || typeof value !== 'object') {
    return null;
  }

  const candidate = value as Partial<ApiError>;
  return typeof candidate.status === 'number' && typeof candidate.message === 'string'
    ? (candidate as ApiError)
    : null;
}
