# Database Migrations (Flyway)

Flyway manages the schema for this backend. This note records how the migrations
are organised, how the squash to a single baseline was done, and the rules to
follow when adding new ones.

## The baseline

`src/main/resources/db/migration/V1__baseline_schema.sql` **is** the schema: the
production schema as of the 2026-10-20 backup, taken with
`mysqldump --no-data` and squashed from every migration that came before it
(V1–V17, including the former Java `V5`/`V6`/`V8`). It is deliberately the only
migration in the repository.

It replaced the old chain for two reasons. The three Java migrations had to be
edited for the Spring Boot 4 / Jackson 3 upgrade, and editing a migration that has
already run is exactly what the checksum rule forbids — the choice stood between
surgery on frozen files and a clean slate. And the chain described a history no
live database needs replayed: every one of them has the resulting schema already.

The former files are not lost. They are in the git history before this change:

```sh
git show 75de5f7:src/main/resources/db/migration/V2__split_character_system_data.sql
git show 75de5f7:src/main/java/db/migration/V5__replace_offworlders_gear_with_items.java
git show 75de5f7:docs/MIGRATIONS.md          # the full V1–V17 table
```

Two properties of the baseline file are deliberate:

* **Tables are ordered so every foreign key points at a table created above it**
  (`users`, `email_whitelist`, `spell`, then the children in dependency order).
  The file therefore runs with foreign key checking *on*, so a wrong order fails
  loudly instead of hiding behind `SET FOREIGN_KEY_CHECKS = 0`.
* **No data and no environment specifics** — no `INSERT`, no
  `AUTO_INCREMENT = <n>` table options, no `DEFINER`, no `CREATE DATABASE`. A
  baseline that carries a counter from one database into another is a trap, and
  re-dumping the file is how one gets in.

The `V<version>__<description>` prefix is kept so the file is an ordinary Flyway
migration: a fresh database runs it and then continues with V2, V3, …

## Environments

The project runs on the **MySQL 8.4 LTS** series. The image tag is pinned in four
places and they must be bumped together, or CI and local development drift apart:

| Where | Pin |
|-------|-----|
| `compose.yaml` | `mysql:8.4.11` (dev database started by Spring Boot's Docker Compose support) |
| `.github/workflows/docker-image.yml` | `mysql:8.4.11` (release-runner service container) |
| `TestContainersConfiguration` | `mysql:8.4.11` (reused test container) |
| `SchemaValidationIT` | `mysql:8.4.11` (non-reused validation container) |

| Profile | Database | Flyway | Schema created by |
|---------|----------|--------|-------------------|
| `prod` | MySQL 8.4 LTS (`compose.yaml`: `mysql:8.4.11`) | enabled | Flyway |
| `develop` | MySQL 8.4 LTS (docker compose) | enabled | Flyway |
| `test` | MySQL 8.4 LTS Testcontainer (`TestContainersConfiguration`, `mysql:8.4.11`) | enabled | Flyway |
| `demo` | H2 in-memory | **disabled** (`spring.flyway.enabled=false`) | Hibernate `ddl-auto=create-drop` + `demo-data.sql` |

Flyway reaches the application through `spring-boot-starter-flyway` (plus
`flyway-mysql`). Spring Boot 4 moved the auto-configuration into a module of its
own, so `flyway-core` alone no longer starts it — and without it a fresh database
would be left with no tables at all, baseline included.

Because the `demo` profile disables Flyway and builds the schema from the JPA
entities, H2 never executes migrations. That is why MySQL-specific SQL in
migrations is safe.

## Existing databases: baselined, not migrated

A database that already has this schema — staging, production, or a developer's
database restored from a backup — must **not** run the baseline file: its tables
already exist. It is *marked* as baselined instead, and from then on only
migrations after V1 run.

Flyway does that itself through `baseline-on-migrate`, which the `prod` profile
already sets (`spring.flyway.baseline-on-migrate=true`). When Flyway finds a
non-empty schema and no `flyway_schema_history` table, it records the baseline
version as applied and skips everything up to and including `V1`.

The one manual step is removing the old history, because it lists migrations that
no longer exist in the repository:

```sql
-- on the target database, with the application stopped
DROP TABLE flyway_schema_history;
```

Then start the application. It recreates `flyway_schema_history` with a single
`<< Baseline >>` row and logs that no migration was necessary; the data is
untouched.

| Environment | What to do |
|---|---|
| `demo` (H2) | nothing — Flyway is disabled, Hibernate builds the schema |
| fresh `develop` / test container | nothing — V1 runs and builds the schema |
| `develop` restored from a backup | drop `flyway_schema_history`, then start the app |
| a developer's reused test container | drop and recreate `testdb` **once** after this change: a `create-drop` run leaves `flyway_schema_history` behind with the old checksums and no tables, so every integration test fails with `Migration checksum mismatch for migration version 1` until it is reset |
| staging | pull the latest backup, drop `flyway_schema_history`, start the app, read the log |
| production | the same as staging, in the same step as the release: drop the table, deploy, then verify a login, a campaign and an image |

Verify with the query at the end of this note: it must show a single row, version
`1`, description `<< Baseline >>`.

## Rules

1. **Never edit a migration that has run.** Flyway stores a checksum in
   `flyway_schema_history`; changing an applied file makes `flyway validate` fail
   on every database that has it. Add a new version instead. Once staging or
   production has baselined against `V1__baseline_schema.sql`, that file is in
   the same position the old V5/V6 were: frozen.
2. **SQL only, unless SQL genuinely cannot express it.** The Java migrations
   existed to reshape legacy JSON (`gear` → `items`, string arrays → entry
   objects, `items` → typed `weapons`) that no longer occurs anywhere; that shape
   of migration is also what SonarQube objected to. If a transform really needs
   Java, say why in the pull request instead of reaching for
   `BaseJavaMigration` by habit.
3. **Naming:** `V<version>__<snake_case_description>.sql`, versions strictly
   increasing with no gaps. The next migration after this change is `V2`.
4. **Check the next free version before creating a file.** A duplicate makes
   Flyway abort at startup with `Found more than one migration with version N` —
   the app never boots. If two branches are in flight, skip a number rather than
   reusing one.
5. **Order is part of the file.** A table's parents have to be created before the
   table itself: `SchemaValidationIT` runs the whole chain against a fresh MySQL
   container, so a broken order or a `CREATE TABLE` that assumes something the
   file has not done yet fails there rather than in production.
6. **Keep the `demo` profile working.** It does not run Flyway, so any new table or
   column is created by Hibernate from the entity; update `demo-data.sql` if a seed
   needs the new data.

## Sanity check

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

* On a **fresh** database: one row, version `1`, type `SQL`, `success = 1`.
* On a **baselined** database: one row, version `1`, description
  `<< Baseline >>`, type `BASELINE`, `success = 1`.

Anything else means the history was not reset (an old row for a migration that no
longer exists blocks startup), or that two files claim the same version.
