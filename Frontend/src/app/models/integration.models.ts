import { ApplicationStatus, EmploymentType, JobApplication } from './application.models';

export interface GmailConnectionStatus {
  configured: boolean;
  connected: boolean;
  email: string | null;
  connectedAt: string | null;
  lastSyncAt: string | null;
}

export interface GmailAuthorizationResponse {
  authorizationUrl: string;
}

export type GmailImportProvider = 'WORKDAY' | 'GENERIC';
export type DetectionConfidence = 'HIGH' | 'MEDIUM' | 'LOW';
export type GmailImportState = 'PENDING' | 'IMPORTED' | 'DISMISSED';

export interface GmailImportCandidate {
  id: number;
  provider: GmailImportProvider;
  confidence: DetectionConfidence;
  company: string;
  jobTitle: string;
  location: string | null;
  jobUrl: string | null;
  applicationDate: string;
  status: ApplicationStatus;
  employmentType: EmploymentType;
  sourceSubject: string;
  sourceSender: string;
  receivedAt: string;
  state: GmailImportState;
  importedApplicationId: number | null;
  detectedAt: string;
}

export interface GmailImportScanResponse {
  messagesScanned: number;
  matchesDetected: number;
  candidatesAdded: number;
  duplicatesSkipped: number;
  candidates: GmailImportCandidate[];
}

export type GmailImportedApplication = JobApplication;
