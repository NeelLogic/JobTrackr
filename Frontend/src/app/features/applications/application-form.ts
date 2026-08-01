import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { apiErrorMessage, apiFieldErrors } from '../../core/api-error';
import {
  APPLICATION_STATUSES,
  EMPLOYMENT_TYPES,
  SALARY_CURRENCIES,
  ApplicationRequest,
  ApplicationStatus,
  EmploymentType,
  JobApplication,
} from '../../models/application.models';
import {
  MAX_SUPPORTED_DATE,
  MIN_SUPPORTED_DATE,
  fourDigitDateYear,
} from '../../shared/date.validators';
import { DeleteConfirmationDialog } from '../../shared/delete-confirmation-dialog';

type FieldName =
  | 'company'
  | 'jobTitle'
  | 'location'
  | 'jobUrl'
  | 'applicationDate'
  | 'status'
  | 'employmentType'
  | 'salaryMin'
  | 'salaryMax'
  | 'salaryCurrency'
  | 'notes'
  | 'followUpDate';

const applicationRules: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const status = control.get('status')?.value as ApplicationStatus | null;
  const applicationDate = control.get('applicationDate')?.value as string | null;
  const salaryMin = control.get('salaryMin')?.value as number | null;
  const salaryMax = control.get('salaryMax')?.value as number | null;
  const currency = control.get('salaryCurrency')?.value as string | null;
  const errors: ValidationErrors = {};

  if (status && status !== 'SAVED' && !applicationDate) {
    errors['applicationDateRequired'] = true;
  }
  if ((salaryMin !== null || salaryMax !== null) && !currency?.trim()) {
    errors['salaryCurrencyRequired'] = true;
  }
  if (salaryMin !== null && salaryMax !== null && salaryMin > salaryMax) {
    errors['salaryRange'] = true;
  }

  return Object.keys(errors).length ? errors : null;
};

