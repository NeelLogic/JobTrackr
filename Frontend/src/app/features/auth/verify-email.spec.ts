import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { VerifyEmail } from './verify-email';

describe('VerifyEmail', () => {
  function setup() {
    const auth = {
      verifyEmail:
        vi.fn<
          (request: { email: string; code: string }) => ReturnType<AuthService['verifyEmail']>
        >(),
      resendEmailVerification:
        vi.fn<(request: { email: string }) => ReturnType<AuthService['resendEmailVerification']>>(),
    };
    TestBed.configureTestingModule({
      imports: [VerifyEmail],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(VerifyEmail);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { auth, component, router };
  }

  it('requires a valid email and six-digit code', () => {
    const { auth, component } = setup();
    component.form.setValue({ email: 'invalid', code: '123' });

    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(auth.verifyEmail).not.toHaveBeenCalled();
  });

  it('confirms the code and continues to the dashboard', () => {
    const { auth, component, router } = setup();
    auth.verifyEmail.mockReturnValue(
      of({
        token: 'token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: { id: 1, name: 'Alex', email: 'alex@example.com' },
      }),
    );
    component.form.setValue({ email: 'alex@example.com', code: '123456' });

    component.submit();

    expect(auth.verifyEmail).toHaveBeenCalledWith({
      email: 'alex@example.com',
      code: '123456',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('shows invalid or expired code errors', () => {
    const { auth, component } = setup();
    auth.verifyEmail.mockReturnValue(throwError(() => new Error('offline')));
    component.form.setValue({ email: 'alex@example.com', code: '123456' });

    component.submit();

    expect(component.error()).toBe('The code is invalid or has expired.');
  });
});
