import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AnalyticsSummary, CompaniesSummary, FollowUpSummary } from '../../models/insights.models';
import { InsightsApiService } from './insights-api.service';

describe('InsightsApiService', () => {
  let service: InsightsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InsightsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests analytics for the selected range', () => {
    const response = {} as AnalyticsSummary;
    service.getAnalytics('NINETY_DAYS').subscribe((result) => expect(result).toBe(response));

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/analytics' && candidate.params.get('range') === 'NINETY_DAYS',
    );
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('normalizes company filters into query parameters', () => {
    const response = { totalCompanies: 0, companies: [] } as CompaniesSummary;
    service
      .getCompanies({ search: ' Acme ', sort: 'offers', direction: 'asc' })
      .subscribe((result) => expect(result).toBe(response));

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/companies' &&
        candidate.params.get('search') === 'Acme' &&
        candidate.params.get('sort') === 'offers' &&
        candidate.params.get('direction') === 'asc',
    );
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });

  it('loads the follow-up queue', () => {
    const response = {
      overdueCount: 0,
      dueTodayCount: 0,
      upcomingCount: 0,
      staleCount: 0,
      overdue: [],
      dueToday: [],
      upcoming: [],
      stale: [],
    } as FollowUpSummary;
    service.getFollowUps().subscribe((result) => expect(result).toBe(response));

    const request = http.expectOne('/api/follow-ups');
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });
});
