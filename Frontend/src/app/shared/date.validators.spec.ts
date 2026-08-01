import { FormControl } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { fourDigitDateYear } from './date.validators';

describe('fourDigitDateYear', () => {
  it.each(['2026-08-01', '1000-01-01', '9999-12-31'])('accepts %s', (value) => {
    expect(fourDigitDateYear(new FormControl(value))).toBeNull();
  });

  it.each(['123456-08-01', '999-08-01', '+12345-08-01'])(
    'rejects a non-four-digit year in %s',
    (value) => {
      expect(fourDigitDateYear(new FormControl(value))).toEqual({ dateYear: true });
    },
  );

  it('allows an optional empty date', () => {
    expect(fourDigitDateYear(new FormControl(''))).toBeNull();
  });
});
