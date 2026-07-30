import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { GmailIntegrationService } from '../../core/api/gmail-integration.service';
import { JobApplication } from '../../models/application.models';
import { GmailConnectionStatus, GmailImportCandidate } from '../../models/integration.models';
import { GmailImport } from './gmail-import';

describe('GmailImport', () => {
  const connected: GmailConnectionStatus = {
    configured: true,
    connected: true,
    email: 'alex@example.com',
    connectedAt: '2026-07-29T10:00:00Z',
    lastSyncAt: null,
  };
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

  function setup(
    status: GmailConnectionStatus = connected,
    pending: GmailImportCandidate[] = [candidate],
  ) {
    const gmail = {
      status: vi.fn().mockReturnValue(of(status)),
      candidates: vi.fn().mockReturnValue(of(pending)),
      scan: vi.fn(),
      importCandidate: vi.fn(),
      dismissCandidate: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [GmailImport],
      providers: [provideRouter([]), { provide: GmailIntegrationService, useValue: gmail }],
    });
    const fixture = TestBed.createComponent(GmailImport);
    fixture.detectChanges();
    return { component: fixture.componentInstance, fixture, gmail };
  }

  it('loads connected Gmail suggestions into a private review queue', () => {
    const { fixture, gmail } = setup();

    expect(gmail.status).toHaveBeenCalledOnce();
    expect(gmail.candidates).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Detected applications');
    expect(fixture.nativeElement.textContent).toContain('Software Developer');
    expect(fixture.nativeElement.textContent).toContain('Nothing is imported automatically');
  });

  it('does not load candidates before Gmail is connected', () => {
    const disconnected: GmailConnectionStatus = {
      configured: true,
      connected: false,
      email: null,
      connectedAt: null,
      lastSyncAt: null,
    };
    const { fixture, gmail } = setup(disconnected, []);

    expect(gmail.candidates).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Connect Gmail to begin');
  });

  it('shows self-hosting guidance and does not load candidates when Gmail is unconfigured', () => {
    const unavailable: GmailConnectionStatus = {
      configured: false,
      connected: false,
      email: null,
      connectedAt: null,
      lastSyncAt: null,
    };
    const { fixture, gmail } = setup(unavailable, []);
    const guideLink = fixture.nativeElement.querySelector(
      '.self-hosted-actions a[target="_blank"]',
    ) as HTMLAnchorElement;

    expect(gmail.candidates).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain(
      'Gmail import is not enabled in this environment',
    );
    expect(fixture.nativeElement.textContent).toContain('Add manually');
    expect(guideLink.href).toContain('Docs/GOOGLE_INTEGRATION.md');
  });

  it('scans Gmail on demand and reports new candidates', () => {
    const { component, gmail } = setup(connected, []);
    gmail.scan.mockReturnValue(
      of({
        messagesScanned: 9,
        matchesDetected: 1,
        candidatesAdded: 1,
        duplicatesSkipped: 0,
        candidates: [candidate],
      }),
    );

    component.scan();

    expect(gmail.scan).toHaveBeenCalledOnce();
    expect(component.candidates()).toEqual([candidate]);
    expect(component.success()).toContain('Found 1 new application candidate');
  });

  it('imports only the reviewed and edited form values', () => {
    const { component, gmail } = setup();
    const imported: JobApplication = {
      id: 41,
      company: 'Acme Technologies',
      jobTitle: 'Junior Software Developer',
      location: 'Toronto, ON',
      jobUrl: 'https://acme.example/jobs/12',
      applicationDate: '2026-07-20',
      status: 'APPLIED',
      employmentType: 'FULL_TIME',
      salaryMin: null,
      salaryMax: null,
      salaryCurrency: null,
      notes: 'Reviewed from Gmail.',
      followUpDate: null,
      createdAt: '2026-07-29T13:00:00Z',
      updatedAt: '2026-07-29T13:00:00Z',
    };
    gmail.importCandidate.mockReturnValue(of(imported));

    component.selectCandidate(candidate);
    component.form.patchValue({
      company: 'Acme Technologies',
      jobTitle: 'Junior Software Developer',
      notes: 'Reviewed from Gmail.',
    });
    component.importSelected();

    expect(gmail.importCandidate).toHaveBeenCalledWith(
      12,
      expect.objectContaining({
        company: 'Acme Technologies',
        jobTitle: 'Junior Software Developer',
        notes: 'Reviewed from Gmail.',
      }),
    );
    expect(component.candidates()).toEqual([]);
    expect(component.importedApplicationId()).toBe(41);
  });

  it('requires confirmation before dismissing a suggestion', () => {
    const { component, gmail } = setup();
    gmail.dismissCandidate.mockReturnValue(of(null));

    component.requestDismiss(12);
    expect(component.dismissConfirmationId()).toBe(12);
    expect(gmail.dismissCandidate).not.toHaveBeenCalled();

    component.dismiss(12);
    expect(gmail.dismissCandidate).toHaveBeenCalledWith(12);
    expect(component.candidates()).toEqual([]);
    expect(component.success()).toContain('will not be shown again');
  });
});
