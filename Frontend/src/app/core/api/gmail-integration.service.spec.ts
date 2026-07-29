import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApplicationRequest, JobApplication } from '../../models/application.models';
import {
  GmailConnectionStatus,
  GmailImportCandidate,
  GmailImportScanResponse,
} from '../../models/integration.models';
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

  it('scans and loads pending Gmail import candidates', () => {
    const candidate: GmailImportCandidate = {
      id: 12,
      provider: 'WORKDAY',
      confidence: 'HIGH',
      company: 'Acme',
      jobTitle: 'Software Developer',
      location: 'Toronto, ON',
      jobUrl: 'https://acme.example/jobs/12',
      applicationDate: '2026-07-20',
      status: 'APPLIED',
      employmentType: 'FULL_TIME',
      sourceSubject: 'Application submitted',
      sourceSender: 'Acme Recruiting <jobs@acme.example>',
      receivedAt: '2026-07-20T12:00:00Z',
      state: 'PENDING',
      importedApplicationId: null,
      detectedAt: '2026-07-29T12:00:00Z',
    };
    const scan: GmailImportScanResponse = {
      messagesScanned: 8,
      matchesDetected: 1,
      candidatesAdded: 1,
      duplicatesSkipped: 0,
      candidates: [candidate],
    };

    service.scan().subscribe((result) => expect(result).toEqual(scan));
    const scanRequest = http.expectOne('/api/integrations/gmail/scan');
    expect(scanRequest.request.method).toBe('POST');
    expect(scanRequest.request.body).toBeNull();
    scanRequest.flush(scan);

    service.candidates().subscribe((result) => expect(result).toEqual([candidate]));
    const candidatesRequest = http.expectOne('/api/integrations/gmail/candidates');
    expect(candidatesRequest.request.method).toBe('GET');
    candidatesRequest.flush([candidate]);
  });

  it('imports reviewed data and dismisses unwanted candidates', () => {
    const reviewed: ApplicationRequest = {
      company: 'Acme',
      jobTitle: 'Junior Developer',
      location: null,
      jobUrl: null,
      applicationDate: '2026-07-20',
      status: 'APPLIED',
      employmentType: 'FULL_TIME',
      salaryMin: null,
      salaryMax: null,
      salaryCurrency: null,
      notes: null,
      followUpDate: null,
    };
    const application = {
      ...reviewed,
      id: 33,
      createdAt: '2026-07-29T12:00:00Z',
      updatedAt: '2026-07-29T12:00:00Z',
    } satisfies JobApplication;

    service
      .importCandidate(12, reviewed)
      .subscribe((result) => expect(result).toEqual(application));
    const importRequest = http.expectOne('/api/integrations/gmail/candidates/12/import');
    expect(importRequest.request.method).toBe('POST');
    expect(importRequest.request.body).toEqual(reviewed);
    importRequest.flush(application);

    service.dismissCandidate(14).subscribe((result) => expect(result).toBeNull());
    const dismissRequest = http.expectOne('/api/integrations/gmail/candidates/14');
    expect(dismissRequest.request.method).toBe('DELETE');
    dismissRequest.flush(null);
  });
});
