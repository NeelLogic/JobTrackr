# Contributing to JobTrackr

Thank you for considering a contribution to JobTrackr. Contributions should
preserve the project's security, user-data isolation, focused technology stack,
and professional Git history.

## Before Starting

1. Review [README.md](README.md).
2. Review [Docs/ARCHITECTURE.md](Docs/ARCHITECTURE.md).
3. Check existing issues and pull requests to avoid duplicate work.
4. For a substantial change, open an issue describing the use case and proposed
   approach before implementation.
5. Report security vulnerabilities through the process in
   [SECURITY.md](SECURITY.md), not through a public issue.

## Local Setup

Prerequisites:

- Java 21 or newer
- Node.js 22 and npm 10
- MySQL 8
- Git

Run the backend:

```powershell
cd Backend\jobtrackr

$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-local-password"
$env:JWT_SECRET = "a-local-secret-containing-at-least-32-characters"

.\mvnw.cmd spring-boot:run
```

Run the frontend in another terminal:

```powershell
cd Frontend
npm ci
npm start
```

Google integrations are optional. Follow
[Docs/GOOGLE_INTEGRATION.md](Docs/GOOGLE_INTEGRATION.md) only when the change
requires them.

## Branching

Create a focused branch from an up-to-date `main` branch:

```powershell
git switch main
git pull --ff-only origin main
git switch -c feature/short-description
```

Use an appropriate prefix:

- `feature/` for new user-facing capabilities;
- `fix/` for defects;
- `test/` for test-only changes;
- `docs/` for documentation;
- `ci/` for build and automation changes;
- `refactor/` for behavior-preserving structural work.

Do not commit directly to `main`.

## Commit Messages

Use Conventional Commit-style messages:

```text
feat: add application status filtering
fix: prevent cross-user application access
test: cover Gmail candidate ownership
docs: document Render deployment
ci: validate Docker image builds
```

Each commit should represent one logical, working change. Do not create empty or
artificial commits to increase the commit count, and do not rewrite shared
history unnecessarily.

## Engineering Standards

### Backend

- Preserve the controller-service-repository architecture.
- Use request and response DTOs; do not expose JPA entities.
- Validate untrusted input at the API boundary.
- Put business and ownership rules in services.
- Scope every user-owned query to the authenticated user.
- Add a new Flyway migration for schema changes; never edit an applied
  migration.
- Use centralized exception handling and safe client messages.
- Do not log passwords, JWTs, OAuth codes, Gmail tokens, or email bodies.

### Frontend

- Keep API calls in core API services rather than components.
- Use route guards for protected and guest-only pages.
- Represent loading, empty, success, and error states.
- Provide accessible labels, keyboard behavior, and validation feedback.
- Keep request and response models aligned with backend DTOs.
- Do not place OAuth client secrets or encryption keys in Angular code.

### Dependencies

Add a dependency only when it has a clear maintenance or product benefit.
Prefer the existing stack over introducing a second solution for a problem that
is already handled.

## Testing

Run the backend verification:

```powershell
cd Backend\jobtrackr
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Run the frontend checks:

```powershell
cd Frontend
npm ci
npx prettier --check "src/**/*.{ts,html,scss}" "angular.json" "package.json" "tsconfig*.json"
npm test -- --watch=false
npm run build
```

Add meaningful tests for changed behavior. Security-sensitive changes should
include authentication, authorization, validation, failure, and cross-user
isolation cases where applicable.

## Pull Requests

Keep pull requests focused and small enough to review. Include:

- the user or engineering problem;
- a concise summary of the solution;
- security and data-migration implications;
- tests executed and their results;
- screenshots or a short recording for visible UI changes;
- follow-up work that is intentionally out of scope.

Before requesting review:

- [ ] The branch is current with `main`
- [ ] No unrelated or generated files are included
- [ ] No credentials or personal data are present
- [ ] Backend tests and build pass
- [ ] Frontend formatting, tests, and build pass
- [ ] New behavior is documented
- [ ] GitHub Actions checks pass

Do not merge a failing pull request.

## Documentation

Update documentation in the same pull request when behavior, setup, architecture,
environment variables, or deployment steps change. Documentation must describe
the application as it actually behaves; do not mark planned features as
complete.

## License

By contributing, you agree that your contribution will be licensed under the
[MIT License](LICENSE).
