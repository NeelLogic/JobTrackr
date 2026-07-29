import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { InsightsApiService } from '../../core/api/insights-api.service';
import { CompaniesSummary } from '../../models/insights.models';
import { Companies } from './companies';

describe('Companies', () => {
  const summary: CompaniesSummary = {
    totalCompanies: 1,
    companies: [
      {
        company: 'Acme',
        totalApplications: 3,
        activeApplications: 2,
        interviewsReached: 1,
        offersReached: 1,
        latestApplicationDate: '2026-07-20',
        lastActivityAt: '2026-07-21T12:00:00Z',
      },
    ],
  };

  function setup() {
    const api = {
      getCompanies: vi.fn().mockReturnValue(of(summary)),
    };
    TestBed.configureTestingModule({
      imports: [Companies],
      providers: [provideRouter([]), { provide: InsightsApiService, useValue: api }],
    });
    const fixture = TestBed.createComponent(Companies);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { api, component, fixture };
  }

  it('loads company outcome summaries', () => {
    const { api, component, fixture } = setup();

    expect(api.getCompanies).toHaveBeenCalledWith({
      search: '',
      sort: 'applications',
      direction: 'desc',
    });
    expect(component.summary()).toEqual(summary);
    expect(fixture.nativeElement.textContent).not.toContain('Private Analytics Co');
    expect(fixture.nativeElement.textContent).toContain('Acme');
  });

  it('applies search and outcome sorting', () => {
    const { api, component } = setup();
    api.getCompanies.mockClear();
    component.filters.setValue({ search: ' Acme ', sort: 'offers', direction: 'asc' });

    component.applyFilters();

    expect(api.getCompanies).toHaveBeenCalledWith({
      search: ' Acme ',
      sort: 'offers',
      direction: 'asc',
    });
  });
});
