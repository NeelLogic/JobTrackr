import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse, ConnectedIdentity } from '../models/auth.models';
import { AuthService } from './auth.service';

describe('AuthService Google integration', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const response: AuthResponse = {
    token: 'jobtrackr-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: { id: 1, name: 'Alex Morgan', email: 'alex@example.com' },
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('exchanges a Google credential and persists the JobTrackr session', () => {
    service.loginWithGoogle('google-credential').subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = http.expectOne('/api/auth/google');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ credential: 'google-credential' });
    request.flush(response);

    expect(service.token()).toBe('jobtrackr-token');
    expect(service.user()).toEqual(response.user);
  });

  it('uses authenticated account-linking endpoints', () => {
    const identity: ConnectedIdentity = {
      provider: 'GOOGLE',
      connectedAt: '2026-07-28T12:00:00Z',
    };

    service.linkGoogle('google-credential').subscribe((result) => {
      expect(result).toEqual(identity);
    });
    const linkRequest = http.expectOne('/api/auth/google/link');
    expect(linkRequest.request.method).toBe('POST');
    expect(linkRequest.request.body).toEqual({ credential: 'google-credential' });
    linkRequest.flush(identity);

    service.connectedIdentities().subscribe((result) => {
      expect(result).toEqual([identity]);
    });
    const identitiesRequest = http.expectOne('/api/auth/identities');
    expect(identitiesRequest.request.method).toBe('GET');
    identitiesRequest.flush([identity]);
  });
});
