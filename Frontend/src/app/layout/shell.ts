import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DashboardApiService } from '../core/api/dashboard-api.service';
import { GmailIntegrationService } from '../core/api/gmail-integration.service';
import { AuthService } from '../core/auth.service';
import { GOOGLE_INTEGRATION_GUIDE_URL } from '../core/integration-links';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly dashboardApi = inject(DashboardApiService);
  private readonly gmailIntegration = inject(GmailIntegrationService);

  readonly menuOpen = signal(false);
  readonly gmailInfoOpen = signal(false);
  readonly gmailConfigured = signal<boolean | null>(null);
  readonly overdueFollowUps = signal(0);
  readonly user = this.auth.user;
  readonly gmailSetupGuideUrl = GOOGLE_INTEGRATION_GUIDE_URL;

  ngOnInit(): void {
    this.dashboardApi.getSummary().subscribe({
      next: (summary) => this.overdueFollowUps.set(summary.overdueFollowUps),
      error: () => this.overdueFollowUps.set(0),
    });
    this.gmailIntegration.status().subscribe({
      next: (status) => this.gmailConfigured.set(status.configured),
      error: () => this.gmailConfigured.set(null),
    });
  }

  openMenu(): void {
    this.menuOpen.set(true);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  openGmailInfo(): void {
    this.closeMenu();
    this.gmailInfoOpen.set(true);
  }

  closeGmailInfo(): void {
    this.gmailInfoOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  closeOverlaysOnEscape(): void {
    this.closeMenu();
    this.closeGmailInfo();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
