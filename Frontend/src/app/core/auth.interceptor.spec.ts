import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  function configure(token: string | null = 'signed-token') {
    const auth = {
      token: vi.fn(() => token),
      logout: vi.fn(),
    };
    const router = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
    return {
      auth,
      router,
      client: TestBed.inject(HttpTestingController),
    };
  }

  it('adds the current JWT to API requests', () => {
    const { client } = configure();
    const http = TestBed.inject(HttpClient);

    http.get('/api/applications').subscribe();
    const request = client.expectOne('/api/applications');
    expect(request.request.headers.get('Authorization')).toBe('Bearer signed-token');
    request.flush([]);
    client.verify();
  });

  it('clears the session and redirects after a protected 401 response', () => {
    const { auth, router, client } = configure();
    const http = TestBed.inject(HttpClient);

    http.get('/api/dashboard').subscribe({ error: () => undefined });
    const request = client.expectOne('/api/dashboard');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalledOnce();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
    client.verify();
  });
});
