import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export const MIN_SUPPORTED_DATE = '1000-01-01';
export const MAX_SUPPORTED_DATE = '9999-12-31';

const FOUR_DIGIT_DATE = /^\d{4}-\d{2}-\d{2}$/;

export const fourDigitDateYear: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const value = control.value as string | null;
  return value && !FOUR_DIGIT_DATE.test(value) ? { dateYear: true } : null;
};
