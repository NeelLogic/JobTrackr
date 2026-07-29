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
import { ApplicationStatus, JobApplication } from '../../models/application.models';
import { FollowUpSummary } from '../../models/insights.models';

type FollowUpGroup = 'overdue' | 'dueToday' | 'upcoming' | 'stale';

@Component({
  selector: 'app-follow-ups',
  imports: [DatePipe, RouterLink],
  templateUrl: './follow-ups.html',
  styleUrl: './follow-ups.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FollowUps implements OnInit {
  private readonly api = inject(InsightsApiService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly summary = signal<FollowUpSummary | null>(null);
  readonly selectedGroup = signal<FollowUpGroup>('overdue');
  readonly selectedApplications = computed<JobApplication[]>(() => {
    const summary = this.summary();
    return summary ? summary[this.selectedGroup()] : [];
  });
  readonly groups: ReadonlyArray<{ key: FollowUpGroup; label: string }> = [
    { key: 'overdue', label: 'Overdue' },
    { key: 'dueToday', label: 'Due today' },
    { key: 'upcoming', label: 'Upcoming' },
    { key: 'stale', label: 'Stale' },
  ];

  ngOnInit(): void {
    this.load();
  }

  selectGroup(group: FollowUpGroup): void {
    this.selectedGroup.set(group);
  }

  count(group: FollowUpGroup): number {
    const summary = this.summary();
    return summary ? (summary[`${group}Count` as keyof FollowUpSummary] as number) : 0;
  }

  groupTitle(): string {
    return this.groups.find((group) => group.key === this.selectedGroup())?.label ?? 'Follow-ups';
  }

  groupDescription(): string {
    return {
      overdue: 'Follow-up dates that have already passed.',
      dueToday: 'Conversations that need attention today.',
      upcoming: 'Follow-ups scheduled during the next 14 days.',
      stale: 'Active applications without activity for at least 14 days.',
    }[this.selectedGroup()];
  }

  emptyMessage(): string {
    return {
      overdue: 'Nothing is overdue. Your follow-up schedule is on track.',
      dueToday: 'You have no follow-ups due today.',
      upcoming: 'No follow-ups are scheduled for the next 14 days.',
      stale: 'No active applications have gone stale.',
    }[this.selectedGroup()];
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

  retry(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .getFollowUps()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load follow-up insights.')),
      });
  }
}
