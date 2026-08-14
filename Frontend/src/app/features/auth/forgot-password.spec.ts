import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { ForgotPassword } from './forgot-password';

describe('ForgotPassword', () => {
  function setup() {
    const auth = {
      requestPasswordReset:
        vi.fn<(request: { email: string }) => ReturnType<AuthService['requestPasswordReset']>>(),
      resetPassword:
        vi.fn<
          (request: {
            email: string;
            code: string;
            password: string;
          }) => ReturnType<AuthService['resetPassword']>
        >(),
    };
    TestBed.configureTestingModule({
      imports: [ForgotPassword],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ForgotPassword);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { auth, component, router };
  }

  it('requests a code without exposing whether the account exists', () => {
    const { auth, component } = setup();
    auth.requestPasswordReset.mockReturnValue(
      of({ message: 'If an eligible account exists, a code has been sent' }),
    );
    component.requestForm.setValue({ email: 'alex@example.com' });

    component.requestCode();

    expect(auth.requestPasswordReset).toHaveBeenCalledWith({ email: 'alex@example.com' });
    expect(component.codeSent()).toBe(true);
    expect(component.resetForm.controls.email.value).toBe('alex@example.com');
  });

  it('does not submit mismatched replacement passwords', () => {
    const { auth, component } = setup();
    component.resetForm.setValue({
      email: 'alex@example.com',
      code: '123456',
      password: 'NewPassword2',
      confirmPassword: 'DifferentPassword3',
    });

    component.resetPassword();

    expect(component.resetForm.controls.confirmPassword.hasError('mismatch')).toBe(true);
    expect(auth.resetPassword).not.toHaveBeenCalled();
  });

  it('resets the password with the OTP and returns to sign in', () => {
    const { auth, component, router } = setup();
    auth.resetPassword.mockReturnValue(of({ message: 'Password reset' }));
    component.resetForm.setValue({
      email: 'alex@example.com',
      code: '123456',
      password: 'NewPassword2',
      confirmPassword: 'NewPassword2',
    });

    component.resetPassword();

    expect(auth.resetPassword).toHaveBeenCalledWith({
      email: 'alex@example.com',
      code: '123456',
      password: 'NewPassword2',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reset: 'success' },
    });
  });
});
