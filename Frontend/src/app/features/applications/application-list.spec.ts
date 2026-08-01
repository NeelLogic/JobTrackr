import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { ApplicationQuery, JobApplication, PageResponse } from '../../models/application.models';
import { ApplicationList } from './application-list';

describe('ApplicationList', () => {
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

  function page(
    overrides: Partial<PageResponse<JobApplication>> = {},
  ): PageResponse<JobApplication> {
    return {
      content: [application],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      ...overrides,
    };
  }

  function setup(response: PageResponse<JobApplication> = page()) {
    const api = {
      list: vi.fn<(query?: ApplicationQuery) => ReturnType<ApplicationApiService['list']>>(),
      delete: vi.fn<(id: number) => ReturnType<ApplicationApiService['delete']>>(),
    };
    api.list.mockReturnValue(of(response));
    api.delete.mockReturnValue(of(undefined));

    TestBed.configureTestingModule({
      imports: [ApplicationList],
      providers: [provideRouter([]), { provide: ApplicationApiService, useValue: api }],
    });

    const fixture = TestBed.createComponent(ApplicationList);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { api, component, fixture };
  }

  it('loads the first page and exposes the displayed result range', () => {
    const { api, component } = setup(
      page({ page: 1, size: 10, totalElements: 15, totalPages: 2, first: false }),
    );

    expect(api.list).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 10, sort: 'updatedAt', direction: 'desc' }),
    );
    expect(component.applications()).toEqual([application]);
    expect(component.rangeStart()).toBe(11);
    expect(component.rangeEnd()).toBe(15);
  });

  it('applies filters from page zero and reports active filters', () => {
    const { api, component } = setup();
    api.list.mockClear();
    component.filters.patchValue({
      search: ' developer ',
      status: 'APPLIED',
      employmentType: 'FULL_TIME',
      sort: 'company',
      direction: 'asc',
      size: 20,
    });

    component.applyFilters();

    expect(component.hasActiveFilters()).toBe(true);
    expect(api.list).toHaveBeenCalledWith({
      search: ' developer ',
      status: 'APPLIED',
      employmentType: 'FULL_TIME',
      sort: 'company',
      direction: 'asc',
      size: 20,
      page: 0,
    });
  });

  it('resets filters and reloads the default query', () => {
    const { api, component } = setup();
    component.filters.patchValue({ search: 'Acme', status: 'INTERVIEW', size: 50 });
    api.list.mockClear();

    component.clearFilters();

    expect(component.filters.getRawValue()).toEqual({
      search: '',
      status: '',
      employmentType: '',
      sort: 'updatedAt',
      direction: 'desc',
      size: 10,
    });
    expect(api.list).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, search: '', status: '', size: 10 }),
    );
  });

  it('ignores invalid pagination requests and loads a valid page', () => {
    const { api, component } = setup(
      page({ totalElements: 25, totalPages: 3, first: true, last: false }),
    );
    api.list.mockClear();

    component.goToPage(-1);
    component.goToPage(3);
    component.goToPage(0);
    expect(api.list).not.toHaveBeenCalled();

    component.goToPage(1);
    expect(api.list).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }));
  });

  it('shows a useful fallback and allows the failed request to be retried', () => {
    const api = {
      list: vi.fn<(query?: ApplicationQuery) => ReturnType<ApplicationApiService['list']>>(),
      delete: vi.fn<(id: number) => ReturnType<ApplicationApiService['delete']>>(),
    };
    api.list
      .mockReturnValueOnce(throwError(() => new Error('offline')))
      .mockReturnValue(of(page()));
    TestBed.configureTestingModule({
      imports: [ApplicationList],
      providers: [provideRouter([]), { provide: ApplicationApiService, useValue: api }],
    });
    const fixture = TestBed.createComponent(ApplicationList);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    expect(component.error()).toBe('Unable to load your applications.');

    component.retry();
    expect(api.list).toHaveBeenCalledTimes(2);
    expect(component.error()).toBe('');
    expect(component.applications()).toEqual([application]);
  });

  it('confirms deletion and reloads the current page after success', () => {
    const { api, component } = setup();
    api.list.mockClear();

    component.requestDelete(application);
    expect(component.deleteTarget()).toEqual(application);

    component.deleteApplication();

    expect(api.delete).toHaveBeenCalledWith(7);
    expect(component.deleteTarget()).toBeNull();
    expect(api.list).toHaveBeenCalledWith(expect.objectContaining({ page: 0 }));
  });

  it('keeps the dialog open when deletion fails', () => {
    const { api, component } = setup();
    api.delete.mockReturnValue(throwError(() => new Error('offline')));

    component.requestDelete(application);
    component.deleteApplication();

    expect(component.deleteTarget()).toEqual(application);
    expect(component.deleteError()).toBe('Unable to delete this application.');
  });
});
