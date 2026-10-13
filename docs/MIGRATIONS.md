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
| V10 | `V10__promote_offworlders_look_to_generic_appearance.sql` | SQL | Move Offworlders `look` → generic `game_character.appearance` |
| V11 | `V11__add_campaign_primary_system_and_ship.sql` | SQL | Add `campaign.primary_system` and the 1:1 `ship` table |
| V12 | `V12__add_campaign_private_description.sql` | SQL | Add `campaign.private_description` (owner/GM only) |
| V13 | `V13__add_ship_images.sql` | SQL | Add `ship.image_url` (profile image) + `ship.image_urls` (gallery) |
| V14 | `V14__add_campaign_world_lore.sql` | SQL | Add `campaign.world_lore` (public world lore & background) |
| V15 | `V15__add_ship_credits.sql` | SQL | Add `ship.credits` (shared crew credits balance) |
| V16 | `V16__add_ship_crew.sql` | SQL | Add `ship_crew` (one crew entry per participant) |

Versions are complete and strictly increasing (1–16, no gaps or duplicates).

## Rules

1. **Never edit a migration that has run.** Flyway stores a checksum in
   `flyway_schema_history`; changing an applied file makes `flyway validate` fail
   on every existing database. Add a new version instead. (V5/V6 are on `main` and
   are therefore frozen.)
2. **A new migration is immutable once merged.** Decide on SQL vs Java *before*
   the first commit — V8 was still changeable only because it was uncommitted.
3. **Naming:** `V<version>__<snake_case_description>.sql|.java`, versions strictly
   increasing with no gaps.
4. **Check the next free version before creating a file.** Versions are shared
   across the SQL and Java migrations, and a duplicate makes Flyway abort at
   startup with `Found more than one migration with version N` — the app never
   boots. List the whole folder (don't filter the output) and take
   `highest + 1`; if two branches are in flight, take `highest + 2` rather than
   reusing a number.
5. **SQL by default.** Use a Java migration only when a JSON/data transform
   genuinely requires it.
6. **Keep the `demo` profile working.** It does not run Flyway, so any new table or
   column is created by Hibernate from the entity; update `demo-data.sql` if a seed
   needs the new data.

## Prod sanity check

```sql
SELECT version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

should list V1–V16 with `success = 1`.
