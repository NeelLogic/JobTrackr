# JobTrackr Routes

Router source: `Frontend/src/app/app.routes.ts`. Angular 21 standalone lazy-loaded components with `authGuard` and `guestGuard`.

| URL | Page component | Layout | Access |
|---|---|---|---|
| `/login` | `features/auth/login.ts` | Standalone auth layout | Guest |
| `/register` | `features/auth/register.ts` | Standalone auth layout | Guest |
| `/verify-email` | `features/auth/verify-email.ts` | Standalone auth layout | Guest |
| `/forgot-password` | `features/auth/forgot-password.ts` | Standalone auth layout | Guest |
| `/dashboard` | `features/dashboard/dashboard.ts` | `layout/shell.ts` | Authenticated |
| `/applications` | `features/applications/application-list.ts` | Shell | Authenticated |
| `/applications/new` | `features/applications/application-form.ts` | Shell | Authenticated |
| `/applications/:id/edit` | `features/applications/application-form.ts` | Shell | Authenticated |
| `/applications/:id` | `features/applications/application-detail.ts` | Shell | Authenticated |
| `/analytics` | `features/analytics/analytics.ts` | Shell | Authenticated |
| `/companies` | `features/companies/companies.ts` | Shell | Authenticated |
| `/follow-ups` | `features/follow-ups/follow-ups.ts` | Shell | Authenticated |
| `/imports` | `features/imports/gmail-import.ts` | Shell | Authenticated/config-gated |
| `/settings` | `features/settings/settings.ts` | Shell | Authenticated |

Default authenticated redirect is `/dashboard`; wildcard redirects to the app root.

```ts
export const routes: Routes = [
  { path:'login', title:'Sign in | JobTrackr', canActivate:[guestGuard], loadComponent:()=>import('./features/auth/login').then(m=>m.Login) },
  { path:'register', title:'Create account | JobTrackr', canActivate:[guestGuard], loadComponent:()=>import('./features/auth/register').then(m=>m.Register) },
  { path:'verify-email', title:'Verify email | JobTrackr', canActivate:[guestGuard], loadComponent:()=>import('./features/auth/verify-email').then(m=>m.VerifyEmail) },
  { path:'forgot-password', title:'Reset password | JobTrackr', canActivate:[guestGuard], loadComponent:()=>import('./features/auth/forgot-password').then(m=>m.ForgotPassword) },
  { path:'', canActivate:[authGuard], loadComponent:()=>import('./layout/shell').then(m=>m.Shell), children:[
    { path:'dashboard', loadComponent:()=>import('./features/dashboard/dashboard').then(m=>m.Dashboard) },
    { path:'applications', loadComponent:()=>import('./features/applications/application-list').then(m=>m.ApplicationList) },
    { path:'analytics', loadComponent:()=>import('./features/analytics/analytics').then(m=>m.Analytics) },
    { path:'companies', loadComponent:()=>import('./features/companies/companies').then(m=>m.Companies) },
    { path:'follow-ups', loadComponent:()=>import('./features/follow-ups/follow-ups').then(m=>m.FollowUps) },
    { path:'applications/new', loadComponent:()=>import('./features/applications/application-form').then(m=>m.ApplicationForm) },
    { path:'applications/:id/edit', loadComponent:()=>import('./features/applications/application-form').then(m=>m.ApplicationForm) },
    { path:'applications/:id', loadComponent:()=>import('./features/applications/application-detail').then(m=>m.ApplicationDetail) },
    { path:'imports', loadComponent:()=>import('./features/imports/gmail-import').then(m=>m.GmailImport) },
    { path:'settings', loadComponent:()=>import('./features/settings/settings').then(m=>m.Settings) },
    { path:'', pathMatch:'full', redirectTo:'dashboard' },
  ]},
  { path:'**', redirectTo:'' },
];
```
