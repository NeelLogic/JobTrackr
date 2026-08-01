import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    window.localStorage.removeItem('jobtrackr-theme');
    delete document.documentElement.dataset['theme'];
    document.documentElement.style.removeProperty('color-scheme');
    if (!document.querySelector('meta[name="theme-color"]')) {
      const meta = document.createElement('meta');
      meta.name = 'theme-color';
      document.head.appendChild(meta);
    }
    TestBed.configureTestingModule({});
  });

  it('applies and persists an explicit dark theme', () => {
    const service = TestBed.inject(ThemeService);

    service.setTheme('dark');

    expect(service.theme()).toBe('dark');
    expect(service.isDark()).toBe(true);
    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(window.localStorage.getItem('jobtrackr-theme')).toBe('dark');
    expect(document.querySelector('meta[name="theme-color"]')?.getAttribute('content')).toBe(
      '#0b1120',
    );
  });

  it('toggles from light to dark and back to light', () => {
    const service = TestBed.inject(ThemeService);
    service.setTheme('light');

    service.toggle();
    expect(service.theme()).toBe('dark');

    service.toggle();
    expect(service.theme()).toBe('light');
  });
});
