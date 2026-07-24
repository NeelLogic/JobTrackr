import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApplicationQuery,
  ApplicationRequest,
  JobApplication,
  PageResponse,
} from '../../models/application.models';

@Injectable({ providedIn: 'root' })
export class ApplicationApiService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/applications`;

  list(query: ApplicationQuery = {}): Observable<PageResponse<JobApplication>> {
    return this.http.get<PageResponse<JobApplication>>(this.endpoint, {
      params: this.queryParams(query),
    });
  }

  get(id: number): Observable<JobApplication> {
    return this.http.get<JobApplication>(`${this.endpoint}/${id}`);
  }

  create(request: ApplicationRequest): Observable<JobApplication> {
    return this.http.post<JobApplication>(this.endpoint, request);
  }

  update(id: number, request: ApplicationRequest): Observable<JobApplication> {
    return this.http.put<JobApplication>(`${this.endpoint}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private queryParams(query: ApplicationQuery): HttpParams {
    let params = new HttpParams()
      .set('page', String(query.page ?? 0))
      .set('size', String(query.size ?? 10))
      .set('sort', query.sort ?? 'updatedAt')
      .set('direction', query.direction ?? 'desc');

    const search = query.search?.trim();
    if (search) {
      params = params.set('search', search);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.employmentType) {
      params = params.set('employmentType', query.employmentType);
    }

    return params;
  }
}
