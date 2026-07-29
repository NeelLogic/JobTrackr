import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { GoogleIdentityService } from '../../core/google-identity.service';
import { AuthService } from '../../core/auth.service';
import { AuthResponse, RegisterRequest } from '../../models/auth.models';
import { Register } from './register';

describe('Register', () => {
  const response: AuthResponse = {
    token: 'signed-token',
    tokenType: 'Bearer',
    expiresIn: 86400,
    user: { id: 1, name: 'Alex Morgan', email: 'alex@example.com' },
  };

  function setup() {
    const auth = {
      register: vi.fn<(request: RegisterRequest) => ReturnType<AuthService['register']>>(),
      loginWithGoogle: vi.fn<(credential: string) => ReturnType<AuthService['loginWithGoogle']>>(),
    };
    const googleIdentity = {
      renderButton: vi.fn().mockResolvedValue(false),
    };
    TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: GoogleIdentityService, useValue: googleIdentity },
      ],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Register);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { auth, component, fixture, router };
  }

  it('requires a valid name, email, and strong password', () => {
    const { auth, component } = setup();
    component.form.setValue({
      name: '',
      email: 'invalid',
      password: 'password',
    });

    component.submit();

    expect(component.form.invalid).toBe(true);
    expect(component.form.controls.name.touched).toBe(true);
    expect(component.form.controls.email.hasError('email')).toBe(true);
    expect(component.form.controls.password.hasError('pattern')).toBe(true);
    expect(auth.register).not.toHaveBeenCalled();
  });

  it('registers a valid account and redirects to the dashboard', () => {
    const { auth, component, router } = setup();
    auth.register.mockReturnValue(of(response));
    component.form.setValue({
      name: 'Alex Morgan',
      email: 'alex@example.com',
      password: 'Password1',
    });

    component.submit();

    expect(auth.register).toHaveBeenCalledWith({
      name: 'Alex Morgan',
      email: 'alex@example.com',
      password: 'Password1',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading()).toBe(false);
  });

  it('shows a useful error when account creation fails', () => {
    const { auth, component } = setup();
    auth.register.mockReturnValue(throwError(() => new Error('offline')));
    component.form.setValue({
      name: 'Alex Morgan',
      email: 'alex@example.com',
      password: 'Password1',
    });

    component.submit();

    expect(component.error()).toBe('Unable to create your account.');
    expect(component.loading()).toBe(false);
  });

  it('creates an account with a Google credential', () => {
    const { auth, component, router } = setup();
    auth.loginWithGoogle.mockReturnValue(of(response));

    component.signUpWithGoogle('google-credential');

    expect(auth.loginWithGoogle).toHaveBeenCalledWith('google-credential');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});
