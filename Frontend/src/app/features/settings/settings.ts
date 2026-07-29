import { Component, OnInit, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ConnectedIdentity } from '../../models/auth.models';
import { GoogleSignInButton } from '../../shared/google-sign-in-button';

@Component({
  selector: 'app-settings',
  imports: [GoogleSignInButton],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
})
export class Settings implements OnInit {
  private readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly linking = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly identities = signal<ConnectedIdentity[]>([]);
  readonly user = this.auth.user;

  ngOnInit(): void {
    this.loadConnections();
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

  private loadConnections(): void {
    this.loading.set(true);
    this.error.set('');
    this.auth
      .connectedIdentities()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (identities) => this.identities.set(identities),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load connected accounts.')),
      });
  }
}
