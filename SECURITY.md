# Security Policy

JobTrackr handles account credentials, job-search records, and optional Google
OAuth tokens. Security reports are taken seriously.

## Supported Versions

Until V1.0 is released, security fixes are applied to the latest `main` branch.
After V1.0, the latest V1 release will receive security fixes while it remains
the current supported release.

| Version                   | Supported   |
| ------------------------- | ----------- |
| Latest V1.x release       | Yes         |
| Older releases            | No          |
| Development `main` branch | Best effort |

## Reporting a Vulnerability

Do not disclose a suspected vulnerability in a public issue, discussion, pull
request, screenshot, or social-media post.

Use GitHub's **Security** tab and select **Report a vulnerability** if private
vulnerability reporting is available. If it is unavailable, open a minimal
public issue asking the maintainer to establish a private contact channel. Do
not include exploit details, credentials, personal information, or affected
user data in that issue.

Include privately:

- the affected version or commit;
- the affected endpoint or component;
- reproduction steps or a minimal proof of concept;
- the expected and observed result;
- potential impact;
- suggested remediation, if known;
- whether the issue has been disclosed elsewhere.

You should receive an acknowledgement within three business days. Triage timing
depends on severity and reproducibility. Please allow time for a fix and release
before public disclosure.

JobTrackr does not currently operate a paid bug-bounty program.

## In-Scope Security Areas

- authentication or JWT bypass;
- cross-user access to applications, analytics, Gmail connections, or imports;
- password, token, OAuth code, or encryption-key exposure;
- unsafe Google identity or OAuth verification;
- SQL injection or unsafe query construction;
- stored or reflected cross-site scripting;
- cross-site request or CORS misconfiguration;
- insecure direct object references;
- validation bypass with security impact;
- sensitive information in errors or logs;
- dependency vulnerabilities with an exploitable JobTrackr path;
- deployment configuration that exposes MySQL or production secrets.

## Out of Scope

- social engineering or phishing;
- denial-of-service testing against a public demo;
- automated scanning that materially degrades service;
- reports requiring access to another person's account without permission;
- vulnerabilities that exist only in unsupported dependencies with no
  exploitable JobTrackr path;
- missing features or general hardening suggestions without a security impact;
- Google OAuth restrictions caused solely by a self-hosted operator's incorrect
  Google Cloud configuration.

## Security Design Summary

- Passwords are hashed with BCrypt.
- Protected APIs require a signed, unexpired JobTrackr JWT.
- User-owned database queries are scoped to the authenticated user.
- Request DTOs use server-side validation.
- Centralized error handling omits stack traces and internal details.
- CORS origins are environment-configurable.
- Google identity credentials are verified server-side.
- Existing password accounts require authenticated linking to Google.
- Gmail uses read-only authorization and short-lived, single-use OAuth state.
- Gmail tokens are encrypted with AES-256-GCM before persistence.
- Gmail imports require explicit review and approval.
- Raw Gmail message bodies and raw message IDs are not stored.
- Flyway owns production schema changes.
- The Spring Boot runtime image uses a non-root Linux user.
- Local MySQL is reachable only on the private container network. The hosted
  Aiven MySQL endpoint requires TLS and credentials stored only in Render.
- Docker build contexts exclude local environments, logs, build output, and
  editor metadata.
- Frontend build configuration accepts only a public HTTP(S) or root-relative
  API URL.
- GitHub Actions builds, tests, and validates both production images on pull
  requests.

See [Docs/ARCHITECTURE.md](Docs/ARCHITECTURE.md) for the complete trust
boundaries and data flows.

## Secret Handling

Never commit:

- database passwords;
- JWT secrets;
- Google OAuth client secrets;
- Gmail access or refresh tokens;
- Gmail token-encryption keys;
- production `.env` files;
- real user or application data.

Use environment variables or the hosting provider's secret store. The
repository ignores `.env` and environment-specific property files. A public
OAuth client ID may be delivered to a browser, but it must still be
environment-specific and must not be confused with a client secret.

If a secret is exposed:

1. Revoke or rotate it immediately.
2. Remove it from the active deployment.
3. Invalidate affected sessions or OAuth connections.
4. Review logs for misuse.
5. Remove it from Git history when necessary; deleting only the latest file is
   insufficient.
6. Document the incident and prevention measures privately.

## Deployment Expectations

Production and public-demo deployments must:

- use HTTPS;
- use a strong production-only JWT secret;
- use provider-issued database credentials and a least-privilege application
  account when the provider supports one;
- keep local MySQL on a private network and require TLS for hosted Aiven MySQL;
- restrict CORS to the deployed frontend;
- keep error details and stack traces disabled;
- store secrets only in deployment environment settings;
- run health checks, backups, and restore tests;
- configure only the public Google Sign-In client ID in the public demo;
- omit Gmail client secrets and token-encryption keys until the hosted
  integration is verified.

See [Docs/DEPLOYMENT.md](Docs/DEPLOYMENT.md) for the release checklist.

## Safe Testing

Use accounts and data you own. Do not access, modify, or delete another user's
data. Stop testing and report privately if you unexpectedly encounter real user
information.
