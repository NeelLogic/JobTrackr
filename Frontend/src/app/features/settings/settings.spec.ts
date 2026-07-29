import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { GmailIntegrationService } from '../../core/api/gmail-integration.service';
import { AuthService } from '../../core/auth.service';
import { GoogleIdentityService } from '../../core/google-identity.service';
import { ConnectedIdentity } from '../../models/auth.models';
import { GmailConnectionStatus } from '../../models/integration.models';
import { Settings } from './settings';

describe('Settings', () => {
  const disconnected: GmailConnectionStatus = {
    configured: true,
    connected: false,
    email: null,
    connectedAt: null,
    lastSyncAt: null,
  };

  function setup(
    identities: ConnectedIdentity[] = [],
    gmailStatus: GmailConnectionStatus = disconnected,
    callbackResult: string | null = null,
  ) {
    const auth = {
      user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
      connectedIdentities: vi.fn().mockReturnValue(of(identities)),
      linkGoogle: vi.fn(),
    };
    const googleIdentity = {
      renderButton: vi.fn().mockResolvedValue(false),
    };
    const gmail = {
      status: vi.fn().mockReturnValue(of(gmailStatus)),
      connect: vi.fn(),
      disconnect: vi.fn(),
      redirectToAuthorization: vi.fn(),
    };
    const router = { navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: GoogleIdentityService, useValue: googleIdentity },
        { provide: GmailIntegrationService, useValue: gmail },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(callbackResult ? { gmail: callbackResult } : {}),
            },
          },
        },
        { provide: Router, useValue: router },
      ],
    });
    const fixture = TestBed.createComponent(Settings);
    fixture.detectChanges();
    return { auth, component: fixture.componentInstance, fixture, gmail, router };
  }

  it('loads and displays a connected Google account', () => {
    const identity: ConnectedIdentity = {
      provider: 'GOOGLE',
      connectedAt: '2026-07-27T18:00:00Z',
    };
    const { auth, component, fixture } = setup([identity]);

    expect(auth.connectedIdentities).toHaveBeenCalledOnce();
    expect(component.isGoogleConnected()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Available for sign-in');
  });

  it('links Google and updates the connected state', () => {
    const { auth, component } = setup();
    const identity: ConnectedIdentity = {
      provider: 'GOOGLE',
      connectedAt: '2026-07-27T18:00:00Z',
    };
    auth.linkGoogle.mockReturnValue(of(identity));

    component.linkGoogle('google-credential');

    expect(auth.linkGoogle).toHaveBeenCalledWith('google-credential');
    expect(component.isGoogleConnected()).toBe(true);
    expect(component.success()).toContain('Google account connected');
    expect(component.linking()).toBe(false);
  });

  it('shows an error when account linking fails', () => {
    const { auth, component } = setup();
    auth.linkGoogle.mockReturnValue(throwError(() => new Error('offline')));

    component.linkGoogle('google-credential');

    expect(component.error()).toBe('Unable to connect your Google account.');
    expect(component.isGoogleConnected()).toBe(false);
  });

  it('loads and displays a connected Gmail account separately from Google sign-in', () => {
    const connected: GmailConnectionStatus = {
      configured: true,
      connected: true,
      email: 'alex@example.com',
      connectedAt: '2026-07-29T18:00:00Z',
      lastSyncAt: null,
    };
    const { gmail, fixture } = setup([], connected);

    expect(gmail.status).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Gmail application import');
    expect(fixture.nativeElement.textContent).toContain('alex@example.com');
    expect(fixture.nativeElement.textContent).toContain('read-only Gmail access');
  });

  it('starts Gmail OAuth through the backend and redirects to Google', () => {
    const { component, gmail } = setup();
    gmail.connect.mockReturnValue(
      of({ authorizationUrl: 'https://accounts.google.com/o/oauth2/v2/auth?state=secure' }),
    );

    component.connectGmail();

    expect(gmail.connect).toHaveBeenCalledOnce();
    expect(gmail.redirectToAuthorization).toHaveBeenCalledWith(
      'https://accounts.google.com/o/oauth2/v2/auth?state=secure',
    );
    expect(component.gmailConnecting()).toBe(false);
  });

  it('requires confirmation before disconnecting Gmail', () => {
    const connected: GmailConnectionStatus = {
      configured: true,
      connected: true,
      email: 'alex@example.com',
      connectedAt: '2026-07-29T18:00:00Z',
      lastSyncAt: null,
    };
    const { component, gmail } = setup([], connected);
    gmail.disconnect.mockReturnValue(of(null));

    component.requestGmailDisconnect();
    expect(component.confirmGmailDisconnect()).toBe(true);
    expect(gmail.disconnect).not.toHaveBeenCalled();

    component.disconnectGmail();
    expect(gmail.disconnect).toHaveBeenCalledOnce();
    expect(component.gmailStatus()?.connected).toBe(false);
    expect(component.success()).toContain('stored access was removed');
  });

  it('reports the OAuth callback result and removes it from the URL', () => {
    const { component, router } = setup([], disconnected, 'connected');

    expect(component.success()).toContain('Gmail connected securely');
    expect(router.navigate).toHaveBeenCalledWith([], {
      relativeTo: expect.anything(),
      queryParams: { gmail: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  });
});
