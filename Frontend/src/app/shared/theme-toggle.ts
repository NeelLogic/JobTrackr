import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ThemeService } from '../core/theme.service';

@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeToggle {
  private readonly themeService = inject(ThemeService);

  readonly compact = input(false);
  readonly tone = input<'surface' | 'sidebar'>('surface');
  readonly isDark = this.themeService.isDark;
  readonly nextThemeLabel = computed(() =>
    this.isDark() ? 'Switch to light mode' : 'Switch to dark mode',
  );
  readonly visibleLabel = computed(() => (this.isDark() ? 'Light mode' : 'Dark mode'));

  toggle(): void {
    this.themeService.toggle();
  }
}
