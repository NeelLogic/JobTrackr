import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { JobApplication } from '../../models/application.models';
import { ApplicationDetail } from './application-detail';

describe('ApplicationDetail', () => {
  const application: JobApplication = {
    id: 7,
    company: 'Acme',
    jobTitle: 'Software Developer',
    location: 'Toronto, ON',
    jobUrl: 'https://example.com/jobs/7',
    applicationDate: '2026-07-20',
    status: 'APPLIED',
    employmentType: 'FULL_TIME',
    salaryMin: 70000,
    salaryMax: 85000,
    salaryCurrency: 'CAD',
    notes: 'Referral submitted.',
    followUpDate: '2026-07-30',
    createdAt: '2026-07-20T12:00:00Z',
    updatedAt: '2026-07-21T12:00:00Z',
  };

  function setup(routeId = '7') {
    const api = {
      get: vi.fn<(id: number) => ReturnType<ApplicationApiService['get']>>(),
      delete: vi.fn<(id: number) => ReturnType<ApplicationApiService['delete']>>(),
    };
    const router = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };
    const route = {
      snapshot: { paramMap: convertToParamMap({ id: routeId }) },
    };

    TestBed.configureTestingModule({
      imports: [ApplicationDetail],
      providers: [
        { provide: ApplicationApiService, useValue: api },
        { provide: ActivatedRoute, useValue: route },
        { provide: Router, useValue: router },
      ],
    });
    const fixture = TestBed.createComponent(ApplicationDetail);
    const component = fixture.componentInstance;
    return { api, component, fixture, router };
  }

  it('loads and displays the requested application', () => {
    const { api, component, fixture } = setup();
    api.get.mockReturnValue(of(application));

    fixture.detectChanges();

    expect(api.get).toHaveBeenCalledWith(7);
    expect(component.application()).toEqual(application);
    expect(component.salary()).toContain('$70,000');
    expect(component.salary()).toContain('$85,000');
    expect(component.loading()).toBe(false);
  });

  it('rejects an invalid application route before calling the API', () => {
    const { api, component, fixture } = setup('0');

    fixture.detectChanges();

    expect(component.error()).toBe('This application link is invalid.');
    expect(component.loading()).toBe(false);
    expect(api.get).not.toHaveBeenCalled();
  });

  it('formats open-ended and missing salary ranges', () => {
    const { api, component, fixture } = setup();
    api.get.mockReturnValue(of({ ...application, salaryMax: null }));
    fixture.detectChanges();
    expect(component.salary()).toContain('From');

    component.application.set({
      ...application,
      salaryMin: null,
      salaryMax: null,
      salaryCurrency: null,
    });
    expect(component.salary()).toBe('Not provided');
  });

  it('requires confirmation before deleting and redirects after success', () => {
    const { api, component, fixture, router } = setup();
    api.get.mockReturnValue(of(application));
    api.delete.mockReturnValue(of(undefined));
    fixture.detectChanges();

    component.requestDelete();
    expect(component.confirmingDelete()).toBe(true);

    component.deleteApplication();
    expect(api.delete).toHaveBeenCalledWith(7);
    expect(router.navigate).toHaveBeenCalledWith(['/applications']);
    expect(component.deleting()).toBe(false);
  });

  it('keeps the confirmation visible and reports a failed deletion', () => {
    const { api, component, fixture } = setup();
    api.get.mockReturnValue(of(application));
    api.delete.mockReturnValue(throwError(() => new Error('offline')));
    fixture.detectChanges();

    component.requestDelete();
    component.deleteApplication();

    expect(component.confirmingDelete()).toBe(true);
    expect(component.deleteError()).toBe('Unable to delete this application.');
    expect(component.deleting()).toBe(false);
  });
});
