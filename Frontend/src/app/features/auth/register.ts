import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { apiErrorMessage } from '../../core/api-error';
import { GoogleSignInButton } from '../../shared/google-sign-in-button';
import { ThemeToggle } from '../../shared/theme-toggle';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, GoogleSignInButton, ThemeToggle],
  templateUrl: './register.html',
})
export class Register {
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {
    this.form = formBuilder.nonNullable.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/),
        ],
      ],
    });
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
    this.auth
      .register(this.form.getRawValue())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () =>
          void this.router.navigate(['/verify-email'], {
            queryParams: { email: this.form.controls.email.value },
          }),
        error: (error) => this.error.set(apiErrorMessage(error, 'Unable to create your account.')),
      });
  }

  signUpWithGoogle(credential: string): void {
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
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to create your account with Google.')),
      });
  }
}
