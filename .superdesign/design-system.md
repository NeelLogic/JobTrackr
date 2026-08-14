# JobTrackr Design System

Source: Superdesign project `JobTrackr Dashboard`, draft `JobTrackr Design System` (`ebc8da61-8bb8-430b-8b4d-a346d9b35cf3`). Adapted to the existing Angular application and its real user-facing workflows.

## Product context

JobTrackr is a focused career workspace for students, new graduates, and job seekers. It manages applications, status progression, deadlines, follow-ups, analytics, company research, and optional Gmail imports. The interface should reduce cognitive load while making the next useful action obvious.

## Visual foundation

- Primary typeface: Plus Jakarta Sans, weights 400–700. Bundle locally; retain system sans-serif fallbacks.
- Base spacing: 8px. Prefer 8, 16, 24, 32, 48, and 64px for layout rhythm, with 4 and 12px allowed inside dense controls.
- Primary brand: deep blue `#1e40af`; hover/emphasis `#1e3a8a`; bright accent blue `#3b82f6`.
- Accents: orange `#f97316`, purple `#a855f7`, green `#10b981`, red `#ef4444`, pink `#ec4899`.
- Light canvas: `#f1f5f9`; surfaces `#ffffff`; borders `#e2e8f0`; primary text `#0f172a`; muted text `#64748b`.
- Dark canvas: `#0b1120`; surfaces `#151f30`; borders `#2a3950`; primary text `#e7edf7`.
- Cards: 16px radius, 1px neutral border, subtle `0 2px 10px -3px rgba(15,23,42,.12)` shadow.
- Controls: 12px radius, minimum 44px touch target, clearly visible keyboard focus.
- Motion: restrained 150–180ms transitions; no looping decorative animation; honor reduced motion.

## Status semantics

- Saved: neutral slate.
- Applied: blue.
- Assessment: orange.
- Interview: purple.
- Offer: green.
- Rejected: red.
- Withdrawn: slate.

Never use color as the only status signal; pair it with a text label and accessible name.

## Layout

- Desktop (1024px+): fixed 248px light sidebar, grouped navigation, wide fluid workspace up to 1440px.
- Tablet: off-canvas sidebar, two- or three-column metric layouts.
- Mobile (820px and below): compact brand header, stacked content, two-column metric cards, fixed bottom navigation with elevated Add action.
- Dashboard priority: greeting → key metrics → attention items → status distribution and recent applications.

## Content and behavior rules

- Use real backend values and routes. Do not reproduce sample recruiter metrics, AI scores, subscriptions, candidate management, or account-balance content from reference mockups.
- Preserve loading, error, empty, destructive confirmation, dark-theme, keyboard, and responsive states.
- Use the existing JobTrackr mark from `Frontend/public/jobtrackr-mark.svg` and inline product icons; do not rely on third-party logo services.
- Keep one high-emphasis action per region. Secondary actions should use bordered or text treatments.