function localDate(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

const notFutureDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.value && control.value > localDate() ? { futureDate: true } : null;

@Component({
  selector: 'app-application-form',
  imports: [ReactiveFormsModule, RouterLink, DeleteConfirmationDialog],
  templateUrl: './application-form.html',
  styleUrl: './application-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationForm implements OnInit {
  private readonly api = inject(ApplicationApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly applicationId = signal<number | null>(null);
  readonly isEdit = computed(() => this.applicationId() !== null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly submitted = signal(false);
  readonly loadError = signal('');
  readonly saveError = signal('');
  readonly application = signal<JobApplication | null>(null);
  readonly confirmingDelete = signal(false);
  readonly deleting = signal(false);
  readonly deleteError = signal('');
  readonly serverErrors = signal<Record<string, string>>({});
  readonly maxApplicationDate = localDate();
  readonly minSupportedDate = MIN_SUPPORTED_DATE;
  readonly maxSupportedDate = MAX_SUPPORTED_DATE;
  readonly statuses = APPLICATION_STATUSES;
  readonly employmentTypes = EMPLOYMENT_TYPES;
  readonly salaryCurrencies = SALARY_CURRENCIES;
  readonly cancelLink = computed(() => {
    const id = this.applicationId();
    return id === null ? ['/applications'] : ['/applications', id];
  });

  readonly form = this.formBuilder.nonNullable.group(
    {
      company: ['', [Validators.required, Validators.maxLength(120)]],
      jobTitle: ['', [Validators.required, Validators.maxLength(160)]],
      location: ['', Validators.maxLength(160)],
      jobUrl: ['', [Validators.maxLength(1000), Validators.pattern(/^https?:\/\/\S+$/i)]],
      applicationDate: ['', [fourDigitDateYear, notFutureDate]],
      status: this.formBuilder.nonNullable.control<ApplicationStatus>('SAVED', Validators.required),
      employmentType: this.formBuilder.nonNullable.control<EmploymentType>(
        'FULL_TIME',
        Validators.required,
      ),
      salaryMin: this.formBuilder.control<number | null>(null, Validators.min(0)),
      salaryMax: this.formBuilder.control<number | null>(null, Validators.min(0)),
      salaryCurrency: ['', Validators.pattern(/^[A-Za-z]{3}$/)],
      notes: ['', Validators.maxLength(10000)],
      followUpDate: ['', fourDigitDateYear],
    },
    { validators: applicationRules },
  );

  private readonly messages: Partial<Record<FieldName, Record<string, string>>> = {
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
    applicationDate: {
      dateYear: 'Application date must use a four-digit year.',
      futureDate: 'Application date cannot be in the future.',
    },
    salaryMin: { min: 'Minimum salary cannot be negative.' },
    salaryMax: { min: 'Maximum salary cannot be negative.' },
    salaryCurrency: { pattern: 'Select a supported currency.' },
    notes: { maxlength: 'Notes must be 10,000 characters or fewer.' },
    followUpDate: { dateYear: 'Follow-up date must use a four-digit year.' },
  };

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    if (routeId === null) {
      return;
    }

    const id = Number(routeId);
    if (!Number.isInteger(id) || id <= 0) {
      this.loadError.set('This application link is invalid.');
      return;
    }

    this.applicationId.set(id);
    this.loadApplication(id);
  }

  submit(): void {
    if (this.saving() || this.loading()) {
      return;
    }

    this.submitted.set(true);
    this.serverErrors.set({});
    this.saveError.set('');
    this.form.updateValueAndValidity();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const id = this.applicationId();
    const request = this.toRequest();
    const operation = id === null ? this.api.create(request) : this.api.update(id, request);

    this.saving.set(true);
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (application) => void this.router.navigate(['/applications', application.id]),
      error: (error) => {
        this.serverErrors.set(apiFieldErrors(error));
        this.saveError.set(apiErrorMessage(error, 'Unable to save this application.'));
      },
    });
  }

  retryLoad(): void {
    const id = this.applicationId();
    if (id !== null) {
      this.loadApplication(id);
    }
  }

  requestDelete(): void {
    if (this.application()) {
      this.deleteError.set('');
      this.confirmingDelete.set(true);
    }
  }

  cancelDelete(): void {
    if (!this.deleting()) {
      this.confirmingDelete.set(false);
      this.deleteError.set('');
    }
  }

  deleteApplication(): void {
    const id = this.applicationId();
    if (id === null || this.deleting() || this.saving()) {
      return;
    }

    this.deleting.set(true);
    this.deleteError.set('');
    this.api
      .delete(id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/applications']),
        error: (error) =>
          this.deleteError.set(apiErrorMessage(error, 'Unable to delete this application.')),
      });
  }

  fieldError(field: FieldName): string | null {
    const serverError = this.serverErrors()[field];
    if (serverError) {
      return serverError;
    }

    const control = this.form.controls[field];
    if ((!control.touched && !this.submitted()) || !control.errors) {
      return null;
    }

    const key = Object.keys(control.errors)[0];
    return key ? (this.messages[field]?.[key] ?? 'Check this value.') : null;
  }

  enumLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private loadApplication(id: number): void {
    this.loading.set(true);
    this.loadError.set('');
    this.api
      .get(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (application) => this.populateForm(application),
        error: (error) =>
          this.loadError.set(apiErrorMessage(error, 'Unable to load this application.')),
      });
  }

  private populateForm(application: JobApplication): void {
    this.application.set(application);
    this.form.reset({
      company: application.company,
      jobTitle: application.jobTitle,
      location: application.location ?? '',
      jobUrl: application.jobUrl ?? '',
      applicationDate: application.applicationDate ?? '',
      status: application.status,
      employmentType: application.employmentType,
      salaryMin: application.salaryMin,
      salaryMax: application.salaryMax,
      salaryCurrency: application.salaryCurrency ?? '',
      notes: application.notes ?? '',
      followUpDate: application.followUpDate ?? '',
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
      salaryMin: value.salaryMin,
      salaryMax: value.salaryMax,
      salaryCurrency: this.optional(value.salaryCurrency)?.toUpperCase() ?? null,
      notes: this.optional(value.notes),
      followUpDate: value.followUpDate || null,
    };
  }

  private optional(value: string): string | null {
    const normalized = value.trim();
    return normalized || null;
  }
}
