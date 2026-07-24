import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { DashboardApiService } from '../../core/api/dashboard-api.service';
import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { APPLICATION_STATUSES, ApplicationStatus } from '../../models/application.models';
import { DashboardSummary } from '../../models/dashboard.models';

interface DashboardStat {
  label: string;
  value: number;
  description: string;
  tone: 'blue' | 'violet' | 'amber' | 'green' | 'red';
}

interface StatusRow {
  status: ApplicationStatus;
  count: number;
  percentage: number;
}

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dashboard implements OnInit {
  private readonly api = inject(DashboardApiService);
  private readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly summary = signal<DashboardSummary | null>(null);
  readonly firstName = computed(() => this.auth.user()?.name.trim().split(/\s+/)[0] || 'there');
  readonly stats = computed<DashboardStat[]>(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    return [
      {
        label: 'Total applications',
        value: summary.totalApplications,
        description: 'Across your pipeline',
        tone: 'blue',
      },
      {
        label: 'This month',
        value: summary.applicationsThisMonth,
        description: 'Applications submitted',
        tone: 'violet',
      },
      {
        label: 'Interviews',
        value: summary.interviews,
        description: 'Currently interviewing',
        tone: 'amber',
      },
      {
        label: 'Offers',
        value: summary.offers,
        description: 'Offers received',
        tone: 'green',
      },
      {
        label: 'Rejections',
        value: summary.rejections,
        description: 'Closed opportunities',
        tone: 'red',
      },
    ];
  });
  readonly statusRows = computed<StatusRow[]>(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    return APPLICATION_STATUSES.map((status) => {
      const count = summary.applicationsByStatus[status] ?? 0;
      return {
        status,
        count,
        percentage:
          summary.totalApplications === 0
            ? 0
            : Math.round((count / summary.totalApplications) * 100),
      };
    });
  });

  ngOnInit(): void {
    this.load();
  }

  retry(): void {
    this.load();
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

  private load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .getSummary()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error) => this.error.set(apiErrorMessage(error, 'Unable to load your dashboard.')),
      });
  }
}
