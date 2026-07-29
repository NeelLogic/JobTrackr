import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { GoogleIdentityService } from '../../core/google-identity.service';
import { ConnectedIdentity } from '../../models/auth.models';
import { Settings } from './settings';

describe('Settings', () => {
  function setup(identities: ConnectedIdentity[] = []) {
    const auth = {
      user: signal({ id: 1, name: 'Alex Morgan', email: 'alex@example.com' }),
      connectedIdentities: vi.fn().mockReturnValue(of(identities)),
      linkGoogle: vi.fn(),
    };
    const googleIdentity = {
      renderButton: vi.fn().mockResolvedValue(false),
    };
    TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: GoogleIdentityService, useValue: googleIdentity },
      ],
    });
    const fixture = TestBed.createComponent(Settings);
    fixture.detectChanges();
    return { auth, component: fixture.componentInstance, fixture };
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
});
