import {
  AfterViewInit,
  Component,
  ElementRef,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { GoogleIdentityService } from '../core/google-identity.service';

@Component({
  selector: 'app-google-sign-in-button',
  templateUrl: './google-sign-in-button.html',
  styleUrl: './google-sign-in-button.scss',
})
export class GoogleSignInButton implements AfterViewInit {
  readonly disabled = input(false);
  readonly credentialSelected = output<string>();
  readonly loading = signal(true);
  readonly configured = signal(true);
  readonly error = signal('');
  private readonly container = viewChild.required<ElementRef<HTMLElement>>('container');

  constructor(private readonly googleIdentity: GoogleIdentityService) {}

  ngAfterViewInit(): void {
    void this.render();
  }

  retry(): void {
    void this.render();
  }

  private async render(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const configured = await this.googleIdentity.renderButton(
        this.container().nativeElement,
        (credential) => this.credentialSelected.emit(credential),
      );
      this.configured.set(configured);
    } catch {
      this.error.set('Google sign-in is temporarily unavailable.');
    } finally {
      this.loading.set(false);
    }
  }
}
