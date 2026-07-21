import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login').then(m => m.Login) },
  { path: 'register', loadComponent: () => import('./features/auth/register').then(m => m.Register) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'applications', loadComponent: () => import('./features/applications/application-list').then(m => m.ApplicationList) },
      { path: 'applications/new', loadComponent: () => import('./features/applications/application-form').then(m => m.ApplicationForm) },
      { path: 'applications/:id/edit', loadComponent: () => import('./features/applications/application-form').then(m => m.ApplicationForm) },
      { path: 'applications/:id', loadComponent: () => import('./features/applications/application-detail').then(m => m.ApplicationDetail) },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: '' }
];
