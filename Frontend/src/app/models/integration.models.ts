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
