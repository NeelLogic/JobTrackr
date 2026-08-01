import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApplicationApiService } from '../../core/api/application-api.service';
import { apiErrorMessage } from '../../core/api-error';
import { ApplicationStatus, JobApplication } from '../../models/application.models';
import { DeleteConfirmationDialog } from '../../shared/delete-confirmation-dialog';

@Component({
  selector: 'app-application-detail',
  imports: [DatePipe, RouterLink, DeleteConfirmationDialog],
  templateUrl: './application-detail.html',
  styleUrl: './application-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApplicationDetail implements OnInit {
  private readonly api = inject(ApplicationApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly applicationId = signal<number | null>(null);
  readonly application = signal<JobApplication | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly confirmingDelete = signal(false);
  readonly deleting = signal(false);
  readonly deleteError = signal('');
  readonly salary = computed(() => {
    const application = this.application();
    return application ? this.salaryLabel(application) : 'Not provided';
  });

  ngOnInit(): void {
    const routeId = this.route.snapshot.paramMap.get('id');
    const id = Number(routeId);
    if (!routeId || !Number.isInteger(id) || id <= 0) {
      this.loading.set(false);
      this.error.set('This application link is invalid.');
      return;
    }

    this.applicationId.set(id);
    this.load(id);
  }

  retry(): void {
    const id = this.applicationId();
    if (id !== null) {
      this.load(id);
    }
  }

  requestDelete(): void {
    this.deleteError.set('');
    this.confirmingDelete.set(true);
  }

  cancelDelete(): void {
    if (!this.deleting()) {
      this.confirmingDelete.set(false);
    }
  }

  deleteApplication(): void {
    const id = this.applicationId();
    if (id === null || this.deleting()) {
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

  private load(id: number): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .get(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (application) => this.application.set(application),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load this application.')),
      });
  }

  private salaryLabel(application: JobApplication): string {
    const { salaryMin, salaryMax, salaryCurrency } = application;
    if (salaryMin === null && salaryMax === null) {
      return 'Not provided';
    }

    const format = (amount: number): string => {
      if (!salaryCurrency) {
        return amount.toLocaleString('en-CA');
      }
      try {
        return new Intl.NumberFormat('en-CA', {
          style: 'currency',
          currency: salaryCurrency,
          maximumFractionDigits: 0,
        }).format(amount);
      } catch {
        return `${salaryCurrency} ${amount.toLocaleString('en-CA')}`;
      }
    };

    if (salaryMin !== null && salaryMax !== null) {
      return `${format(salaryMin)} - ${format(salaryMax)}`;
    }
    return salaryMin !== null ? `From ${format(salaryMin)}` : `Up to ${format(salaryMax!)}`;
  }
}
