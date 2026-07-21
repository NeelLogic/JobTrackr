import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models/api-error.model';

export function apiErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiError | undefined;
    return body?.message || (error.status === 0 ? 'Unable to connect to the server.' : fallback);
  }
  return fallback;
}
