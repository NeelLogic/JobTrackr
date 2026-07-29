import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { GmailIntegrationService } from '../../core/api/gmail-integration.service';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ConnectedIdentity } from '../../models/auth.models';
import { GmailConnectionStatus } from '../../models/integration.models';
import { GoogleSignInButton } from '../../shared/google-sign-in-button';

@Component({
  selector: 'app-settings',
  imports: [GoogleSignInButton, RouterLink],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly gmail = inject(GmailIntegrationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly accountsLoading = signal(true);
  readonly gmailLoading = signal(true);
  readonly linking = signal(false);
  readonly gmailConnecting = signal(false);
  readonly gmailDisconnecting = signal(false);
  readonly confirmGmailDisconnect = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly identities = signal<ConnectedIdentity[]>([]);
  readonly gmailStatus = signal<GmailConnectionStatus | null>(null);
  readonly user = this.auth.user;

  ngOnInit(): void {
    this.readGmailCallbackResult();
    this.loadConnections();
    this.loadGmailStatus();
  }

  isGoogleConnected(): boolean {
    return this.identities().some((identity) => identity.provider === 'GOOGLE');
  }

  linkGoogle(credential: string): void {
    if (this.linking() || this.isGoogleConnected()) {
      return;
    }
    this.linking.set(true);
    this.error.set('');
    this.success.set('');
    this.auth
      .linkGoogle(credential)
      .pipe(finalize(() => this.linking.set(false)))
      .subscribe({
        next: (identity) => {
          this.identities.update((identities) => [...identities, identity]);
          this.success.set('Google account connected. You can now use it to sign in.');
        },
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to connect your Google account.')),
      });
  }

  connectGmail(): void {
    const status = this.gmailStatus();
    if (this.gmailConnecting() || status?.connected || !status?.configured) {
      return;
    }
    this.gmailConnecting.set(true);
    this.error.set('');
    this.success.set('');
    this.gmail
      .connect()
      .pipe(finalize(() => this.gmailConnecting.set(false)))
      .subscribe({
        next: ({ authorizationUrl }) => this.gmail.redirectToAuthorization(authorizationUrl),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to start Gmail authorization.')),
      });
  }

  requestGmailDisconnect(): void {
    this.confirmGmailDisconnect.set(true);
    this.error.set('');
    this.success.set('');
  }

  cancelGmailDisconnect(): void {
    this.confirmGmailDisconnect.set(false);
  }

  disconnectGmail(): void {
    if (this.gmailDisconnecting()) {
      return;
    }
    this.gmailDisconnecting.set(true);
    this.error.set('');
    this.success.set('');
    this.gmail
      .disconnect()
      .pipe(finalize(() => this.gmailDisconnecting.set(false)))
      .subscribe({
        next: () => {
          this.confirmGmailDisconnect.set(false);
          this.gmailStatus.update((status) =>
            status
              ? {
                  ...status,
                  connected: false,
                  email: null,
                  connectedAt: null,
                  lastSyncAt: null,
                }
              : status,
          );
          this.success.set('Gmail disconnected and stored access was removed.');
        },
        error: (error) => this.error.set(apiErrorMessage(error, 'Unable to disconnect Gmail.')),
      });
  }

  private loadConnections(): void {
    this.accountsLoading.set(true);
    this.auth
      .connectedIdentities()
      .pipe(finalize(() => this.accountsLoading.set(false)))
      .subscribe({
        next: (identities) => this.identities.set(identities),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load connected accounts.')),
      });
  }

  private loadGmailStatus(): void {
    this.gmailLoading.set(true);
    this.gmail
      .status()
      .pipe(finalize(() => this.gmailLoading.set(false)))
      .subscribe({
        next: (status) => this.gmailStatus.set(status),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load Gmail connection status.')),
      });
  }

  private readGmailCallbackResult(): void {
    const result = this.route.snapshot.queryParamMap.get('gmail');
    if (!result) {
      return;
    }
    if (result === 'connected') {
      this.success.set('Gmail connected securely. You can now scan for application emails.');
    } else if (result === 'denied') {
      this.error.set('Gmail permission was not granted. Your account was not connected.');
    } else {
      this.error.set('Gmail could not be connected. Please try again.');
    }
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { gmail: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
