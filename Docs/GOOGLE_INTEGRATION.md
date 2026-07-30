# Optional Google Integrations

JobTrackr supports two separate Google capabilities:

1. **Google Sign-In** verifies a Google identity and exchanges it for a
   JobTrackr JWT.
2. **Gmail application import** obtains read-only Gmail authorization, scans a
   bounded recent window on demand, and creates reviewable application
   suggestions.

Both are optional. Password authentication and manual application tracking work
without Google configuration.

## V1 Availability

The public V1 Render deployment uses a dedicated production Google Cloud
project for basic Google Sign-In. Visitors can sign in with their own Google
account without supplying credentials. The deployment stores only the public
Google client ID in the backend environment; it does not use a browser client
secret.

Gmail import remains a self-hosted V1 feature. The hosted interface labels it
as **Self-hosted**, explains why it is unavailable, and links to this guide.
Visitors must never enter an OAuth client secret or token-encryption key into
the hosted browser application.

Developers who clone or fork JobTrackr can enable Gmail import using their own
Google Cloud testing project and backend environment variables. Password
authentication and manual application tracking continue to work when either
Google capability is disabled.

## Credential Model

Google terminology is important:

- An **OAuth client ID** identifies the application. A browser client ID is
  public configuration, not a password.
- An **OAuth client secret** authenticates a server-side OAuth client. It must
  remain on the backend.
- An **API key** does not replace OAuth consent and cannot authorize access to a
  user's Gmail account.
- Google issues user-specific credentials only after the user grants the
  requested permission.

Do not implement a form that accepts other users' client secrets in the hosted
frontend. A developer who wants to use their own credentials should self-host
the application and configure its environment.

## Recommended Google Cloud Layout

For local V1 development, one testing project can contain separate web clients
for Google Sign-In and the Gmail server flow.

For the public V1 deployment, use separate projects:

- a production project requesting only basic identity scopes for public Google
  Sign-In;
- a separate testing project for self-hosted Gmail import while
  restricted-scope verification is incomplete.

Publishing status and consent-screen scopes apply at the Google Cloud project
level, not merely to one client ID. Separating projects prevents a restricted
Gmail scope from complicating basic public sign-in.

## Prerequisites

- A Google account
- A Google Cloud project
- An external OAuth audience
- Your account added as a test user while the project is in Testing
- The Gmail API enabled if Gmail import is required

## Configure Google Sign-In

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a project.
3. Open **Google Auth Platform**.
4. Configure branding and select an **External** audience.
5. Keep the application in **Testing** for local development.
6. Add the Google accounts that will test the application.
7. Open **Clients** and create an OAuth client.
8. Select **Web application**.
9. Add these authorized JavaScript origins:

   ```text
   http://localhost
   http://localhost:4200
   ```

10. Copy the generated client ID.
11. Set it before starting the backend:

    ```powershell
    $env:GOOGLE_CLIENT_ID = "your-client-id.apps.googleusercontent.com"
    ```

12. Restart the backend and frontend.

Google Sign-In uses the Google Identity Services callback flow. It does not
require a client secret or redirect URI in the V1 implementation.

For the hosted Render frontend, add its exact HTTPS origin to the production
client's **Authorized JavaScript origins** and set `GOOGLE_CLIENT_ID` in the
backend Render service. Do not configure Gmail scopes in this production
sign-in project.

The backend publishes only this safe configuration:

```http
GET /api/auth/google/config
```

When `GOOGLE_CLIENT_ID` is blank, the response marks Google Sign-In as disabled.

## Configure Gmail Import

### 1. Enable the API and scope

In the same testing project or a separate Gmail testing project:

1. Enable the **Gmail API**.
2. Open **Google Auth Platform > Data access**.
3. Add:

   ```text
   https://www.googleapis.com/auth/gmail.readonly
   ```

4. Confirm your Google account is listed as a test user.

`gmail.readonly` is a restricted scope. Keep this integration limited to test
users for V1 unless the application completes Google's applicable verification
and security requirements.

### 2. Create the server OAuth client

Create another OAuth client with application type **Web application**.

Add this exact authorized redirect URI:

```text
http://localhost:8080/api/integrations/gmail/callback
```

Copy the client ID and client secret. The secret belongs only in the Spring Boot
environment.

### 3. Generate the encryption key

JobTrackr encrypts stored Gmail access and refresh tokens with AES-256-GCM.
Generate exactly 32 random bytes and Base64-encode them:

```powershell
$gmailKeyBytes = New-Object byte[] 32
$gmailRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$gmailRng.GetBytes($gmailKeyBytes)
$gmailRng.Dispose()
$gmailKey = [Convert]::ToBase64String($gmailKeyBytes)
$gmailKey
```

