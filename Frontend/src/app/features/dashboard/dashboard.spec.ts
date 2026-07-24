import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { AuthService } from '../../core/auth.service';
import { DashboardSummary } from '../../models/dashboard.models';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  const summary: DashboardSummary = {
    totalApplications: 10,
    applicationsThisMonth: 4,
    interviews: 2,
    offers: 1,
    rejections: 2,
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
    const auth = {
      user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
    };

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        { provide: DashboardApiService, useValue: api },
        { provide: AuthService, useValue: auth },
      ],
    });
    const fixture = TestBed.createComponent(Dashboard);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { api, component, fixture };
  }

  it('loads dashboard metrics and personalizes the greeting', () => {
    const { api, component } = setup();

    expect(api.getSummary).toHaveBeenCalledOnce();
    expect(component.firstName()).toBe('Alex');
    expect(component.stats().map((stat) => stat.value)).toEqual([10, 4, 2, 1, 2]);
    expect(component.loading()).toBe(false);
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
});
