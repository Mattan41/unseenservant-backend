# Unseen Servant Backend — Project Guidelines

## Read these before changing behaviour
- `docs/ARCHITECTURE.md` — package structure, and the **Concurrency** section: virtual
  threads are on, so the real ceilings are `server.tomcat.max-connections=500` and the
  Hikari pool (`maximum-pool-size=15`). Those are judgement calls, not tuned values —
  do not "optimise" them, and do not tune MySQL's buffer pool.
- `docs/MIGRATIONS.md` — Flyway owns the schema. SQL migrations live in
  `src/main/resources/db/migration`, Java migrations in
  `src/main/java/db/migration`; read the numbering rules there before adding one.
- `docs/INTEGRATION_CONTRACT.md` — binding for the frontend. Endpoints return DTOs,
  never entities, and changes have to stay backward compatible or be coordinated.
- `docs/API_REFERENCE.md` — the endpoint reference.
- Update the describing document in the same change as the behaviour.

## Build and tests
- The pom targets **Java 25** (`<java.version>25</java.version>`) but the JDK on PATH is
  21, so a bare `./mvnw` fails. Use
  `JAVA_HOME=~/.jdks/temurin-25.0.4.1 ./mvnw -B verify`.
- `verify` (not `test`) is the goal that runs everything: surefire runs `*Test`,
  failsafe runs `*IT`. The `*IT` classes use Testcontainers and need a working Docker
  daemon.
- No test framework beyond JUnit 5 + Testcontainers/AssertJ; do not introduce another
  one. A new class of behaviour deserves a test in the same style as its neighbours.

## Configuration
- Profiles: `prod` (env-driven, MySQL), `develop` (local MySQL), `demo` (H2 + seeded
  users User1–User5/admin, password `password`).
- The uppercase property names in `application-prod.properties` are read straight from
  environment variables (`SPRING_DATASOURCE_*`, `JWT_SECRET`, `ADMIN_WHITELIST`,
  `USER_WHITELIST`, `ALLOWED_ORIGINS`, `FRONTEND_URL`, `GOOGLE_*`, `GITHUB_*`,
  `FILE_UPLOAD_DIR`) and the host compose files interpolate exactly those names. Do not
  rename them.
- `FRONTEND_URL` is genuinely used: the two `/under-construction` redirects
  (`CustomOAuth2SuccessHandler`) use it directly, and it is the last fallback for the
  post-login redirect. The file has a stale TODO claiming it can be dropped — the code
  still reads it in three places.
- `ALLOWED_ORIGINS` gates the `oauth_redirect_origin` cookie and the dynamic redirect
  origin; the comparison is **exact string match**, so no trailing slash, no case
  drift, one entry per origin.
- OAuth `redirect_uri` is derived from the forwarded headers
  (`server.forward-headers-strategy=framework`), which means each host's nginx must
  send `X-Forwarded-Proto/Host/Port` on `/oauth2/` and `/login/oauth2/`, and every new
  hostname has to be registered at Google and GitHub before it can log in.
- `ADMIN_WHITELIST` / `USER_WHITELIST` are **first-run only**: `WhitelistLoader` seeds
  while `email_whitelist` is empty. A new environment must get them right before its
  first start.
- `file.upload-dir` has no default, so an empty `FILE_UPLOAD_DIR` (a missing key in a
  host `.env` is enough) starts the app but breaks every upload.
- `spring.jpa.hibernate.ddl-auto=validate` in prod: schema changes go through Flyway,
  never through Hibernate.

## Host and ports
- The app listens on 8080 inside the container. 3306 (MySQL) and 5005 (JDWP) are never
  published; the debug override binds JDWP to `127.0.0.1` and is reached over SSH.
- Image tags: `latest` is production (a published release), `main` is staging. A merge
  to main must never move `latest`.

## Language
- Identifiers, comments and docs in English. Existing Swedish names or comments are
  translated when that code is touched anyway — not in a sweeping rename of its own.
