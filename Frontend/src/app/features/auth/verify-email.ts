import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ThemeToggle } from '../../shared/theme-toggle';

@Component({
  selector: 'app-verify-email',
  imports: [ReactiveFormsModule, RouterLink, ThemeToggle],
  templateUrl: './verify-email.html',
})
export class VerifyEmail {
  readonly loading = signal(false);
  readonly resending = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly form;

  constructor(
    formBuilder: FormBuilder,
    route: ActivatedRoute,
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {
    this.form = formBuilder.nonNullable.group({
      email: [
        route.snapshot.queryParamMap.get('email') ?? '',
        [Validators.required, Validators.email],
      ],
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
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
    this.message.set('');
    this.auth
      .verifyEmail(this.form.getRawValue())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/dashboard']),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'The code is invalid or has expired.')),
      });
  }

  resend(): void {
    if (this.resending() || this.form.controls.email.invalid) {
      this.form.controls.email.markAsTouched();
      return;
    }

    this.resending.set(true);
    this.error.set('');
    this.message.set('');
    this.auth
      .resendEmailVerification({ email: this.form.controls.email.value })
      .pipe(finalize(() => this.resending.set(false)))
      .subscribe({
        next: (response) => this.message.set(response.message),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to send a new verification code.')),
      });
  }
}
