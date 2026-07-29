import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, NgZone, inject } from '@angular/core';
import { firstValueFrom, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { GoogleAuthConfig } from '../models/auth.models';

interface GoogleCredentialResponse {
  credential: string;
}

interface GoogleIdentityApi {
  initialize(options: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
  }): void;
  renderButton(
    parent: HTMLElement,
    options: {
      type: 'standard';
      theme: 'outline';
      size: 'large';
      shape: 'rectangular';
      text: 'continue_with';
      width: number;
    },
  ): void;
}

type GoogleWindow = Window & {
  google?: {
    accounts?: {
      id?: GoogleIdentityApi;
    };
  };
};

@Injectable({ providedIn: 'root' })
export class GoogleIdentityService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);
  private readonly zone = inject(NgZone);
  private readonly config$ = this.http
    .get<GoogleAuthConfig>(`${environment.apiUrl}/auth/google/config`)
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));
  private scriptPromise?: Promise<void>;

  async renderButton(
    container: HTMLElement,
    onCredential: (credential: string) => void,
  ): Promise<boolean> {
    const config = await firstValueFrom(this.config$);
    if (!config.enabled || !config.clientId) {
      return false;
    }

    await this.loadScript();
    const identityApi = (this.document.defaultView as GoogleWindow | null)?.google?.accounts?.id;
    if (!identityApi) {
      throw new Error('Google Identity Services did not load');
    }

    identityApi.initialize({
      client_id: config.clientId,
      callback: (response) => this.zone.run(() => onCredential(response.credential)),
    });
    container.replaceChildren();
    identityApi.renderButton(container, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      shape: 'rectangular',
      text: 'continue_with',
      width: Math.min(Math.max(container.clientWidth, 280), 400),
    });
    return true;
  }

  private loadScript(): Promise<void> {
    const googleWindow = this.document.defaultView as GoogleWindow | null;
    if (googleWindow?.google?.accounts?.id) {
      return Promise.resolve();
    }
    if (this.scriptPromise) {
      return this.scriptPromise;
    }

    this.scriptPromise = new Promise<void>((resolve, reject) => {
      const existing = this.document.getElementById('google-identity-services');
      const script =
        existing instanceof HTMLScriptElement ? existing : this.document.createElement('script');

      script.addEventListener('load', () => resolve(), { once: true });
      script.addEventListener(
        'error',
        () => {
          this.scriptPromise = undefined;
          reject(new Error('Unable to load Google Identity Services'));
        },
        { once: true },
      );

      if (!existing) {
        script.id = 'google-identity-services';
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        this.document.head.appendChild(script);
      }
    });
    return this.scriptPromise;
  }
}