Validate the result:

```powershell
([Convert]::FromBase64String($gmailKey)).Length
```

The result must be:

```text
32
```

Keep the same key for an existing environment. Replacing it makes previously
stored Gmail tokens unreadable and requires affected users to reconnect.

### 4. Set local variables

```powershell
$env:GOOGLE_GMAIL_CLIENT_ID = "your-gmail-client-id.apps.googleusercontent.com"
$env:GOOGLE_GMAIL_CLIENT_SECRET = "your-client-secret"
$env:GMAIL_TOKEN_ENCRYPTION_KEY = $gmailKey
$env:GOOGLE_GMAIL_REDIRECT_URI = "http://localhost:8080/api/integrations/gmail/callback"
$env:GMAIL_FRONTEND_CALLBACK_URL = "http://localhost:4200/settings"
```

The following optional limits have safe defaults:

```powershell
$env:GMAIL_OAUTH_STATE_TTL = "10m"
$env:GMAIL_IMPORT_LOOKBACK_DAYS = "180"
$env:GMAIL_IMPORT_MAX_MESSAGES = "100"
```

Restart Spring Boot after changing environment variables.

### 5. Connect and scan

1. Sign in to JobTrackr.
2. Open **Settings**.
3. Select **Connect Gmail**.
4. Approve the read-only permission using a configured test account.
5. Return to **Gmail import**.
6. Select **Scan Gmail**.
7. Review, correct, and explicitly import or dismiss each suggestion.

JobTrackr never receives the Google password. It does not use Workday
credentials or a private Workday API.

## Security Properties

- Google ID credentials are verified server-side.
- Gmail OAuth uses a short-lived, random, single-use state.
- Only a hash of the OAuth state is stored.
- Gmail tokens are encrypted with AES-256-GCM and bound to the owning user.
- The connected Gmail address must match the authenticated JobTrackr email.
- Gmail is accessed only after a user action and only with read-only permission.
- Scans are bounded by message count and age.
- Raw Gmail message bodies and raw Gmail message IDs are not stored.
- Import candidates and fingerprints are scoped to the owning user.
- No application is created until the user approves the suggestion.
- Disconnecting removes the stored credentials and attempts token revocation.

## Disable Google Services

Remove the Google environment variables and restart the backend.

Password authentication and manual application tracking remain available.
Without `GOOGLE_CLIENT_ID`, the login and registration screens omit Google
Sign-In. Without the Gmail variables, the sidebar, import screen, and Settings
show the self-hosted Gmail state and link to this guide instead of starting a
broken OAuth flow.

## Troubleshooting

### `Error 401: invalid_client`

- Confirm the full client ID was copied without surrounding quotes or spaces.
- Confirm the ID belongs to a **Web application** client.
- Confirm `GOOGLE_CLIENT_ID` and `GOOGLE_GMAIL_CLIENT_ID` were not accidentally
  swapped.
- Restart the backend after changing variables.

### `redirect_uri_mismatch`

The Google Cloud redirect URI must exactly match
`GOOGLE_GMAIL_REDIRECT_URI`, including protocol, hostname, port, path, and
trailing slash behavior.

### Only one Google account can authorize

While the OAuth project is in Testing, add every intended local tester under
**Audience > Test users**.

### Gmail integration is not configured

Confirm all three values exist in the same backend process:

```powershell
[bool]$env:GOOGLE_GMAIL_CLIENT_ID
[bool]$env:GOOGLE_GMAIL_CLIENT_SECRET
[bool]$env:GMAIL_TOKEN_ENCRYPTION_KEY
```

Validate the encryption key:

```powershell
([Convert]::FromBase64String($env:GMAIL_TOKEN_ENCRYPTION_KEY.Trim())).Length
```

The result must be `32`.

### Gmail access stops during testing

Google OAuth projects in Testing can issue refresh tokens with limited
lifetimes. Reconnect the test account and confirm the application's testing
audience and scopes.

## Production Considerations

Before offering Gmail import to arbitrary public users:

- host the application on verified HTTPS domains;
- publish accurate terms of service and privacy policy pages;
- configure the production redirect URIs;
- request only the minimum scopes;
- complete Google's brand and restricted-scope verification as applicable;
- complete any required security assessment when restricted-scope data is
  transmitted to or stored on a server;
- document retention, deletion, revocation, and incident-response behavior.

Official references:

- [Google OAuth app verification](https://support.google.com/cloud/answer/13463073)
- [Google OAuth verification requirements](https://support.google.com/cloud/answer/13464321)
- [Gmail API scopes](https://developers.google.com/workspace/gmail/api/auth/scopes)
- [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy)
