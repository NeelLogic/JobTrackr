# JobTrackr Shared UI Components

Framework: Angular 21 standalone components. Styling is custom SCSS; no third-party component library is used. Shared visual primitives rely on global classes in `Frontend/src/styles.scss`.

## ThemeToggle

- Source: `Frontend/src/app/shared/theme-toggle.ts`
- Template: `Frontend/src/app/shared/theme-toggle.html`
- Styles: `Frontend/src/app/shared/theme-toggle.scss`
- Purpose: accessible light/dark theme switch used in the app shell and authentication pages.
- Props: `compact: boolean`, `tone: 'surface' | 'sidebar'`

```ts
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ThemeService } from '../core/theme.service';

@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeToggle {
  private readonly themeService = inject(ThemeService);
  readonly compact = input(false);
  readonly tone = input<'surface' | 'sidebar'>('surface');
  readonly isDark = this.themeService.isDark;
  readonly nextThemeLabel = computed(() =>
    this.isDark() ? 'Switch to light mode' : 'Switch to dark mode',
  );
  readonly visibleLabel = computed(() => (this.isDark() ? 'Light mode' : 'Dark mode'));
  toggle(): void { this.themeService.toggle(); }
}
```

```html
<button class="theme-toggle" type="button" [class.theme-toggle--compact]="compact()"
  [attr.data-tone]="tone()" [attr.aria-label]="nextThemeLabel()"
  [attr.title]="nextThemeLabel()" [attr.aria-pressed]="isDark()" (click)="toggle()">
  <span class="theme-toggle__icon" aria-hidden="true">
    @if (isDark()) {
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="4"></circle><path d="M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.66 17.66l1.41 1.41"></path><path d="M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.66 6.34l1.41-1.41"></path></svg>
    } @else {
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M20.4 15.2A8.5 8.5 0 0 1 8.8 3.6 8.5 8.5 0 1 0 20.4 15.2Z"></path></svg>
    }
  </span>
  @if (!compact()) { <span>{{ visibleLabel() }}</span> }
</button>
```

```scss
:host { display: inline-flex; }
.theme-toggle { display:inline-flex; min-height:42px; align-items:center; justify-content:center; gap:10px; padding:9px 13px; border:1px solid var(--border-strong); border-radius:var(--radius-control); color:var(--ink-secondary); background:var(--surface); cursor:pointer; font:inherit; font-size:.78rem; font-weight:700; }
.theme-toggle:hover { border-color:var(--primary); background:var(--surface-hover); }
.theme-toggle--compact { width:42px; padding:0; }
.theme-toggle[data-tone='sidebar'] { width:100%; justify-content:flex-start; border-color:var(--border); color:var(--ink-secondary); background:var(--surface-subtle); }
.theme-toggle__icon, .theme-toggle__icon svg { display:block; width:20px; height:20px; }
```

## DeleteConfirmationDialog

- Source: `Frontend/src/app/shared/delete-confirmation-dialog.ts`
- Template: `Frontend/src/app/shared/delete-confirmation-dialog.html`
- Styles: `Frontend/src/app/shared/delete-confirmation-dialog.scss`
- Purpose: accessible destructive confirmation overlay used by Dashboard, Applications, Detail, and Form.
- Props: `application`, `deleting`, `error`; outputs `confirmed`, `cancelled`.

```ts
import { ChangeDetectionStrategy, Component, HostListener, input, output } from '@angular/core';
import { JobApplication } from '../models/application.models';
@Component({ selector:'app-delete-confirmation-dialog', templateUrl:'./delete-confirmation-dialog.html', styleUrl:'./delete-confirmation-dialog.scss', changeDetection:ChangeDetectionStrategy.OnPush })
export class DeleteConfirmationDialog {
  readonly application=input.required<JobApplication>(); readonly deleting=input(false); readonly error=input('');
  readonly confirmed=output<void>(); readonly cancelled=output<void>();
  @HostListener('document:keydown.escape') onEscape():void { this.cancel(); }
  cancel():void { if(!this.deleting()) this.cancelled.emit(); }
  confirm():void { if(!this.deleting()) this.confirmed.emit(); }
  backdropClick(event:MouseEvent):void { if(event.target===event.currentTarget) this.cancel(); }
}
```

```html
<div class="dialog-backdrop" (click)="backdropClick($event)">
  <section class="delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-dialog-title" aria-describedby="delete-dialog-description">
    <div class="delete-dialog__icon" aria-hidden="true">!</div>
    <div><p class="delete-dialog__eyebrow">Permanent action</p><h2 id="delete-dialog-title">Delete application?</h2>
      <p id="delete-dialog-description"><strong>{{ application().jobTitle }}</strong> at <strong>{{ application().company }}</strong> and its history will be permanently removed.</p></div>
    @if (error()) { <div class="alert alert--error" role="alert">{{ error() }}</div> }
    <div class="delete-dialog__actions"><button class="button button--secondary" type="button" [disabled]="deleting()" autofocus (click)="cancel()">Keep application</button><button class="button delete-dialog__confirm" type="button" [disabled]="deleting()" (click)="confirm()">{{ deleting() ? 'Deleting...' : 'Yes, delete application' }}</button></div>
  </section>
</div>
```

## GoogleSignInButton

- Source: `Frontend/src/app/shared/google-sign-in-button.ts`
- Template: `Frontend/src/app/shared/google-sign-in-button.html`
- Styles: `Frontend/src/app/shared/google-sign-in-button.scss`
- Purpose: Google Identity button wrapper with configured, loading, and error states.
- Props: `disabled`; output `credentialSelected`.

```ts
import { AfterViewInit, Component, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { GoogleIdentityService } from '../core/google-identity.service';
@Component({ selector:'app-google-sign-in-button', templateUrl:'./google-sign-in-button.html', styleUrl:'./google-sign-in-button.scss' })
export class GoogleSignInButton implements AfterViewInit {
  readonly disabled=input(false); readonly credentialSelected=output<string>(); readonly loading=signal(true); readonly configured=signal(true); readonly error=signal('');
  private readonly container=viewChild.required<ElementRef<HTMLElement>>('container');
  constructor(private readonly googleIdentity:GoogleIdentityService) {}
  ngAfterViewInit():void { void this.render(); } retry():void { void this.render(); }
  private async render():Promise<void> { this.loading.set(true); this.error.set(''); try { const configured=await this.googleIdentity.renderButton(this.container().nativeElement,(credential)=>this.credentialSelected.emit(credential)); this.configured.set(configured); } catch { this.error.set('Google sign-in is temporarily unavailable.'); } finally { this.loading.set(false); } }
}
```

```html
<div class="google-button" [class.google-button--disabled]="disabled()" [attr.aria-disabled]="disabled()"><div #container></div></div>
@if (loading()) { <p class="google-message" role="status">Loading Google sign-in...</p> }
@else if (error()) { <div class="google-message google-message--error" role="alert">{{ error() }} <button type="button" (click)="retry()">Try again</button></div> }
@else if (!configured()) { <p class="google-message">Google sign-in is not configured for this environment.</p> }
```

## Global visual primitives

Actual implementations are in `Frontend/src/styles.scss`: `.button`, `.card`, `.status-badge`, `.alert`, `.loading-state`, `.message-state`, `.page`, `.page-heading`, `.dashboard-header`, form fields, tables, pagination, and visually-hidden utilities. These are class primitives rather than Angular components and should be reused in designs.
