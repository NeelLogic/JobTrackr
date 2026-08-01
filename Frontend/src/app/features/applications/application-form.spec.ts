import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { ApplicationRequest, JobApplication } from '../../models/application.models';
import { ApplicationForm } from './application-form';

describe('ApplicationForm', () => {
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

  function setup(routeId?: string) {
    const api = {
      create: vi.fn<(request: ApplicationRequest) => ReturnType<ApplicationApiService['create']>>(),
      update:
        vi.fn<
          (id: number, request: ApplicationRequest) => ReturnType<ApplicationApiService['update']>
        >(),
      get: vi.fn<(id: number) => ReturnType<ApplicationApiService['get']>>(),
      delete: vi.fn<(id: number) => ReturnType<ApplicationApiService['delete']>>(),
    };
    const router = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };
    const route = {
      snapshot: {
        paramMap: convertToParamMap(routeId === undefined ? {} : { id: routeId }),
      },
    };

    TestBed.configureTestingModule({
      imports: [ApplicationForm],
      providers: [
        { provide: ApplicationApiService, useValue: api },
        { provide: ActivatedRoute, useValue: route },
        { provide: Router, useValue: router },
      ],
    });

    const fixture = TestBed.createComponent(ApplicationForm);
    const component = fixture.componentInstance;
    return { api, component, fixture, router };
  }

  it('marks an empty form invalid without calling the API', () => {
    const { api, component, fixture } = setup();
    fixture.detectChanges();

    component.submit();

    expect(component.submitted()).toBe(true);
    expect(component.fieldError('company')).toBe('Company is required.');
    expect(component.fieldError('jobTitle')).toBe('Job title is required.');
    expect(api.create).not.toHaveBeenCalled();
  });

  it('enforces application date and salary business rules', () => {
    const { component, fixture } = setup();
    fixture.detectChanges();
    component.form.patchValue({
      company: 'Acme',
      jobTitle: 'Developer',
      status: 'INTERVIEW',
      applicationDate: '',
      salaryMin: 90000,
      salaryMax: 80000,
      salaryCurrency: '',
    });

    component.submit();

    expect(component.form.hasError('applicationDateRequired')).toBe(true);
    expect(component.form.hasError('salaryRange')).toBe(true);
    expect(component.form.hasError('salaryCurrencyRequired')).toBe(true);
  });

  it('rejects future dates and invalid job URLs', () => {
    const { component, fixture } = setup();
    fixture.detectChanges();

    component.form.patchValue({
      applicationDate: '2999-01-01',
      jobUrl: 'example.com/jobs/7',
    });
    component.form.controls.applicationDate.markAsTouched();
    component.form.controls.jobUrl.markAsTouched();

    expect(component.fieldError('applicationDate')).toBe(
      'Application date cannot be in the future.',
    );
    expect(component.fieldError('jobUrl')).toBe('Enter a complete HTTP or HTTPS URL.');
  });

  it('normalizes optional values and creates a valid application', () => {
    const { api, component, fixture, router } = setup();
    api.create.mockReturnValue(of(application));
    fixture.detectChanges();
    component.form.patchValue({
      company: '  Acme  ',
      jobTitle: '  Software Developer ',
      location: '   ',
      jobUrl: 'https://example.com/jobs/7',
      status: 'SAVED',
      salaryCurrency: 'cad',
      notes: '  Referral submitted. ',
    });

    component.submit();

    expect(api.create).toHaveBeenCalledWith(
      expect.objectContaining({
        company: 'Acme',
        jobTitle: 'Software Developer',
        location: null,
        jobUrl: 'https://example.com/jobs/7',
        salaryCurrency: 'CAD',
        notes: 'Referral submitted.',
      }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/applications', 7]);
    expect(component.saving()).toBe(false);
  });

  it('loads an existing application and submits an update', () => {
    const { api, component, fixture, router } = setup('7');
    api.get.mockReturnValue(of(application));
    api.update.mockReturnValue(of({ ...application, status: 'INTERVIEW' }));

    fixture.detectChanges();
    expect(api.get).toHaveBeenCalledWith(7);
    expect(component.isEdit()).toBe(true);
    expect(component.form.controls.company.value).toBe('Acme');

    component.form.patchValue({ status: 'INTERVIEW' });
    component.submit();

    expect(api.update).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ company: 'Acme', status: 'INTERVIEW' }),
    );
    expect(router.navigate).toHaveBeenCalledWith(['/applications', 7]);
  });

  it('deletes an application from the edit page after confirmation', () => {
    const { api, component, fixture, router } = setup('7');
    api.get.mockReturnValue(of(application));
    api.delete.mockReturnValue(of(undefined));
    fixture.detectChanges();

    component.requestDelete();
    expect(component.confirmingDelete()).toBe(true);

    component.deleteApplication();

    expect(api.delete).toHaveBeenCalledWith(7);
    expect(router.navigate).toHaveBeenCalledWith(['/applications']);
  });

  it('reports a failed edit-page deletion without closing confirmation', () => {
    const { api, component, fixture } = setup('7');
    api.get.mockReturnValue(of(application));
    api.delete.mockReturnValue(throwError(() => new Error('offline')));
    fixture.detectChanges();

    component.requestDelete();
    component.deleteApplication();

    expect(component.confirmingDelete()).toBe(true);
    expect(component.deleteError()).toBe('Unable to delete this application.');
  });

  it('does not request an application for an invalid route id', () => {
    const { api, component, fixture } = setup('invalid');

    fixture.detectChanges();

    expect(component.loadError()).toBe('This application link is invalid.');
    expect(api.get).not.toHaveBeenCalled();
  });

  it('maps server field errors back to the corresponding form control', () => {
    const { api, component, fixture } = setup();
    api.create.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            statusText: 'Bad Request',
            error: {
              timestamp: '2026-07-24T00:00:00Z',
              status: 400,
              error: 'Bad Request',
              message: 'Validation failed.',
              path: '/api/applications',
              fieldErrors: { company: 'Company already exists in this test.' },
            },
          }),
      ),
    );
    fixture.detectChanges();
    component.form.patchValue({ company: 'Acme', jobTitle: 'Developer' });

    component.submit();

    expect(component.saveError()).toBe('Validation failed.');
    expect(component.fieldError('company')).toBe('Company already exists in this test.');
    expect(component.saving()).toBe(false);
  });
});
