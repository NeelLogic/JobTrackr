import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApplicationRequest } from '../../models/application.models';
import {
  GmailAuthorizationResponse,
  GmailConnectionStatus,
  GmailImportCandidate,
  GmailImportedApplication,
  GmailImportScanResponse,
} from '../../models/integration.models';

@Injectable({ providedIn: 'root' })
export class GmailIntegrationService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);
  private readonly endpoint = `${environment.apiUrl}/integrations/gmail`;

  status(): Observable<GmailConnectionStatus> {
    return this.http.get<GmailConnectionStatus>(this.endpoint);
  }

  connect(): Observable<GmailAuthorizationResponse> {
    return this.http.post<GmailAuthorizationResponse>(`${this.endpoint}/connect`, null);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(this.endpoint);
  }

  scan(): Observable<GmailImportScanResponse> {
    return this.http.post<GmailImportScanResponse>(`${this.endpoint}/scan`, null);
  }

  candidates(): Observable<GmailImportCandidate[]> {
    return this.http.get<GmailImportCandidate[]>(`${this.endpoint}/candidates`);
  }

  importCandidate(
    candidateId: number,
    request: ApplicationRequest,
  ): Observable<GmailImportedApplication> {
    return this.http.post<GmailImportedApplication>(
      `${this.endpoint}/candidates/${candidateId}/import`,
      request,
    );
  }

  dismissCandidate(candidateId: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/candidates/${candidateId}`);
  }

  redirectToAuthorization(authorizationUrl: string): void {
    this.document.defaultView?.location.assign(authorizationUrl);
  }
}
