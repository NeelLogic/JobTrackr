import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { ThemeService } from '../core/theme.service';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  beforeEach(() => {
    window.localStorage.removeItem('jobtrackr-theme');
    document.documentElement.dataset['theme'] = 'light';
    TestBed.configureTestingModule({ imports: [ThemeToggle] });
  });

  it('announces and activates dark mode', () => {
    const fixture = TestBed.createComponent(ThemeToggle);
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;

    expect(button.getAttribute('aria-label')).toBe('Switch to dark mode');
    expect(button.textContent).toContain('Dark mode');

    button.click();
    fixture.detectChanges();

    expect(TestBed.inject(ThemeService).theme()).toBe('dark');
    expect(button.getAttribute('aria-label')).toBe('Switch to light mode');
    expect(button.getAttribute('aria-pressed')).toBe('true');
  });

  it('hides the text label in compact mode', () => {
    const fixture = TestBed.createComponent(ThemeToggle);
    fixture.componentRef.setInput('compact', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.theme-toggle > span')).toHaveLength(1);
  });
});
