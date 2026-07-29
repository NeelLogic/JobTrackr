import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { GmailIntegrationService } from '../../core/api/gmail-integration.service';
import { apiErrorMessage, apiFieldErrors } from '../../core/api-error';
import {
  APPLICATION_STATUSES,
  EMPLOYMENT_TYPES,
  ApplicationRequest,
  ApplicationStatus,
  EmploymentType,
} from '../../models/application.models';
import { GmailConnectionStatus, GmailImportCandidate } from '../../models/integration.models';

type ReviewField =
  | 'company'
  | 'jobTitle'
  | 'location'
  | 'jobUrl'
  | 'applicationDate'
  | 'status'
  | 'employmentType'
  | 'notes'
  | 'followUpDate';

function localDate(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

const notFutureDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.value && control.value > localDate() ? { futureDate: true } : null;

const applicationDateRequired: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const status = control.get('status')?.value as ApplicationStatus | null;
  const applicationDate = control.get('applicationDate')?.value as string | null;
  return status && status !== 'SAVED' && !applicationDate
    ? { applicationDateRequired: true }
    : null;
};

@Component({
  selector: 'app-gmail-import',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './gmail-import.html',
  styleUrl: './gmail-import.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GmailImport implements OnInit {
  private readonly gmail = inject(GmailIntegrationService);
  private readonly formBuilder = inject(FormBuilder);

  readonly statusLoading = signal(true);
  readonly candidatesLoading = signal(false);
  readonly scanning = signal(false);
  readonly importing = signal(false);
  readonly dismissingId = signal<number | null>(null);
  readonly dismissConfirmationId = signal<number | null>(null);
  readonly submitted = signal(false);
  readonly connection = signal<GmailConnectionStatus | null>(null);
  readonly candidates = signal<GmailImportCandidate[]>([]);
  readonly selectedId = signal<number | null>(null);
  readonly error = signal('');
  readonly success = signal('');
  readonly serverErrors = signal<Record<string, string>>({});
  readonly importedApplicationId = signal<number | null>(null);
  readonly statuses = APPLICATION_STATUSES;
  readonly employmentTypes = EMPLOYMENT_TYPES;
  readonly maxApplicationDate = localDate();
  readonly selectedCandidate = computed(() => {
    const selectedId = this.selectedId();
    return this.candidates().find((candidate) => candidate.id === selectedId) ?? null;
  });

  readonly form = this.formBuilder.nonNullable.group(
    {
      company: ['', [Validators.required, Validators.maxLength(120)]],
      jobTitle: ['', [Validators.required, Validators.maxLength(160)]],
      location: ['', Validators.maxLength(160)],
      jobUrl: ['', [Validators.maxLength(1000), Validators.pattern(/^https?:\/\/\S+$/i)]],
      applicationDate: ['', notFutureDate],
      status: this.formBuilder.nonNullable.control<ApplicationStatus>(
        'APPLIED',
        Validators.required,
      ),
      employmentType: this.formBuilder.nonNullable.control<EmploymentType>(
        'FULL_TIME',
        Validators.required,
      ),
      notes: ['', Validators.maxLength(10000)],
      followUpDate: [''],
    },
    { validators: applicationDateRequired },
  );

  private readonly fieldMessages: Partial<Record<ReviewField, Record<string, string>>> = {
    company: {
      required: 'Company is required.',
      maxlength: 'Company must be 120 characters or fewer.',
    },
    jobTitle: {
      required: 'Job title is required.',
      maxlength: 'Job title must be 160 characters or fewer.',
    },
    location: { maxlength: 'Location must be 160 characters or fewer.' },
    jobUrl: {
      pattern: 'Enter a complete HTTP or HTTPS URL.',
      maxlength: 'Job URL must be 1,000 characters or fewer.',
    },
    applicationDate: { futureDate: 'Application date cannot be in the future.' },
    notes: { maxlength: 'Notes must be 10,000 characters or fewer.' },
  };

  ngOnInit(): void {
    this.loadConnection();
  }

  scan(): void {
    if (this.scanning() || !this.connection()?.connected) {
      return;
    }
    this.scanning.set(true);
    this.clearMessages();
    this.gmail
      .scan()
      .pipe(finalize(() => this.scanning.set(false)))
      .subscribe({
        next: (result) => {
          this.candidates.set(result.candidates);
          this.selectedId.set(null);
          this.connection.update((status) =>
            status ? { ...status, lastSyncAt: new Date().toISOString() } : status,
          );
          this.success.set(
            result.candidatesAdded === 0
              ? `Scan complete. No new applications found in ${result.messagesScanned} messages.`
              : `Found ${result.candidatesAdded} new application ${
                  result.candidatesAdded === 1 ? 'candidate' : 'candidates'
                } to review.`,
          );
        },
        error: (error) => this.error.set(apiErrorMessage(error, 'Unable to scan Gmail right now.')),
      });
  }

  selectCandidate(candidate: GmailImportCandidate): void {
    this.selectedId.set(candidate.id);
    this.dismissConfirmationId.set(null);
    this.submitted.set(false);
    this.serverErrors.set({});
    this.error.set('');
    this.success.set('');
    this.form.reset({
      company: candidate.company,
      jobTitle: candidate.jobTitle,
      location: candidate.location ?? '',
      jobUrl: candidate.jobUrl ?? '',
      applicationDate: candidate.applicationDate,
      status: candidate.status,
      employmentType: candidate.employmentType,
      notes: '',
      followUpDate: '',
    });
  }

  cancelReview(): void {
    if (this.importing()) {
      return;
    }
    this.selectedId.set(null);
    this.submitted.set(false);
    this.serverErrors.set({});
  }

  importSelected(): void {
    const candidate = this.selectedCandidate();
    if (!candidate || this.importing()) {
      return;
    }
    this.submitted.set(true);
    this.serverErrors.set({});
    this.error.set('');
    this.success.set('');
    this.form.updateValueAndValidity();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.importing.set(true);
    this.gmail
      .importCandidate(candidate.id, this.toRequest())
      .pipe(finalize(() => this.importing.set(false)))
      .subscribe({
        next: (application) => {
          this.candidates.update((items) => items.filter((item) => item.id !== candidate.id));
          this.selectedId.set(null);
          this.importedApplicationId.set(application.id);
          this.success.set(`${application.company} was added to your applications.`);
        },
        error: (error) => {
          this.serverErrors.set(apiFieldErrors(error));
          this.error.set(apiErrorMessage(error, 'Unable to import this application.'));
        },
      });
  }

  requestDismiss(candidateId: number): void {
    this.dismissConfirmationId.set(candidateId);
    this.error.set('');
    this.success.set('');
  }

  cancelDismiss(): void {
    this.dismissConfirmationId.set(null);
  }

  dismiss(candidateId: number): void {
    if (this.dismissingId() !== null) {
      return;
    }
    this.dismissingId.set(candidateId);
    this.error.set('');
    this.success.set('');
    this.gmail
      .dismissCandidate(candidateId)
      .pipe(finalize(() => this.dismissingId.set(null)))
      .subscribe({
        next: () => {
          this.candidates.update((items) => items.filter((item) => item.id !== candidateId));
          if (this.selectedId() === candidateId) {
            this.selectedId.set(null);
          }
          this.dismissConfirmationId.set(null);
          this.success.set('Suggestion dismissed. It will not be shown again.');
        },
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to dismiss this suggestion.')),
      });
  }

  retry(): void {
    this.loadConnection();
  }

  fieldError(field: ReviewField): string | null {
    const serverError = this.serverErrors()[field];
    if (serverError) {
      return serverError;
    }
    const control = this.form.controls[field];
    if ((!control.touched && !this.submitted()) || !control.errors) {
      return null;
    }
    const key = Object.keys(control.errors)[0];
    return key ? (this.fieldMessages[field]?.[key] ?? 'Check this value.') : null;
  }

  enumLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private loadConnection(): void {
    this.statusLoading.set(true);
    this.error.set('');
    this.gmail
      .status()
      .pipe(finalize(() => this.statusLoading.set(false)))
      .subscribe({
        next: (status) => {
          this.connection.set(status);
          if (status.connected) {
            this.loadCandidates();
          }
        },
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load the Gmail connection.')),
      });
  }

  private loadCandidates(): void {
    this.candidatesLoading.set(true);
    this.gmail
      .candidates()
      .pipe(finalize(() => this.candidatesLoading.set(false)))
      .subscribe({
        next: (candidates) => this.candidates.set(candidates),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load Gmail import suggestions.')),
      });
  }

  private toRequest(): ApplicationRequest {
    const value = this.form.getRawValue();
    return {
      company: value.company.trim(),
      jobTitle: value.jobTitle.trim(),
      location: this.optional(value.location),
      jobUrl: this.optional(value.jobUrl),
      applicationDate: value.applicationDate || null,
      status: value.status,
      employmentType: value.employmentType,
      salaryMin: null,
      salaryMax: null,
      salaryCurrency: null,
      notes: this.optional(value.notes),
      followUpDate: value.followUpDate || null,
    };
  }

  private optional(value: string): string | null {
    const normalized = value.trim();
    return normalized || null;
  }

  private clearMessages(): void {
    this.error.set('');
    this.success.set('');
    this.importedApplicationId.set(null);
  }
}
