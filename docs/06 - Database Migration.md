# Database Migrations

- [Database Migrations](#database-migrations)
  - [Applying Migrations](#applying-migrations)
  - [Adding a New Migration](#adding-a-new-migration)
    - [A. Adding a Database Connection](#a-adding-a-database-connection)
    - [B. Generating & Registering a new Changelog File](#b-generating--registering-a-new-changelog-file)

[Liquibase] is used by this project for database migrations. Liquibase is able to generate changelogs (migration scripts) and Spring will automatically apply the changes to the database.

## Applying Migrations

This project is configured to apply any pending migrations automaticvally when the application is started.

## Adding a New Migration

We usually generate the migrations using IntelliJ IDEA's JPA Buddy plugin. If you don't have it installed yet, please go ahead and download it at https://plugins.jetbrains.com/plugin/15075-jpa-buddy.

There's also an option to generate the migration from command-line or even manually, but we won't be covering that in this tutorial.

### A. Adding a Database Connection

Liquibase migration generator tool will need to compare your current database state with the project database entities, in order to be able to generate a diff that can bring your database to the desired state mapped in the code.

To connect a database, go to JPA Structure -> DB connections -> right click `new` -> DB connection

![img_4.png](img/liquibase-db-connection.png)

### B. Generating & Registering a new Changelog File

1. When data models or table names are changed, a new changelog can be generated. Right click in Liquibase -> New -> Diff Changelog:

    ![img.png](img/liquibase-changelog1.png)

2. Review the changes and generate the changelog.

    > Note: `Directory` should be set to ``platform/src/main/resources/db/changelog/`` so all the changelog files are in one place.

    ![img_1.png](img/liquibase-changelog2.png)

3. Add the generated changelog file to [`platform/src/main/resources/db/changelog/db.changelog-master.yaml`]

![img_3.png](img/liquibase-changelog3.png)

[Liquibase]: https://www.liquibase.org/
[`platform/src/main/resources/db/changelog/db.changelog-master.yaml`]: ../platform/src/main/resources/db/changelog/db.changelog-master.yaml
