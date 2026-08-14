# JobTrackr Layouts

## Authenticated application shell

- Component: `Frontend/src/app/layout/shell.ts`
- Template: `Frontend/src/app/layout/shell.html`
- Styles: `Frontend/src/app/layout/shell.scss`
- Description: persistent white navigation sidebar on desktop, compact top bar and fixed five-item bottom navigation on mobile, and routed page content through `router-outlet`.

### Desktop

- Fixed `248px` sidebar with the JobTrackr wordmark, primary Add application action, grouped navigation, Gmail state, theme control, and signed-in profile.
- Active navigation uses a pale blue surface and deep-blue text while keeping the sidebar neutral in both light and dark themes.
- Routed content occupies the remaining width; `.page` caps content at `1440px` and uses responsive horizontal padding.
- The shared footer stays below routed content.

### Mobile and tablet

- Below `980px`, the sidebar becomes an off-canvas More drawer with a backdrop.
- A compact top header keeps the brand, theme control, and profile access visible.
- A fixed bottom navigation exposes Dashboard, Applications, Add, Analytics, and More. Add is elevated and visually emphasized.
- Content receives bottom safe-area spacing so the navigation never covers controls.

### Shared behavior

- Skip-link, keyboard focus, Escape-to-close, drawer backdrop, overdue follow-up badge, Gmail availability dialog, theme switching, and sign-out behavior are preserved.
- Navigation icons are inline SVGs and carry accessible text labels.
- Source files remain authoritative for exact markup and responsive rules.

## Guest authentication layout

Login and Register are self-contained split auth pages, not wrapped by `Shell`.

- Left: deep navy-blue brand panel with JobTrackr positioning copy.
- Right: neutral canvas with a bordered, elevated form card, theme toggle, Google sign-in state, credential fields, and reciprocal auth link.
- Below `760px`, the layout stacks into one column; the form card remains full-width without horizontal overflow.

Source files: `Frontend/src/app/features/auth/login.html`, `register.html`, and global auth rules in `Frontend/src/styles.scss`.
