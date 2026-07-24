import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authGuard, guestGuard } from './auth.guard';

describe('authentication route guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  function configure(authenticated: boolean) {
    const auth = { isAuthenticated: vi.fn(() => authenticated) };
    const loginTree = { path: '/login' } as unknown as UrlTree;
    const dashboardTree = { path: '/dashboard' } as unknown as UrlTree;
    const router = {
      createUrlTree: vi.fn((commands: string[]) =>
        commands[0] === '/login' ? loginTree : dashboardTree,
      ),
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    return { auth, router, loginTree, dashboardTree };
  }

  it('allows authenticated users into protected routes', () => {
    configure(true);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBe(true);
  });

  it('redirects unauthenticated users to login', () => {
    const { loginTree } = configure(false);
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    expect(result).toBe(loginTree);
  });

  it('allows unauthenticated users into guest routes', () => {
    configure(false);
    const result = TestBed.runInInjectionContext(() => guestGuard(route, state));
    expect(result).toBe(true);
  });

  it('redirects authenticated users away from guest routes', () => {
    const { dashboardTree } = configure(true);
    const result = TestBed.runInInjectionContext(() => guestGuard(route, state));
    expect(result).toBe(dashboardTree);
  });
});
