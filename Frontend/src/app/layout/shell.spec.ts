import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { DashboardApiService } from '../core/api/dashboard-api.service';
import { AuthService } from '../core/auth.service';
import { Shell } from './shell';

describe('Shell', () => {
  function setup() {
    const auth = {
      user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
      logout: vi.fn(),
    };
    const dashboardApi = {
      getSummary: vi.fn().mockReturnValue(
        of({
          totalApplications: 5,
          applicationsThisMonth: 2,
          interviews: 1,
          offers: 0,
          rejections: 1,
          activeApplications: 3,
          overdueFollowUps: 2,
          upcomingFollowUps: 1,
          staleApplications: 0,
          responseRate: 40,
          interviewRate: 20,
          offerRate: 0,
          applicationsByStatus: {},
          recentApplications: [],
        }),
      ),
    };
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: DashboardApiService, useValue: dashboardApi },
      ],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Shell);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { auth, component, dashboardApi, fixture, router };
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

  it('groups insight routes in navigation and shows overdue work', () => {
    const { component, fixture } = setup();

    expect(fixture.nativeElement.querySelector('a[href="/analytics"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('a[href="/companies"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('a[href="/follow-ups"]')).toBeTruthy();
    expect(component.overdueFollowUps()).toBe(2);
    expect(fixture.nativeElement.querySelector('.nav-badge')?.textContent).toContain('2');
  });
});
