import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { JobApplication } from '../models/application.models';

@Component({
  selector: 'app-delete-confirmation-dialog',
  templateUrl: './delete-confirmation-dialog.html',
  styleUrl: './delete-confirmation-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeleteConfirmationDialog {
  readonly application = input.required<JobApplication>();
  readonly deleting = input(false);
  readonly error = input('');
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.cancel();
  }

  cancel(): void {
    if (!this.deleting()) {
      this.cancelled.emit();
    }
  }

  confirm(): void {
    if (!this.deleting()) {
      this.confirmed.emit();
    }
  }

  backdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancel();
    }
  }
}
