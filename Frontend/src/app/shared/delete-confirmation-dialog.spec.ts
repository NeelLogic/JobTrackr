import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { JobApplication } from '../models/application.models';
import { DeleteConfirmationDialog } from './delete-confirmation-dialog';

describe('DeleteConfirmationDialog', () => {
  const application: JobApplication = {
    id: 7,
    company: 'Acme',
    jobTitle: 'Software Developer',
    location: null,
    jobUrl: null,
    applicationDate: '2026-07-20',
    status: 'APPLIED',
    employmentType: 'FULL_TIME',
    salaryMin: null,
    salaryMax: null,
    salaryCurrency: null,
    notes: null,
    followUpDate: null,
    createdAt: '2026-07-20T12:00:00Z',
    updatedAt: '2026-07-21T12:00:00Z',
  };

  function setup() {
    TestBed.configureTestingModule({ imports: [DeleteConfirmationDialog] });
    const fixture = TestBed.createComponent(DeleteConfirmationDialog);
    fixture.componentRef.setInput('application', application);
    fixture.detectChanges();
    return { component: fixture.componentInstance, fixture };
  }

  it('identifies the application and emits confirmation', () => {
    const { component, fixture } = setup();
    const confirmed = vi.fn();
    component.confirmed.subscribe(confirmed);

    const dialog = fixture.nativeElement.querySelector('[role="alertdialog"]') as HTMLElement;
    const confirm = fixture.nativeElement.querySelector(
      '.delete-dialog__confirm',
    ) as HTMLButtonElement;
    confirm.click();

    expect(dialog.textContent).toContain('Software Developer');
    expect(dialog.textContent).toContain('Acme');
    expect(confirmed).toHaveBeenCalledOnce();
  });

  it('blocks cancellation while deletion is in progress', () => {
    const { component, fixture } = setup();
    const cancelled = vi.fn();
    component.cancelled.subscribe(cancelled);
    fixture.componentRef.setInput('deleting', true);
    fixture.detectChanges();

    component.onEscape();

    expect(cancelled).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Deleting...');
  });
});
