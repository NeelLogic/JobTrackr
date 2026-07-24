import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { AuthResponse, LoginRequest } from '../../models/auth.models';
import { Login } from './login';

describe('Login', () => {
  const response: AuthResponse = {
    token: 'signed-token',
    tokenType: 'Bearer',
    expiresIn: 86400,
    user: { id: 1, name: 'Alex Morgan', email: 'alex@example.com' },
  };

  function setup() {
    const auth = {
      login: vi.fn<(request: LoginRequest) => ReturnType<AuthService['login']>>(),
    };
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Login);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    return { auth, component, fixture, router };
  }

  it('marks invalid credentials fields without submitting', () => {
    const { auth, component } = setup();

    component.submit();

    expect(component.form.controls.email.touched).toBe(true);
    expect(component.form.controls.password.touched).toBe(true);
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('submits valid credentials and redirects to the dashboard', () => {
    const { auth, component, router } = setup();
    auth.login.mockReturnValue(of(response));
    component.form.setValue({ email: 'alex@example.com', password: 'Password1' });

    component.submit();

    expect(auth.login).toHaveBeenCalledWith({
      email: 'alex@example.com',
      password: 'Password1',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading()).toBe(false);
  });

  it('tracks the pending authentication request', () => {
    const { auth, component } = setup();
    const request = new Subject<AuthResponse>();
    auth.login.mockReturnValue(request);
    component.form.setValue({ email: 'alex@example.com', password: 'Password1' });

    component.submit();
    component.submit();

    expect(auth.login).toHaveBeenCalledOnce();
    expect(component.loading()).toBe(true);
    request.next(response);
    request.complete();
    expect(component.loading()).toBe(false);
  });

  it('shows a useful error after failed authentication', () => {
    const { auth, component } = setup();
    auth.login.mockReturnValue(throwError(() => new Error('offline')));
    component.form.setValue({ email: 'alex@example.com', password: 'Password1' });

    component.submit();

    expect(component.error()).toBe('Invalid email or password.');
    expect(component.loading()).toBe(false);
  });
});
