import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { GoogleIdentityService } from '../core/google-identity.service';
import { GoogleSignInButton } from './google-sign-in-button';

describe('GoogleSignInButton', () => {
  it('emits the credential returned by Google Identity Services', async () => {
    let callback: ((credential: string) => void) | undefined;
    const googleIdentity = {
      renderButton: vi
        .fn()
        .mockImplementation(
          async (_container: HTMLElement, onCredential: (credential: string) => void) => {
            callback = onCredential;
            return true;
          },
        ),
    };
    TestBed.configureTestingModule({
      imports: [GoogleSignInButton],
      providers: [{ provide: GoogleIdentityService, useValue: googleIdentity }],
    });
    const fixture = TestBed.createComponent(GoogleSignInButton);
    const emitted = vi.fn();
    fixture.componentInstance.credentialSelected.subscribe(emitted);

    fixture.detectChanges();
    await fixture.whenStable();
    callback?.('signed-google-credential');

    expect(googleIdentity.renderButton).toHaveBeenCalledOnce();
    expect(emitted).toHaveBeenCalledWith('signed-google-credential');
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('shows a retryable message when Google cannot load', async () => {
    const googleIdentity = {
      renderButton: vi.fn().mockRejectedValue(new Error('offline')),
    };
    TestBed.configureTestingModule({
      imports: [GoogleSignInButton],
      providers: [{ provide: GoogleIdentityService, useValue: googleIdentity }],
    });
    const fixture = TestBed.createComponent(GoogleSignInButton);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Google sign-in is temporarily unavailable.',
    );
    expect(fixture.nativeElement.querySelector('button')?.textContent).toContain('Try again');
  });
});
