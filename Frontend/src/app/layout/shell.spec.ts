import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../core/auth.service';
import { Shell } from './shell';

describe('Shell', () => {
  function setup() {
    const auth = {
      user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
      logout: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Shell);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { auth, component, fixture, router };
  }

  it('announces the mobile navigation state', () => {
    const { component, fixture } = setup();
    const menuButton = fixture.nativeElement.querySelector(
      'button[aria-controls="primary-navigation"]',
    ) as HTMLButtonElement;

    expect(menuButton.getAttribute('aria-expanded')).toBe('false');
    component.openMenu();
    fixture.detectChanges();
    expect(menuButton.getAttribute('aria-expanded')).toBe('true');
  });

  it('closes the mobile navigation when Escape is pressed', () => {
    const { component } = setup();
    component.openMenu();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(component.menuOpen()).toBe(false);
  });

  it('clears the session and redirects when signing out', () => {
    const { auth, component, router } = setup();

    component.logout();

    expect(auth.logout).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('includes the Gmail import workflow in primary navigation', () => {
    const { fixture } = setup();
    const importLink = fixture.nativeElement.querySelector(
      'a[href="/imports"]',
    ) as HTMLAnchorElement;

    expect(importLink).toBeTruthy();
    expect(importLink.textContent).toContain('Gmail import');
  });
});
