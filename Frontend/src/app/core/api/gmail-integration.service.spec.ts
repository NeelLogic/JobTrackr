import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GmailConnectionStatus } from '../../models/integration.models';
import { GmailIntegrationService } from './gmail-integration.service';

describe('GmailIntegrationService', () => {
  let service: GmailIntegrationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GmailIntegrationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the current user Gmail connection status', () => {
    const response: GmailConnectionStatus = {
      configured: true,
      connected: true,
      email: 'alex@example.com',
      connectedAt: '2026-07-29T18:00:00Z',
      lastSyncAt: null,
    };

    service.status().subscribe((result) => expect(result).toEqual(response));

    const request = http.expectOne('/api/integrations/gmail');
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('starts authorization through the protected backend endpoint', () => {
    service.connect().subscribe((result) => {
      expect(result.authorizationUrl).toContain('accounts.google.com');
    });

    const request = http.expectOne('/api/integrations/gmail/connect');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({ authorizationUrl: 'https://accounts.google.com/o/oauth2/v2/auth?state=abc' });
  });

  it('disconnects the current user Gmail account', () => {
    service.disconnect().subscribe((result) => expect(result).toBeNull());

    const request = http.expectOne('/api/integrations/gmail');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
