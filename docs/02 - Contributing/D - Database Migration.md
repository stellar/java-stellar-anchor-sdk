# Database Migrations

- [Database Migrations](#database-migrations)
  - [Applying Migrations](#applying-migrations)
  - [Adding a New Migration](#adding-a-new-migration)
    - [A. Adding a Database Connection](#a-adding-a-database-connection)
    - [B. Generating & Registering a new Changelog File](#b-generating--registering-a-new-changelog-file)

[Flyway] is used by this project for database migrations. Flyway is able to generate migration scripts (via JPA buddy) 
and Spring will automatically apply the changes to the database (if Flyway is enabled).

## Applying Migrations

This project is configured to apply any pending migrations automatically when the application is started. If you want
to manually apply migrations (https://flywaydb.org/documentation/getstarted/firststeps/commandline), auto-apply can be 
disabled in the Anchor Platform Configuration:
```text
spring.flyway.enabled: false
```

## Adding a New Migration

We usually generate the migrations using IntelliJ IDEA's JPA Buddy plugin. If you don't have it installed yet, please 
go ahead and download it at https://plugins.jetbrains.com/plugin/15075-jpa-buddy.

There's also an option to generate the migration from command-line or even manually, but we won't be covering that in 
this tutorial.

### A. Adding a Database Connection

Flyway migration generator tool will need to compare your current database state with the project database entities, 
in order to be able to generate a diff that can bring your database to the desired state mapped in the code.

To connect a database, go to JPA Structure -> DB connections -> right click `new` -> DB connection

![img_4.png](/docs/resources/img/flyway-db-connection.png)

### B. Generating & Registering a new Changelog File

1. When data models or table names are changed, a new changelog can be generated. New(+) -> Diff Versioned Migration:

    ![img.png](/docs/resources/img/flyway-changelog1.png)

2. Select the DB connection to create a "diff" against

    ![img_3.png](/docs/resources/img/flyway-changelog2.png)

3. Review the changes and generate the migration file.
   > Note: The migration file format =`<Prefix><Version>__<Description>.sql`

    > Note: `Directory` should be set to `platform/src/main/resources/db/migration/` so all the changelog files are in one place.

    ![img_1.png](/docs/resources/img/flyway-changelog3.png)



[Flyway]: https://flywaydb.org/
