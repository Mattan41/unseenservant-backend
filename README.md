# Unseen Servant – Backend

REST API for campaign tracking and character management for tabletop RPG groups.
Originally built as a thesis project at IT-Högskolan (2025), now maintained as a hobby project.

## Companion Frontend

To view and interact with the user interface, you will need the frontend application running:
[Unseen Servant - Frontend Repository](https://github.com/Mattan41/unseenservant-frontend)

## Tech Stack

- Spring Boot · Spring Security · Spring Data JPA
- MySQL · JWT authentication
- OAuth2 (Google, GitHub)
- Maven

## Features

- User authentication via OAuth2 and JWT
- Campaign management with participant roles (GM/Player)
- Character import and tracking
- File uploads for character portraits

## Getting Started

### 1. Configure environment

Create an `application-develop.properties` file (or use the included one) in `src/main/resources/`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydatabase
spring.datasource.username=myuser
spring.datasource.password=secret
FRONTEND_URL=http://localhost:5173
jwt.secret=your-secret-key-here
GOOGLE_ID=your-google-oauth-id
GOOGLE_SECRET=your-google-oauth-secret
GITHUB_ID=your-github-oauth-id
GITHUB_SECRET=your-github-oauth-secret

```

### 2. Build and run

Since the project includes a `compose.yaml` file, modern IDEs like IntelliJ IDEA will automatically spin up the MySQL container for you when you run the application.

*(You can just press "Run" inside IntelliJ IDEA with the `develop` profile active).*

If you are running from the terminal, start the database first:

```sh
docker compose up -d

```

Then build and run the application with the `develop` profile active:

```sh
./mvnw spring-boot:run -Dspring.profiles.active=develop

```

Backend runs on default at `http://localhost:8080`

### Demo Profile

For quick local testing without OAuth2 credentials or external database, use the demo profile with an in-memory H2 database:
> **Note:** For this to work seamlessly, you must also start the frontend application using `npm run demo`!

```sh
./mvnw spring-boot:run -Dspring.profiles.active=demo
```

The demo profile automatically preloads test users and sample data. Login credentials:

| Username | Email | Password | Role |
| :--- | :--- | :--- | :--- |
| User1 | user1@example.com | password | User |
| User2 | user2@example.com | password | User |
| User3 | user3@example.com | password | User |
| User4 | user4@example.com | password | User |
| User5 | user5@example.com | password | User |
| admin | admin@admin.se | password | Admin |

H2 console is available at `http://localhost:8080/h2-console` for database inspection.

## Environment Variables

| Variable | Required | Description |
| :--- | :--- | :--- |
| `GOOGLE_ID` | Yes | OAuth2 Google client ID |
| `GOOGLE_SECRET` | Yes | OAuth2 Google client secret |
| `GITHUB_ID` | Yes | OAuth2 GitHub client ID |
| `GITHUB_SECRET` | Yes | OAuth2 GitHub client secret |
| `JWT_SECRET` | Yes | Secret key for JWT signing |
| `ADMIN_WHITELIST` | No | Comma-separated emails with admin access. Read **only while the `email_whitelist` table is empty** — see below |
| `USER_WHITELIST` | No | Comma-separated emails with user access. Same first-run-only rule |
| `FILE_UPLOAD_DIR` | No | Directory for character portrait uploads (default: `./uploads/character-images`) |
| `APP_UPLOAD_MAX_FILE_SIZE` | No | Max size per uploaded image, enforced by `FileStorageService` (default: `5MB`) |
| `SPRING_DATASOURCE_URL` | Yes (prod) | Database connection URL |
| `SPRING_DATASOURCE_USERNAME` | Yes (prod) | Database user |
| `SPRING_DATASOURCE_PASSWORD` | Yes (prod) | Database password |

`ADMIN_WHITELIST` and `USER_WHITELIST` are **first-run-only**.
`WhitelistLoader` seeds the `email_whitelist` table only while it is empty, so a
fresh database takes exactly the values present when it first starts and later
edits to these variables have no effect. A new environment has to set both before
its first start, or nobody can log in — and correcting a mistake means starting
over from an empty database.

## Running Tests

```sh
./mvnw test

```

## Deployment

The production Docker image is built automatically via GitHub Actions on every new release and pushed to Docker Hub (`mattan41/unseenservant-backend`).

The image is built for `linux/amd64` and `linux/arm64` on native GitHub-hosted
runners (`ubuntu-latest` / `ubuntu-24.04-arm`) and published as a single
multi-arch manifest, so `docker pull` selects the right architecture
automatically. Each architecture is pushed by digest and the manifest is
assembled with `docker buildx imagetools create`. Because buildx provenance is
left at its default, the final index also lists the per-architecture
attestation manifests as extra `unknown/unknown` platform entries — expected,
and it does not affect `docker pull` or `docker run`.

Build cache and per-architecture staging images live in the GitHub Container
Registry (`ghcr.io/mattan41/unseenservant-backend:buildcache-<arch>`), not on
Docker Hub; only the final multi-arch image is published to Docker Hub.

The application serves requests on virtual threads
(`spring.threads.virtual.enabled`); `server.tomcat.max-connections` and the
Hikari pool are therefore the concurrency ceilings rather than Tomcat's
worker-thread pool. See [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) →
"Concurrency" before changing either.

### Tags and environments

Two environments consume these images:

| | Follows | Moved by |
|---|---|---|
| Production (OptiPlex) | `latest` | a published release — production's watchtower polls it |
| Staging (Raspberry Pi 5) | `main` | every merge to `main` |

Because production's watchtower follows `latest`, a merge-to-main workflow must
never be able to move that tag: the workflow resolves its tags in a single step
and a guard step fails the run if `latest` would be published outside a release
event. The host-side runbook and the verification checklist live in the
infrastructure repository (`Mattan41/projects-notes`,
`unseen-servant/staging/README.md`).

Neither host publishes 3306 (MySQL) or 5005 (JDWP). The JDWP agent is only ever
enabled through the explicit `docker-compose.debug.yml` override, which binds it
to `127.0.0.1` and is reached over an SSH tunnel.

```
