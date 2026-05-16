# Database Scripts

This directory keeps database-only artifacts out of the backend project root.

- `schema/`: reference DDL for a full schema reset.
- `migrations/`: manual SQL migrations used before Flyway/Liquibase is introduced.
- `seed/`: local development seed scripts.

Production deployment still relies on Spring JPA `ddl-auto=update` to create or evolve tables. For a full test reset, start the backend once so Hibernate creates the tables, then import a generated seed script with explicit `create_time` values because the JPA-created tables do not define database defaults for those columns.
