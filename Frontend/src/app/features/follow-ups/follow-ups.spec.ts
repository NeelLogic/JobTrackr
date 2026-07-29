import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { InsightsApiService } from '../../core/api/insights-api.service';
import { JobApplication } from '../../models/application.models';
import { FollowUpSummary } from '../../models/insights.models';
import { FollowUps } from './follow-ups';

describe('FollowUps', () => {
  const application: JobApplication = {
    id: 1,
    company: 'Acme',
    jobTitle: 'Software Engineer',
    location: 'Toronto',
    jobUrl: null,
    applicationDate: '2026-07-20',
    status: 'INTERVIEW',
    employmentType: 'FULL_TIME',
    salaryMin: null,
    salaryMax: null,
    salaryCurrency: null,
    notes: null,
    followUpDate: '2026-07-25',
    createdAt: '2026-07-20T12:00:00Z',
    updatedAt: '2026-07-21T12:00:00Z',
  };
  const summary: FollowUpSummary = {
    overdueCount: 1,
    dueTodayCount: 0,
    upcomingCount: 1,
    staleCount: 0,
    overdue: [application],
    dueToday: [],
    upcoming: [{ ...application, id: 2, company: 'Beta' }],
    stale: [],
  };

  function setup() {
    const api = {
      getFollowUps: vi.fn().mockReturnValue(of(summary)),
    };
    TestBed.configureTestingModule({
      imports: [FollowUps],
      providers: [provideRouter([]), { provide: InsightsApiService, useValue: api }],
    });
    const fixture = TestBed.createComponent(FollowUps);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { api, component, fixture };
  }

  it('loads the action queue and defaults to overdue applications', () => {
    const { api, component, fixture } = setup();

    expect(api.getFollowUps).toHaveBeenCalledOnce();
    expect(component.count('overdue')).toBe(1);
    expect(component.selectedApplications()).toEqual([application]);
    expect(fixture.nativeElement.textContent).toContain('Acme');
  });

  it('switches between follow-up groups', () => {
    const { component } = setup();

    component.selectGroup('upcoming');

    expect(component.groupTitle()).toBe('Upcoming');
    expect(component.selectedApplications()[0].company).toBe('Beta');
  });
});
