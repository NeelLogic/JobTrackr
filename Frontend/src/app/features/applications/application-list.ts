import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { apiErrorMessage } from '../../core/api-error';
import {
  APPLICATION_STATUSES,
  EMPLOYMENT_TYPES,
  ApplicationSortField,
  ApplicationStatus,
  EmploymentType,
  JobApplication,
  PageResponse,
  SortDirection,
} from '../../models/application.models';

@Component({
  selector: 'app-application-list',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './application-list.html',
  styleUrl: './application-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationList implements OnInit {
  private readonly api = inject(ApplicationApiService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly result = signal<PageResponse<JobApplication> | null>(null);
  readonly applications = computed(() => this.result()?.content ?? []);
  readonly rangeStart = computed(() => {
    const result = this.result();
    return result && result.totalElements > 0 ? result.page * result.size + 1 : 0;
  });
  readonly rangeEnd = computed(() => {
    const result = this.result();
    return result ? Math.min((result.page + 1) * result.size, result.totalElements) : 0;
  });

  readonly statuses = APPLICATION_STATUSES;
  readonly employmentTypes = EMPLOYMENT_TYPES;
  readonly pageSizes = [10, 20, 50] as const;
  readonly sortOptions: ReadonlyArray<{ value: ApplicationSortField; label: string }> = [
    { value: 'updatedAt', label: 'Recently updated' },
    { value: 'applicationDate', label: 'Application date' },
    { value: 'company', label: 'Company' },
    { value: 'jobTitle', label: 'Job title' },
    { value: 'status', label: 'Status' },
    { value: 'followUpDate', label: 'Follow-up date' },
    { value: 'createdAt', label: 'Date added' },
  ];

  readonly filters = this.formBuilder.nonNullable.group({
    search: '',
    status: this.formBuilder.nonNullable.control<ApplicationStatus | ''>(''),
    employmentType: this.formBuilder.nonNullable.control<EmploymentType | ''>(''),
    sort: this.formBuilder.nonNullable.control<ApplicationSortField>('updatedAt'),
    direction: this.formBuilder.nonNullable.control<SortDirection>('desc'),
    size: 10,
  });

  ngOnInit(): void {
    this.load(0);
  }

  applyFilters(): void {
    this.load(0);
  }

  clearFilters(): void {
    if (this.loading()) {
      return;
    }

    this.filters.reset({
      search: '',
      status: '',
      employmentType: '',
      sort: 'updatedAt',
      direction: 'desc',
      size: 10,
    });
    this.load(0);
  }

  retry(): void {
    this.load(this.result()?.page ?? 0);
  }

  goToPage(page: number): void {
    const result = this.result();
    if (!result || page < 0 || page >= result.totalPages || page === result.page) {
      return;
    }
    this.load(page);
  }

  hasActiveFilters(): boolean {
    const { search, status, employmentType } = this.filters.getRawValue();
    return Boolean(search.trim() || status || employmentType);
  }

  enumLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  statusClass(status: ApplicationStatus): string {
    return `status-badge status-badge--${status.toLowerCase()}`;
  }

  private load(page: number): void {
    if (this.loading()) {
      return;
    }

    const filters = this.filters.getRawValue();
    this.loading.set(true);
    this.error.set('');

    this.api
      .list({ ...filters, page })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => this.result.set(result),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load your applications.')),
      });
  }
}
