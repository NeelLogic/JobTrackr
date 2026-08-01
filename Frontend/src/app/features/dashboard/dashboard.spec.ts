import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { AuthService } from '../../core/auth.service';
import { JobApplication } from '../../models/application.models';
import { DashboardSummary } from '../../models/dashboard.models';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  const application: JobApplication = {
    id: 7,
    company: 'Acme',
    jobTitle: 'Software Developer',
    location: 'Toronto, ON',
    jobUrl: null,
    applicationDate: '2026-07-20',
    status: 'APPLIED',
    employmentType: 'FULL_TIME',
    salaryMin: null,
    salaryMax: null,
    salaryCurrency: null,
    notes: null,
    followUpDate: null,
    createdAt: '2026-07-20T12:00:00Z',
    updatedAt: '2026-07-21T12:00:00Z',
  };

  const summary: DashboardSummary = {
    totalApplications: 10,
    applicationsThisMonth: 4,
    interviews: 2,
    offers: 1,
    rejections: 2,
    activeApplications: 6,
    overdueFollowUps: 2,
    upcomingFollowUps: 3,
    staleApplications: 1,
    responseRate: 50,
    interviewRate: 20,
    offerRate: 10,
    applicationsByStatus: {
      SAVED: 1,
      APPLIED: 3,
      ASSESSMENT: 1,
      INTERVIEW: 2,
      OFFER: 1,
      REJECTED: 2,
      WITHDRAWN: 0,
    },
    recentApplications: [],
  };

  function setup(response: DashboardSummary = summary) {
    const api = {
      getSummary: vi.fn<() => ReturnType<DashboardApiService['getSummary']>>(),
    };
    api.getSummary.mockReturnValue(of(response));
    const applicationApi = {
      delete: vi.fn<(id: number) => ReturnType<ApplicationApiService['delete']>>(),
    };
    applicationApi.delete.mockReturnValue(of(undefined));
    const auth = {
      user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
    };

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: DashboardApiService, useValue: api },
        { provide: ApplicationApiService, useValue: applicationApi },
        { provide: AuthService, useValue: auth },
      ],
    });
    const fixture = TestBed.createComponent(Dashboard);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { api, applicationApi, component, fixture };
  }

  it('loads dashboard metrics and personalizes the greeting', () => {
    const { api, component } = setup();

    expect(api.getSummary).toHaveBeenCalledOnce();
    expect(component.firstName()).toBe('Alex');
    expect(component.stats().map((stat) => stat.value)).toEqual(['10', '6', '4', '50%', '1']);
    expect(component.loading()).toBe(false);
  });

  it('renders consistently sized vector icons for attention cards', () => {
    const { fixture } = setup();
    const icons = fixture.nativeElement.querySelectorAll('.attention-card__icon');

    expect(icons).toHaveLength(3);
    expect(fixture.nativeElement.querySelectorAll('.attention-card__icon svg')).toHaveLength(3);
  });

  it('calculates a row and percentage for every application status', () => {
    const { component } = setup();

    expect(component.statusRows()).toHaveLength(7);
    expect(component.statusRows().find((row) => row.status === 'APPLIED')).toEqual({
      status: 'APPLIED',
      count: 3,
      percentage: 30,
    });
  });

  it('uses zero percentages when the pipeline is empty', () => {
    const { component } = setup({
      ...summary,
      totalApplications: 0,
      applicationsByStatus: {
        SAVED: 0,
        APPLIED: 0,
        ASSESSMENT: 0,
        INTERVIEW: 0,
        OFFER: 0,
        REJECTED: 0,
        WITHDRAWN: 0,
      },
    });

    expect(component.statusRows().every((row) => row.percentage === 0)).toBe(true);
  });

  it('shows a load error and successfully retries the dashboard request', () => {
    const api = {
      getSummary: vi.fn<() => ReturnType<DashboardApiService['getSummary']>>(),
    };
    api.getSummary
      .mockReturnValueOnce(throwError(() => new Error('offline')))
      .mockReturnValue(of(summary));
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: DashboardApiService, useValue: api },
        {
          provide: ApplicationApiService,
          useValue: { delete: vi.fn(() => of(undefined)) },
        },
        {
          provide: AuthService,
          useValue: {
            user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(Dashboard);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    expect(component.error()).toBe('Unable to load your dashboard.');

    component.retry();
    expect(api.getSummary).toHaveBeenCalledTimes(2);
    expect(component.error()).toBe('');
    expect(component.summary()).toEqual(summary);
  });

  it('deletes a recent application and refreshes dashboard metrics', () => {
    const { api, applicationApi, component } = setup({
      ...summary,
      recentApplications: [application],
    });
    api.getSummary.mockClear();

    component.requestDelete(application);
    component.deleteApplication();

    expect(applicationApi.delete).toHaveBeenCalledWith(7);
    expect(component.deleteTarget()).toBeNull();
    expect(api.getSummary).toHaveBeenCalledOnce();
  });

  it('keeps the dashboard confirmation open when deletion fails', () => {
    const { applicationApi, component } = setup({
      ...summary,
      recentApplications: [application],
    });
    applicationApi.delete.mockReturnValue(throwError(() => new Error('offline')));

    component.requestDelete(application);
    component.deleteApplication();

    expect(component.deleteTarget()).toEqual(application);
    expect(component.deleteError()).toBe('Unable to delete this application.');
  });
});
