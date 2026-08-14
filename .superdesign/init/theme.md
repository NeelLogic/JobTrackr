# JobTrackr Theme

## Compact token summary

- Font: locally bundled Plus Jakarta Sans, weights 400–700, with system sans-serif fallbacks.
- Primary: deep blue `#1e40af`; hover/emphasis `#1e3a8a`; dark theme primary `#60a5fa`.
- Accents: blue `#3b82f6`, orange `#f97316`, purple `#a855f7`, green `#10b981`, red `#ef4444`, and pink `#ec4899`.
- Light canvas/surfaces: `#f1f5f9`, `#f8fafc`, `#ffffff`.
- Light text: `#0f172a`, secondary `#334155`, muted `#64748b`.
- Dark canvas/surfaces: `#0b1120`, `#111a2a`, `#151f30`; dark text `#e7edf7`.
- Borders: `#e2e8f0`, strong `#cbd5e1`, soft `#f1f5f9`; dark equivalents `#2a3950`, `#3a4b63`, `#223047`.
- Card radius: `16px`; control radius: `12px`; icon radius: `12px`; pills fully rounded.
- Card shadow: `0 2px 10px -3px rgba(15,23,42,.12)` light; `0 12px 32px rgba(0,0,0,.22)` dark.
- Spacing: 8px base rhythm with 4, 12, 16, 24, 32, 48, and 64px steps.
- Controls: at least 44px high with visible keyboard focus and 150–180ms restrained transitions.

## Core CSS variables

Source: `Frontend/src/styles.scss`.

```scss
:root {
  --primary: #1e40af;
  --primary-dark: #1e3a8a;
  --ink: #0f172a;
  --ink-secondary: #334155;
  --ink-muted: #64748b;
  --surface: #ffffff;
  --surface-muted: #f1f5f9;
  --surface-subtle: #f8fafc;
  --surface-hover: #eff6ff;
  --border: #e2e8f0;
  --border-strong: #cbd5e1;
  --accent-blue: #3b82f6;
  --accent-orange: #f97316;
  --accent-purple: #a855f7;
  --accent-green: #10b981;
  --accent-red: #ef4444;
  --accent-pink: #ec4899;
  --radius-card: 16px;
  --radius-control: 12px;
  --radius-icon: 12px;
  --card-shadow: 0 2px 10px -3px rgba(15, 23, 42, 0.12);
  font-family: 'Plus Jakarta Sans', Inter, ui-sans-serif, system-ui, sans-serif;
}
```

Dark mode overrides the same semantic tokens under `:root[data-theme='dark']`; components never hard-code an alternate theme. Theme state is applied through `Frontend/src/app/core/theme.service.ts` and persisted under `jobtrackr-theme`.

Global primitives include `.button`, `.card`, `.status-badge`, `.page`, `.page-heading`, `.dashboard-header`, `.section-heading`, alerts, loading and empty states, form controls, tables, pagination, and accessibility utilities. There is no Tailwind configuration or third-party UI component library.
