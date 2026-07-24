import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ApplicationRequest, JobApplication, PageResponse } from '../../models/application.models';
import { ApplicationApiService } from './application-api.service';

describe('ApplicationApiService', () => {
  let service: ApplicationApiService;
  let http: HttpTestingController;

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

  const requestBody: ApplicationRequest = {
    company: application.company,
    jobTitle: application.jobTitle,
    location: application.location,
    jobUrl: application.jobUrl,
    applicationDate: application.applicationDate,
    status: application.status,
    employmentType: application.employmentType,
    salaryMin: application.salaryMin,
    salaryMax: application.salaryMax,
    salaryCurrency: application.salaryCurrency,
    notes: application.notes,
    followUpDate: application.followUpDate,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApplicationApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('serializes application filters and pagination', () => {
    const response: PageResponse<JobApplication> = {
      content: [application],
      page: 2,
      size: 20,
      totalElements: 45,
      totalPages: 3,
      first: false,
      last: true,
    };

    service
      .list({
        search: '  developer  ',
        status: 'APPLIED',
        employmentType: 'FULL_TIME',
        page: 2,
        size: 20,
        sort: 'company',
        direction: 'asc',
      })
      .subscribe((result) => expect(result).toEqual(response));

    const request = http.expectOne((candidate) => candidate.url === '/api/applications');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('search')).toBe('developer');
    expect(request.request.params.get('status')).toBe('APPLIED');
    expect(request.request.params.get('employmentType')).toBe('FULL_TIME');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.get('sort')).toBe('company');
    expect(request.request.params.get('direction')).toBe('asc');
    request.flush(response);
  });

  it('uses predictable list defaults and omits empty filters', () => {
    service.list({ search: ' ', status: '', employmentType: '' }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === '/api/applications');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.get('sort')).toBe('updatedAt');
    expect(request.request.params.get('direction')).toBe('desc');
    expect(request.request.params.has('search')).toBe(false);
    expect(request.request.params.has('status')).toBe(false);
    expect(request.request.params.has('employmentType')).toBe(false);
    request.flush({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });
  });

  it('calls the expected CRUD endpoints', () => {
    service.get(7).subscribe((result) => expect(result).toEqual(application));
    const getRequest = http.expectOne('/api/applications/7');
    expect(getRequest.request.method).toBe('GET');
    getRequest.flush(application);

    service.create(requestBody).subscribe((result) => expect(result).toEqual(application));
    const createRequest = http.expectOne('/api/applications');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.body).toEqual(requestBody);
    createRequest.flush(application);

    service.update(7, requestBody).subscribe((result) => expect(result).toEqual(application));
    const updateRequest = http.expectOne('/api/applications/7');
    expect(updateRequest.request.method).toBe('PUT');
    expect(updateRequest.request.body).toEqual(requestBody);
    updateRequest.flush(application);

    service.delete(7).subscribe((result) => expect(result).toBeNull());
    const deleteRequest = http.expectOne('/api/applications/7');
    expect(deleteRequest.request.method).toBe('DELETE');
    deleteRequest.flush(null);
  });
});
