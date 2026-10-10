# Database Migrations (Flyway)

Flyway manages the schema and data migrations for this backend. This note records
how the migrations are organised, why some of them are Java, and the rules to
follow when adding new ones.

## One logical location, two physical folders

Flyway is configured with the single location `classpath:db/migration` (Spring
Boot's default; stated explicitly as `spring.flyway.locations=classpath:db/migration`
in the `test` profile). It loads **both** kinds of migration from that one place:

| Kind | Physical location | Discovered as |
|------|-------------------|---------------|
| SQL  | `src/main/resources/db/migration/Vn__*.sql` | classpath resource `db/migration/Vn__*.sql` |
| Java | `src/main/java/db/migration/Vn__*.java` (package `db.migration`) | class `db.migration.Vn__...` on the classpath |

Both compile/copy into `target/classes/db/migration/`, so Flyway sees them as one
ordered list. Seeing two folders in the IDE is only a consequence of file type —
a Java migration cannot be a `.sql` resource, so its source must live in a Java
source root.

## Why some migrations are Java

V5, V6 and V8 parse and rewrite JSON stored in `TEXT` columns (reading an array
and rewriting each element's shape). Doing that in SQL is database-specific, so
those migrations subclass `BaseJavaMigration`; that also keeps the transform logic
pure and unit-testable (`OffworldersGearMigrationTest`, `OffworldersEntryMigrationTest`,
`OffworldersWeaponsMigrationTest`).

V5 and V6 are already merged to `main`, so their Flyway checksums are **frozen**
and they can never be rewritten as SQL.

**Only MySQL 8 ever runs Flyway** (see the environment matrix below), so SQL
migrations may use MySQL 8 syntax. Going forward: **SQL by default**, and Java only
for genuine JSON/data surgery that SQL cannot express cleanly.

## Environments

| Profile | Database | Flyway | Schema created by |
|---------|----------|--------|-------------------|
| `prod` | MySQL 8 (`compose.yaml`: `mysql:8.0.42`) | enabled | Flyway |
| `develop` | MySQL 8 (docker compose) | enabled | Flyway |
| `test` | MySQL 8 Testcontainer (`TestContainersConfiguration`, `mysql:8.0.42`) | enabled | Flyway |
| `demo` | H2 in-memory | **disabled** (`spring.flyway.enabled=false`) | Hibernate `ddl-auto=create-drop` + `demo-data.sql` |

Because the `demo` profile disables Flyway and builds the schema from the JPA
entities, H2 never executes migrations. That is why MySQL-specific SQL in
migrations is safe.

## Migration sequence

| Version | File | Kind | Purpose |
|---------|------|------|---------|
| V1 | `V1__.sql` | SQL | Initial schema |
| V2 | `V2__split_character_system_data.sql` | SQL | Split 5e data into `dnd5e_character_data` |
| V3 | `V3__add_offworlders_character_data.sql` | SQL | Add `offworlders_character_data` |
| V4 | `V4__add_offworlders_credits_and_gear.sql` | SQL | Credits + legacy `gear` block |
| V5 | `V5__replace_offworlders_gear_with_items.java` | Java | `gear` → free-form `items` array |
| V6 | `V6__offworlders_skills_abilities_as_entries.java` | Java | skills/abilities strings → `{ name, description }` |
| V7 | `V7__offworlders_health_tracking.sql` | SQL | `current_health`, `health_modifier` |
| V8 | `V8__split_offworlders_weapons_and_items.java` | Java | 1.5 `items` → typed `weapons` + free-text `items`, `armor` folded |
| V9 | `V9__add_character_backstory.sql` | SQL | Add `backstory` + `private_backstory` to `game_character` |

Versions are complete and strictly increasing (1–9, no gaps or duplicates).

## Rules

1. **Never edit a migration that has run.** Flyway stores a checksum in
   `flyway_schema_history`; changing an applied file makes `flyway validate` fail
   on every existing database. Add a new version instead. (V5/V6 are on `main` and
   are therefore frozen.)
2. **A new migration is immutable once merged.** Decide on SQL vs Java *before*
   the first commit — V8 was still changeable only because it was uncommitted.
3. **Naming:** `V<version>__<snake_case_description>.sql|.java`, versions strictly
   increasing with no gaps.
4. **SQL by default.** Use a Java migration only when a JSON/data transform
   genuinely requires it.
5. **Keep the `demo` profile working.** It does not run Flyway, so any new table or
   column is created by Hibernate from the entity; update `demo-data.sql` if a seed
   needs the new data.

## Prod sanity check

```sql
SELECT version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

should list V1–V9 with `success = 1`.
