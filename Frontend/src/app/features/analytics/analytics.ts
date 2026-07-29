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
import { apiErrorMessage } from '../../core/api-error';
import { InsightsApiService } from '../../core/api/insights-api.service';
import { APPLICATION_STATUSES, EMPLOYMENT_TYPES } from '../../models/application.models';
import { ANALYTICS_RANGES, AnalyticsRange, AnalyticsSummary } from '../../models/insights.models';

@Component({
  selector: 'app-analytics',
  imports: [DatePipe, RouterLink],
  templateUrl: './analytics.html',
  styleUrl: './analytics.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Analytics implements OnInit {
  private readonly api = inject(InsightsApiService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly range = signal<AnalyticsRange>('THIRTY_DAYS');
  readonly summary = signal<AnalyticsSummary | null>(null);
  readonly ranges = ANALYTICS_RANGES;
  readonly statusDistribution = computed(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    return APPLICATION_STATUSES.map((status) => ({
      label: this.enumLabel(status),
      value: summary.applicationsByStatus[status] ?? 0,
    })).filter((item) => item.value > 0);
  });
  readonly employmentDistribution = computed(() => {
    const summary = this.summary();
    if (!summary) {
      return [];
    }
    return EMPLOYMENT_TYPES.map((type) => ({
      label: this.enumLabel(type),
      value: summary.applicationsByEmploymentType[type] ?? 0,
    })).filter((item) => item.value > 0);
  });
  readonly maxTrendValue = computed(() =>
    Math.max(1, ...(this.summary()?.trend.map((point) => point.applications) ?? [1])),
  );

  ngOnInit(): void {
    this.load();
  }

  selectRange(range: AnalyticsRange): void {
    if (range === this.range() || this.loading()) {
      return;
    }
    this.range.set(range);
    this.load();
  }

  retry(): void {
    this.load();
  }

  rangeLabel(range: AnalyticsRange): string {
    return {
      THIRTY_DAYS: '30 days',
      NINETY_DAYS: '90 days',
      SIX_MONTHS: '6 months',
      ALL_TIME: 'All time',
    }[range];
  }

  enumLabel(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  trendHeight(value: number): number {
    return value === 0 ? 0 : Math.max(8, Math.round((value / this.maxTrendValue()) * 100));
  }

  distributionWidth(value: number): number {
    const total = this.summary()?.applicationsInRange ?? 0;
    return total === 0 ? 0 : Math.round((value / total) * 100);
  }

  private load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .getAnalytics(this.range())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load application analytics.')),
      });
  }
}
