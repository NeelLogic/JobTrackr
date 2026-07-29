import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DashboardApiService } from '../core/api/dashboard-api.service';
import { AuthService } from '../core/auth.service';

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

  readonly menuOpen = signal(false);
  readonly overdueFollowUps = signal(0);
  readonly user = this.auth.user;

  ngOnInit(): void {
    this.dashboardApi.getSummary().subscribe({
      next: (summary) => this.overdueFollowUps.set(summary.overdueFollowUps),
      error: () => this.overdueFollowUps.set(0),
    });
  }

  openMenu(): void {
    this.menuOpen.set(true);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  closeMenuOnEscape(): void {
    this.closeMenu();
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
