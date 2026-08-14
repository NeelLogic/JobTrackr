# Extractable JobTrackr Components

## AppSidebar
- Source: `Frontend/src/app/layout/shell.html` + `shell.scss`
- Category: layout
- Description: persistent white grouped navigation with JobTrackr brand, add-application CTA, feature badge, follow-up count, theme control, signed-in profile, and matching off-canvas behavior.
- Extractable props: `activeItem`, `overdueFollowUps`, `gmailConfigured`, `userName`, `userEmail`, `menuOpen`
- Hardcoded: route labels, section labels, JobTrackr wordmark, icon meanings, layout styles.

## MobileNavigation
- Source: `Frontend/src/app/layout/shell.html` + `shell.scss`
- Category: layout
- Description: compact brand header plus fixed bottom navigation for Dashboard, Applications, Add, Analytics, and More.
- Extractable props: `menuOpen`, `activeItem`, `overdueFollowUps`
- Hardcoded: brand, route labels, inline SVG icons, safe-area layout, and elevated Add action.

## WorkspaceFooter
- Source: `Frontend/src/app/layout/shell.html` + `shell.scss`
- Category: layout
- Description: global copyright footer below routed content.
- Extractable props: none
- Hardcoded: copyright content and styling.

## ThemeToggle
- Source: `Frontend/src/app/shared/theme-toggle.ts`
- Category: basic
- Description: accessible light/dark switch with surface and neutral sidebar variants.
- Extractable props: `compact`, `tone`, `isDark`
- Hardcoded: sun/moon SVG paths, labels, styles.

## DeleteConfirmationDialog
- Source: `Frontend/src/app/shared/delete-confirmation-dialog.ts`
- Category: basic
- Description: alert dialog for permanent application deletion.
- Extractable props: `jobTitle`, `company`, `deleting`, `error`
- Hardcoded: warning icon, permanent-action language, button treatments.

## PageHeading
- Source: global class in `Frontend/src/styles.scss`, used by all authenticated feature templates.
- Category: basic
- Description: eyebrow, page title, descriptive copy, and right-aligned primary action.
- Extractable props: `eyebrow`, `title`, `description`, `actionLabel`, `actionHref`
- Hardcoded: responsive structure and typography.

## StatCard
- Source: `Frontend/src/app/features/dashboard/dashboard.html` + `dashboard.scss`
- Category: basic
- Description: metric card with tone accent, value, label, and supporting description.
- Extractable props: `label`, `value`, `description`, `tone`
- Hardcoded: card geometry and accent treatment.

## ApplicationStatusBadge
- Source: global `.status-badge` variants in `Frontend/src/styles.scss`.
- Category: basic
- Description: consistent compact application-stage label.
- Extractable props: `status`, `label`
- Hardcoded: status palette and pill geometry.

## CompanyMark
- Source: global `.company-mark` in `Frontend/src/styles.scss`.
- Category: basic
- Description: fallback company avatar using the company initial.
- Extractable props: `initial`
- Hardcoded: geometry, blue surface, typography.
