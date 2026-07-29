import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { InsightsApiService } from '../../core/api/insights-api.service';
import { AnalyticsSummary } from '../../models/insights.models';
import { Analytics } from './analytics';

describe('Analytics', () => {
  const summary: AnalyticsSummary = {
    range: 'THIRTY_DAYS',
    fromDate: '2026-07-01',
    toDate: '2026-07-30',
    applicationsInRange: 4,
    previousPeriodApplications: 2,
    applicationGrowthRate: 100,
    responseRate: 75,
    interviewRate: 50,
    offerRate: 25,
    funnel: [
      { stage: 'APPLIED', applications: 4, conversionFromApplied: 100 },
      { stage: 'ASSESSMENT', applications: 3, conversionFromApplied: 75 },
      { stage: 'INTERVIEW', applications: 2, conversionFromApplied: 50 },
      { stage: 'OFFER', applications: 1, conversionFromApplied: 25 },
    ],
    trend: [
      { periodStart: '2026-07-07', applications: 1 },
      { periodStart: '2026-07-14', applications: 3 },
    ],
    applicationsByStatus: {
      SAVED: 0,
      APPLIED: 1,
      ASSESSMENT: 1,
      INTERVIEW: 1,
      OFFER: 1,
      REJECTED: 0,
      WITHDRAWN: 0,
    },
    applicationsByEmploymentType: {
      FULL_TIME: 4,
      PART_TIME: 0,
      CONTRACT: 0,
      INTERNSHIP: 0,
      CO_OP: 0,
      TEMPORARY: 0,
      OTHER: 0,
    },
    topCompanies: [],
  };

  function setup() {
    const api = {
      getAnalytics: vi.fn().mockReturnValue(of(summary)),
    };
    TestBed.configureTestingModule({
      imports: [Analytics],
      providers: [provideRouter([]), { provide: InsightsApiService, useValue: api }],
    });
    const fixture = TestBed.createComponent(Analytics);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { api, component, fixture };
  }

  it('loads conversion metrics and creates proportional trend bars', () => {
    const { api, component } = setup();

    expect(api.getAnalytics).toHaveBeenCalledWith('THIRTY_DAYS');
    expect(component.summary()).toEqual(summary);
    expect(component.trendHeight(3)).toBe(100);
    expect(component.trendHeight(1)).toBe(33);
    expect(component.statusDistribution()).toHaveLength(4);
  });

  it('reloads when a new date range is selected', () => {
    const { api, component } = setup();

    component.selectRange('SIX_MONTHS');

    expect(component.range()).toBe('SIX_MONTHS');
    expect(api.getAnalytics).toHaveBeenLastCalledWith('SIX_MONTHS');
  });

  it('shows a recoverable API error', () => {
    const api = {
      getAnalytics: vi.fn().mockReturnValue(throwError(() => new Error('offline'))),
    };
    TestBed.configureTestingModule({
      imports: [Analytics],
      providers: [provideRouter([]), { provide: InsightsApiService, useValue: api }],
    });
    const fixture = TestBed.createComponent(Analytics);
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe('Unable to load application analytics.');
  });
});
