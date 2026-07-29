import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in | JobTrackr',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login').then((module) => module.Login),
  },
  {
    path: 'register',
    title: 'Create account | JobTrackr',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register').then((module) => module.Register),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((module) => module.Shell),
    children: [
      {
        path: 'dashboard',
        title: 'Dashboard | JobTrackr',
        loadComponent: () =>
          import('./features/dashboard/dashboard').then((module) => module.Dashboard),
      },
      {
        path: 'applications',
        title: 'Applications | JobTrackr',
        loadComponent: () =>
          import('./features/applications/application-list').then(
            (module) => module.ApplicationList,
          ),
      },
      {
        path: 'applications/new',
        title: 'Add application | JobTrackr',
        loadComponent: () =>
          import('./features/applications/application-form').then(
            (module) => module.ApplicationForm,
          ),
      },
      {
        path: 'applications/:id/edit',
        title: 'Edit application | JobTrackr',
        loadComponent: () =>
          import('./features/applications/application-form').then(
            (module) => module.ApplicationForm,
          ),
      },
      {
        path: 'applications/:id',
        title: 'Application details | JobTrackr',
        loadComponent: () =>
          import('./features/applications/application-detail').then(
            (module) => module.ApplicationDetail,
          ),
      },
      {
        path: 'imports',
        title: 'Gmail import | JobTrackr',
        loadComponent: () =>
          import('./features/imports/gmail-import').then((module) => module.GmailImport),
      },
      {
        path: 'settings',
        title: 'Account settings | JobTrackr',
        loadComponent: () =>
          import('./features/settings/settings').then((module) => module.Settings),
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: '' },
];
