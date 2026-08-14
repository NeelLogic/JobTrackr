import { HttpErrorResponse } from '@angular/common/http';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { apiErrorMessage } from '../../core/api-error';
import { GoogleSignInButton } from '../../shared/google-sign-in-button';
import { ThemeToggle } from '../../shared/theme-toggle';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, GoogleSignInButton, ThemeToggle],
  templateUrl: './login.html',
})
export class Login {
  readonly loading = signal(false);
  readonly error = signal('');
  readonly verificationRequired = signal(false);
  readonly notice = signal('');
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
    route: ActivatedRoute,
  ) {
    this.form = formBuilder.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
    if (route.snapshot.queryParamMap.get('reset') === 'success') {
      this.notice.set('Your password was reset. Sign in with your new password.');
    }
  }

  submit(): void {
    if (this.loading()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.verificationRequired.set(false);
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/dashboard']),
        error: (error) => {
          this.verificationRequired.set(error instanceof HttpErrorResponse && error.status === 403);
          this.error.set(apiErrorMessage(error, 'Invalid email or password.'));
        },
      });
  }

  signInWithGoogle(credential: string): void {
    if (this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth
      .loginWithGoogle(credential)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/dashboard']),
        error: (error) => this.error.set(apiErrorMessage(error, 'Unable to sign in with Google.')),
      });
  }
}
