# JobTrackr Page Dependency Trees

All authenticated pages also render within `layout/shell.ts` → `shell.html` + `shell.scss` → `shared/theme-toggle.*`.

## `/dashboard`
- `Frontend/src/app/features/dashboard/dashboard.ts`
  - `dashboard.html`
  - `dashboard.scss`
  - `core/api/dashboard-api.service.ts`
  - `core/api/application-api.service.ts`
  - `core/auth.service.ts`
  - `core/api-error.ts`
  - `models/dashboard.models.ts`
    - `models/application.models.ts`
  - `shared/delete-confirmation-dialog.ts`
    - `delete-confirmation-dialog.html`
    - `delete-confirmation-dialog.scss`

## `/applications`
- `Frontend/src/app/features/applications/application-list.ts`
  - `application-list.html`
  - `application-list.scss`
  - `core/api/application-api.service.ts`
  - `core/api-error.ts`
  - `models/application.models.ts`
  - `shared/delete-confirmation-dialog.ts`
    - `delete-confirmation-dialog.html`
    - `delete-confirmation-dialog.scss`

## `/applications/new` and `/applications/:id/edit`
- `Frontend/src/app/features/applications/application-form.ts`
  - `application-form.html`
  - `application-form.scss`
  - `core/api/application-api.service.ts`
  - `core/api-error.ts`
  - `models/application.models.ts`
  - `shared/date.validators.ts`
  - `shared/delete-confirmation-dialog.ts`
    - `delete-confirmation-dialog.html`
    - `delete-confirmation-dialog.scss`

## `/applications/:id`
- `Frontend/src/app/features/applications/application-detail.ts`
  - `application-detail.html`
  - `application-detail.scss`
  - `core/api/application-api.service.ts`
  - `core/api-error.ts`
  - `models/application.models.ts`
  - `shared/delete-confirmation-dialog.ts`

## `/analytics`
- `Frontend/src/app/features/analytics/analytics.ts`
  - `analytics.html`
  - `analytics.scss`
  - `core/api/insights-api.service.ts`
  - `core/api-error.ts`
  - `models/insights.models.ts`
    - `models/application.models.ts`

## `/companies`
- `Frontend/src/app/features/companies/companies.ts`
  - `companies.html`
  - `companies.scss`
  - `core/api/insights-api.service.ts`
  - `core/api-error.ts`
  - `models/insights.models.ts`

## `/follow-ups`
- `Frontend/src/app/features/follow-ups/follow-ups.ts`
  - `follow-ups.html`
  - `follow-ups.scss`
  - `core/api/insights-api.service.ts`
  - `core/api-error.ts`
  - `models/insights.models.ts`
  - `models/application.models.ts`

## `/imports`
- `Frontend/src/app/features/imports/gmail-import.ts`
  - `gmail-import.html`
  - `gmail-import.scss`
  - `core/api/gmail-integration.service.ts`
  - `core/integration-links.ts`
  - `core/api-error.ts`
  - `models/integration.models.ts`
  - `models/application.models.ts`

## `/settings`
- `Frontend/src/app/features/settings/settings.ts`
  - `settings.html`
  - `settings.scss`
  - `core/auth.service.ts`
  - `core/api/gmail-integration.service.ts`
  - `core/integration-links.ts`
  - `core/api-error.ts`
  - `models/auth.models.ts`
  - `models/integration.models.ts`
  - `shared/google-sign-in-button.ts`
    - `google-sign-in-button.html`
    - `google-sign-in-button.scss`

## `/login` and `/register`
- `Frontend/src/app/features/auth/login.ts` or `register.ts`
  - corresponding `.html` and `.scss`
  - `core/auth.service.ts`
  - `core/google-identity.service.ts`
  - `core/api-error.ts`
  - `models/auth.models.ts`
  - `shared/google-sign-in-button.*`
  - `shared/theme-toggle.*`

Every page additionally depends on global visual primitives and tokens in `Frontend/src/styles.scss`.
