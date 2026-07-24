# JobTrackr

[![Continuous Integration](https://github.com/NeelLogic/JobTrackr/actions/workflows/ci.yml/badge.svg)](https://github.com/NeelLogic/JobTrackr/actions/workflows/ci.yml)

JobTrackr is a full-stack job application tracker for students and new graduates. It provides a secure, user-specific workspace for organizing opportunities, following application progress, and understanding job-search activity.

> **Project status:** Phase 6 is complete. The application, frontend integration, automated tests, accessibility pass, responsive design, and CI quality gates are implemented. Docker and Render deployment are planned for Phase 7.

## Features

- Account registration and login with BCrypt password hashing and JWT authentication
- Protected Angular routes and authenticated API requests
- User-specific data isolation at the repository and service layers
- Complete job application create, read, update, and delete workflows
- Company, job title, location, job URL, dates, employment type, salary, notes, and follow-up tracking
- Saved, Applied, Assessment, Interview, Offer, Rejected, and Withdrawn statuses
- Search by company, job title, or location
- Status and employment-type filters
- Server-side sorting and pagination
- Dashboard totals, monthly activity, interview/offer/rejection counts, status distribution, and recent applications
- Responsive layouts, keyboard navigation, accessible form errors, and loading, empty, and error states

## Tech Stack

| Layer              | Technologies                                                                |
| ------------------ | --------------------------------------------------------------------------- |
| Frontend           | Angular 21, TypeScript 5.9, RxJS, SCSS                                      |
| Backend            | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Bean Validation |
| Database           | MySQL, Flyway migrations; H2 for automated tests                            |
| Authentication     | BCrypt, stateless JWT bearer authentication                                 |
| Testing            | JUnit 5, Spring Boot Test, Spring Security Test, Vitest                     |
| Tooling            | Maven Wrapper, npm, Git, GitHub Actions                                     |
| Planned deployment | Docker and Render                                                           |

## Architecture

```mermaid
flowchart LR
    UI["Angular SPA"] -->|"JWT + JSON"| API["Spring Boot REST API"]
    API --> SEC["Spring Security / JWT filter"]
    SEC --> CTRL["Controllers"]
    CTRL --> SVC["Services and validation"]
    SVC --> REPO["Spring Data repositories"]
    REPO --> DB[("MySQL")]
    FLY["Flyway migrations"] --> DB
```

The backend uses a controller-service-repository structure with DTOs, entity mapping, validation, and centralized exception handling. All application queries are scoped to the authenticated user, including individual record lookups, updates, and deletion.

## Repository Structure

```text
JobTrackr/
|-- Backend/jobtrackr/       Spring Boot API, migrations, and tests
|-- Frontend/                Angular application and component tests
|-- .github/workflows/       Continuous integration
`-- README.md
```

## API Overview

| Method   | Endpoint                 | Purpose                                         |
| -------- | ------------------------ | ----------------------------------------------- |
| `POST`   | `/api/auth/register`     | Create an account                               |
| `POST`   | `/api/auth/login`        | Authenticate and receive a JWT                  |
| `GET`    | `/api/applications`      | Search, filter, sort, and paginate applications |
| `POST`   | `/api/applications`      | Create an application                           |
| `GET`    | `/api/applications/{id}` | View an owned application                       |
| `PUT`    | `/api/applications/{id}` | Update an owned application                     |
| `DELETE` | `/api/applications/{id}` | Delete an owned application                     |
| `GET`    | `/api/dashboard`         | Retrieve user-specific analytics                |
| `GET`    | `/api/health`            | Check API health                                |

Except for authentication and health checks, endpoints require an `Authorization: Bearer <token>` header.

## Local Development

### Prerequisites

- Java 21 or newer
- Node.js 22 and npm 10
- MySQL 8

### 1. Start MySQL

Make sure MySQL is running and that the configured user can create or access `jobtrackr_db`. Flyway creates and validates the application tables automatically.

### 2. Run the backend

From PowerShell:

```powershell
cd Backend\jobtrackr

$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-mysql-password"
$env:JWT_SECRET = "replace-with-a-random-secret-of-at-least-32-characters"

.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8080`. Verify it with `http://localhost:8080/api/health`.

### 3. Run the frontend

In a second terminal:

```powershell
cd Frontend
npm ci
npm start
```

Open `http://localhost:4200`. The Angular development proxy forwards `/api` requests to the backend.

## Environment Variables

| Variable               | Required | Default                        | Description                                           |
| ---------------------- | -------- | ------------------------------ | ----------------------------------------------------- |
| `DB_URL`               | No       | Local MySQL `jobtrackr_db` URL | JDBC connection URL                                   |
| `DB_USERNAME`          | No       | `jobtrackr`                    | Database username                                     |
| `DB_PASSWORD`          | Yes      | None                           | Database password                                     |
| `DB_POOL_SIZE`         | No       | `10`                           | Maximum connection-pool size                          |
| `JWT_SECRET`           | Yes      | None                           | JWT signing secret; use at least 32 random characters |
| `JWT_EXPIRATION_MS`    | No       | `86400000`                     | Token lifetime in milliseconds                        |
| `CORS_ALLOWED_ORIGINS` | No       | `http://localhost:4200`        | Comma-separated allowed frontend origins              |

Never commit real credentials or production secrets. Configure them through local environment variables and, in Phase 7, the Render environment settings.

## Testing

Run the backend test suite and production package:

```powershell
cd Backend\jobtrackr
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Run the frontend tests and production build:

```powershell
cd Frontend
npm test -- --watch=false
npm run build
```

Current Phase 6 baseline:

- 21 backend tests covering authentication, authorization, validation, user data isolation, services, JWT behavior, and API integration
- 41 frontend tests covering API services, route guards, authentication, dashboard, application workflows, and navigation

## Continuous Integration

GitHub Actions runs on every pull request to `main` and every push to `main`:

- Backend tests and executable JAR build
- Frontend formatting check, tests, and production build
- Pull-request dependency security review

Merges should only proceed after all required checks pass.

## Security

- Passwords are hashed with BCrypt and never returned by the API
- The API is stateless and validates signed JWTs on protected endpoints
- CORS origins are environment-configurable
- Request DTOs enforce field, date, URL, currency, and salary validation
- Centralized error responses omit stack traces and internal details
- Application access is always scoped to the authenticated user
- GitHub secret scanning, push protection, dependency graph, and dependency review are enabled

For a future production hardening pass, token storage can move from browser local storage to short-lived access tokens with secure, HTTP-only refresh cookies.

## Project Phases

| Phase | Scope                                                            | Status   |
| ----- | ---------------------------------------------------------------- | -------- |
| 1     | Repository foundation and planning                               | Complete |
| 2     | Backend architecture and authentication                          | Complete |
| 3     | Application management, validation, and data isolation           | Complete |
| 4     | Dashboard analytics and query capabilities                       | Complete |
| 5     | Angular frontend and API integration                             | Complete |
| 6     | Frontend testing, accessibility, responsive polish, and CI gates | Complete |
| 7     | Docker and Render deployment readiness                           | Next     |
| 8     | Final documentation, manual QA, and release review               | Planned  |

### Phase Closeout Checklist

Every phase is complete only after:

- Relevant tests and production builds pass
- Security and repository-cleanliness checks pass
- The README reflects the features, setup, test counts, deployment state, and next phase
- A focused pull request passes all required GitHub checks

## Planned Improvements

- Dockerfiles and Docker Compose for repeatable local environments
- Cost-conscious Render deployment configuration
- Hosted application screenshot and deployment link
- End-to-end browser tests for production-critical workflows
- Optional refresh-token rotation and password-reset workflow
