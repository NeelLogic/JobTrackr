import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'jobtrackr-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly window = this.document.defaultView;
  private readonly mediaQuery = this.window?.matchMedia?.('(prefers-color-scheme: dark)');
  private readonly selectedTheme = signal<Theme>(this.initialTheme());

  readonly theme = this.selectedTheme.asReadonly();
  readonly isDark = computed(() => this.selectedTheme() === 'dark');

  constructor() {
    this.applyTheme(this.selectedTheme());
    if (!this.storedTheme()) {
      this.mediaQuery?.addEventListener?.('change', this.systemThemeChanged);
    }
  }

  toggle(): void {
    this.setTheme(this.isDark() ? 'light' : 'dark');
  }

  setTheme(theme: Theme): void {
    this.selectedTheme.set(theme);
    this.applyTheme(theme);
    try {
      this.window?.localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Theme switching should still work when browser storage is unavailable.
    }
  }

  private readonly systemThemeChanged = (event: MediaQueryListEvent): void => {
    if (!this.storedTheme()) {
      const theme: Theme = event.matches ? 'dark' : 'light';
      this.selectedTheme.set(theme);
      this.applyTheme(theme);
    }
  };

  private initialTheme(): Theme {
    const initializedTheme = this.document.documentElement.dataset['theme'];
    if (initializedTheme === 'light' || initializedTheme === 'dark') {
      return initializedTheme;
    }
    return this.storedTheme() ?? (this.mediaQuery?.matches ? 'dark' : 'light');
  }

  private storedTheme(): Theme | null {
    try {
      const stored = this.window?.localStorage.getItem(THEME_STORAGE_KEY);
      return stored === 'light' || stored === 'dark' ? stored : null;
    } catch {
      return null;
    }
  }

  private applyTheme(theme: Theme): void {
    this.document.documentElement.dataset['theme'] = theme;
    this.document.documentElement.style.colorScheme = theme;
    const themeColor = this.document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    themeColor?.setAttribute('content', theme === 'dark' ? '#0b1120' : '#f1f5f9');
  }
}
