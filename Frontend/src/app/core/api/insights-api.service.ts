import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AnalyticsRange,
  AnalyticsSummary,
  CompaniesQuery,
  CompaniesSummary,
  FollowUpSummary,
} from '../../models/insights.models';

@Injectable({ providedIn: 'root' })
export class InsightsApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = environment.apiUrl;

  getAnalytics(range: AnalyticsRange): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>(`${this.endpoint}/analytics`, {
      params: new HttpParams().set('range', range),
    });
  }

  getCompanies(query: CompaniesQuery = {}): Observable<CompaniesSummary> {
    let params = new HttpParams()
      .set('sort', query.sort ?? 'applications')
      .set('direction', query.direction ?? 'desc');
    const search = query.search?.trim();
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<CompaniesSummary>(`${this.endpoint}/companies`, { params });
  }

  getFollowUps(): Observable<FollowUpSummary> {
    return this.http.get<FollowUpSummary>(`${this.endpoint}/follow-ups`);
  }
}
