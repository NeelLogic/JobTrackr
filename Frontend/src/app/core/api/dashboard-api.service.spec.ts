import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardSummary } from '../../models/dashboard.models';
import { DashboardApiService } from './dashboard-api.service';

describe('DashboardApiService', () => {
  let service: DashboardApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the authenticated dashboard summary', () => {
    const response: DashboardSummary = {
      totalApplications: 12,
      applicationsThisMonth: 5,
      interviews: 2,
      offers: 1,
      rejections: 3,
      activeApplications: 7,
      overdueFollowUps: 2,
      upcomingFollowUps: 3,
      staleApplications: 1,
      responseRate: 58.3,
      interviewRate: 16.7,
      offerRate: 8.3,
      applicationsByStatus: {
        SAVED: 1,
        APPLIED: 3,
        ASSESSMENT: 1,
        INTERVIEW: 2,
        OFFER: 1,
        REJECTED: 3,
        WITHDRAWN: 1,
      },
      recentApplications: [],
    };

    service.getSummary().subscribe((result) => expect(result).toEqual(response));

    const request = http.expectOne('/api/dashboard');
    expect(request.request.method).toBe('GET');
    request.flush(response);
  });
});
