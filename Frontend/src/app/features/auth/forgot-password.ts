import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ThemeToggle } from '../../shared/theme-toggle';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, ThemeToggle],
  templateUrl: './forgot-password.html',
})
export class ForgotPassword {
  readonly codeSent = signal(false);
  readonly loading = signal(false);
  readonly resending = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly requestForm;
  readonly resetForm;

  constructor(
    formBuilder: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {
    this.requestForm = formBuilder.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
    });
    this.resetForm = formBuilder.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/),
        ],
      ],
      confirmPassword: ['', Validators.required],
    });
  }

  requestCode(): void {
    if (this.loading()) {
      return;
    }
    if (this.requestForm.invalid) {
      this.requestForm.markAllAsTouched();
      return;
    }
    this.sendCode(false);
  }

  resendCode(): void {
    if (this.resending()) {
      return;
    }
    if (this.resetForm.controls.email.invalid) {
      this.resetForm.controls.email.markAsTouched();
      return;
    }
    this.sendCode(true);
  }

  resetPassword(): void {
    if (this.loading()) {
      return;
    }
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }
    if (this.resetForm.controls.password.value !== this.resetForm.controls.confirmPassword.value) {
      this.resetForm.controls.confirmPassword.setErrors({ mismatch: true });
      this.resetForm.controls.confirmPassword.markAsTouched();
      return;
    }

    const { email, code, password } = this.resetForm.getRawValue();
    this.loading.set(true);
    this.error.set('');
    this.auth
      .resetPassword({ email, code, password })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/login'], { queryParams: { reset: 'success' } }),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'The code is invalid or has expired.')),
      });
  }

  startOver(): void {
    this.codeSent.set(false);
    this.error.set('');
    this.message.set('');
    this.resetForm.reset();
  }

  private sendCode(resend: boolean): void {
    const email = resend
      ? this.resetForm.controls.email.value
      : this.requestForm.controls.email.value;
    resend ? this.resending.set(true) : this.loading.set(true);
    this.error.set('');
    this.message.set('');
    this.auth
      .requestPasswordReset({ email })
      .pipe(
        finalize(() => {
          resend ? this.resending.set(false) : this.loading.set(false);
        }),
      )
      .subscribe({
        next: (response) => {
          this.resetForm.controls.email.setValue(email);
          this.codeSent.set(true);
          this.message.set(response.message);
        },
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to send a password reset code.')),
      });
  }
}
