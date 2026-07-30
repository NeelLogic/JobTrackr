# JobTrackr Frontend

The JobTrackr frontend is an Angular single-page application. It communicates
with the Spring Boot API, attaches JobTrackr JWTs through an HTTP interceptor,
and protects authenticated routes with Angular guards.

## Development

From this directory:

```powershell
npm ci
npm start
```

Open `http://localhost:4200`. The development proxy forwards `/api` to
`http://localhost:8080`.

To start both the frontend and backend using the repository-root `.env` file:

```powershell
npm run dev
```

## API configuration

The production build generates `public/config.js` from
`JOBTRACKR_API_URL`. The value must be either a root-relative path such as
`/api` or an absolute HTTP(S) URL such as
`https://jobtrackr-api.example.com/api`.

```powershell
$env:JOBTRACKR_API_URL = "https://api.example.com/api"
npm run build
```

Only the public API URL belongs in frontend configuration. Never place database
passwords, OAuth client secrets, JWT secrets, or Gmail token-encryption keys in
Angular environment files or Docker build arguments.

## Tests and build

```powershell
npm run config:check
npm test -- --watch=false
npm run build
```

## Container

The multi-stage Dockerfile builds Angular with Node 22 and serves the optimized
artifact through unprivileged application content on Nginx. Nginx supports
Angular route fallback, long-lived caching for hashed assets, no-store caching
for runtime configuration, security headers, health checks, and `/api`
proxying to the backend service on the private Docker network.

Build it directly:

```powershell
docker build --tag jobtrackr-frontend .
```

For the complete frontend, backend, and MySQL stack, use `docker compose` from
the repository root.
