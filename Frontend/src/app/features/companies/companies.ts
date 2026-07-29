import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { apiErrorMessage } from '../../core/api-error';
import { InsightsApiService } from '../../core/api/insights-api.service';
import { CompaniesSummary, CompanySort } from '../../models/insights.models';

@Component({
  selector: 'app-companies',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './companies.html',
  styleUrl: './companies.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Companies implements OnInit {
  private readonly api = inject(InsightsApiService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly summary = signal<CompaniesSummary | null>(null);
  readonly sortOptions: ReadonlyArray<{ value: CompanySort; label: string }> = [
    { value: 'applications', label: 'Most applications' },
    { value: 'recent', label: 'Recent activity' },
    { value: 'interviews', label: 'Most interviews' },
    { value: 'offers', label: 'Most offers' },
    { value: 'company', label: 'Company name' },
  ];
  readonly filters = this.formBuilder.nonNullable.group({
    search: '',
    sort: this.formBuilder.nonNullable.control<CompanySort>('applications'),
    direction: this.formBuilder.nonNullable.control<'asc' | 'desc'>('desc'),
  });

  ngOnInit(): void {
    this.load();
  }

  applyFilters(): void {
    this.load();
  }

  clearSearch(): void {
    this.filters.controls.search.setValue('');
    this.load();
  }

  retry(): void {
    this.load();
  }

  private load(): void {
    if (this.loading() && this.summary()) {
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.api
      .getCompanies(this.filters.getRawValue())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (summary) => this.summary.set(summary),
        error: (error) =>
          this.error.set(apiErrorMessage(error, 'Unable to load company insights.')),
      });
  }
}
