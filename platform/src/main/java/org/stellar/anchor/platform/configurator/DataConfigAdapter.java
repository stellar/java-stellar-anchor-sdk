package org.stellar.anchor.platform.configurator;

import java.util.Arrays;
import java.util.List;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.util.Log;

/**
 * This adapter manages Spring datasource and jpa configurations accoring to the config map. The
 * following lists Spring data configurations that we manage based on: <a
 * href="https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html">https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html</a>
 *
 * <p>Spring Datasource:
 *
 * <pre>
 * spring.datasource.driver-class-name:            The JDBC driver class name
 * spring.datasource.name:                         Datasource name to use if "generate-unique-name" is false. Defaults to "testdb" when using an embedded database, otherwise null.
 * spring.datasource.type:                         Fully qualified name of the connection pool implementation to use. By default, it is auto-detected from the classpath.
 * spring.datasource.url:                          JDBC URL of the database.
 * spring.datasource.username:                     Login username of the database.
 * spring.datasource.password:                     Login password of the database.
 * spring.datasource.generate-unique-name:         Whether to generate a random datasource name.
 * </pre>
 *
 * Spring Connection Pool Management
 *
 * <pre>
 * spring.datasource.hikari.connection-timeout = 20000 # maximum number of milliseconds that a client will wait for a connection
 * spring.datasource.hikari.minimum-idle= 10           # minimum number of idle connections maintained by HikariCP in a connection pool
 * spring.datasource.hikari.maximum-pool-size= 10      # maximum pool size
 * spring.datasource.hikari.idle-timeout=10000         # maximum idle time for connection
 * spring.datasource.hikari.max-lifetime= 1000         # maximum lifetime in milliseconds of a connection in the pool after it is closed.
 * spring.datasource.hikari.auto-commit =true          # default auto-commit behavior.
 * </pre>
 *
 * Spring JPA.
 *
 * <pre>
 * spring.jpa.database: 	                           Target database to operate on, auto-detected by default. Can be alternatively set using the "databasePlatform" property.
 * spring.jpa.database-platform:                       Name of the target database to operate on, auto-detected by default. Can be alternatively set using the "Database" enum.
 * spring.jpa.defer-datasource-initialization:         Whether to defer DataSource initialization until after any EntityManagerFactory beans have been created and initialized.: false
 * spring.jpa.generate-ddl:                            Whether to initialize the schema on startup. : false
 * spring.jpa.hibernate.ddl-auto:                      DDL mode. This is actually a shortcut for the "hibernate.hbm2ddl.auto" property. Defaults to "create-drop" when using an embedded database and no schema manager was detected. Otherwise, defaults to "none".
 * spring.jpa.hibernate.naming.implicit-strategy:      Fully qualified name of the implicit naming strategy.
 * spring.jpa.hibernate.naming.physical-strategy:      Fully qualified name of the physical naming strategy.
 * spring.jpa.hibernate.use-new-id-generator-mappings: Whether to use Hibernate's newer IdentifierGenerator for AUTO, TABLE and SEQUENCE. This is actually a shortcut for the "hibernate.id.new_generator_mappings" property. When not specified will default to "true".
 * spring.jpa.mapping-resources:                       Mapping resources (equivalent to "mapping-file" entries in persistence.xml).
 * spring.jpa.open-in-view:                            Register OpenEntityManagerInViewInterceptor. Binds a JPA EntityManager to the thread for the entire processing of the request. : true
 * spring.jpa.show-sql:                                Show SQL : false
 * </pre>
 */
public class DataConfigAdapter extends SpringConfigAdapter {
  final List<String> allFields =
      Arrays.asList(
          "spring.datasource.driver-class-name",
          "spring.datasource.name",
          "spring.datasource.type",
          "spring.datasource.url",
          "spring.datasource.username",
          "spring.datasource.password",
          "spring.datasource.generate-unique-name",
          "spring.datasource.hikari.connection-timeout ",
          "spring.datasource.hikari.minimum-idle",
          "spring.datasource.hikari.maximum-pool-size",
          "spring.datasource.hikari.idle-timeout",
          "spring.datasource.hikari.max-lifetime",
          "spring.datasource.hikari.auto-commit ",
          "spring.jpa.database",
          "spring.jpa.database-platform",
          "spring.jpa.defer-datasource-initialization",
          "spring.jpa.generate-ddl",
          "spring.jpa.hibernate.use-new-id-generator-mappings",
          "spring.jpa.open-in-view",
          "spring.jpa.show-sql",
          "spring.flyway.enabled");

  @Override
  void updateSpringEnv(ConfigMap config) throws InvalidConfigException {
    // Set our default value to start with
    setSpringDataDefaults();

    //
    // The following code block purposely kept long for easier understanding of how the Spring
    // database configurations are set.
    //
    String type = config.getString("data.type").toLowerCase();
    switch (type) {
      case "h2":
        set("spring.datasource.driver-class-name", "org.h2.Driver");
        set("spring.datasource.embedded-database-connection", "H2");
        set("spring.datasource.name", "anchor-platform");
        set("spring.datasource.url", "jdbc:h2:mem:test");
        set("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
        set("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        break;
      case "sqlite":
        set("spring.datasource.driver-class-name", "org.sqlite.JDBC");
        set("spring.datasource.name", "anchor-platform");
        set("spring.jpa.database-platform", "org.stellar.anchor.platform.sqlite.SQLiteDialect");
        set("spring.jpa.generate-ddl", true);
        set("spring.jpa.hibernate.ddl-auto", "update");
        copy(config, "data.url", "spring.datasource.url");
        set("spring.datasource.username", SecretManager.getInstance().get("secret.data.username"));
        set("spring.datasource.password", SecretManager.getInstance().get("secret.data.password"));
        break;
      case "aurora":
        set("spring.datasource.driver-class-name", "org.postgresql.Driver");
        set("spring.datasource.name", "anchor-platform");
        set("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQL9Dialect");
        set(
            "spring.datasource.hikari.max-lifetime",
            840000); // 14 minutes because IAM tokens are valid for 15 min
        copy(config, "data.url", "spring.datasource.url");
        set("spring.datasource.username", SecretManager.getInstance().get("secret.data.username"));
        set("spring.datasource.password", SecretManager.getInstance().get("secret.data.password"));
        if (config.getString("data.flyway_enabled", "").equalsIgnoreCase("true")) {
          set("spring.flyway.enabled", true);
          set("spring.flyway.locations", "classpath:/db/migration");
          set("spring.flyway.user", SecretManager.getInstance().get("secret.data.username"));
          set("spring.flyway.password", SecretManager.getInstance().get("secret.data.password"));
          copy(config, "data.url", "spring.flyway.url");
        } else {
          set("spring.jpa.generate-ddl", true);
          set("spring.jpa.hibernate.ddl-auto", "update");
        }
        break;
      case "postgres":
        set("spring.datasource.driver-class-name", "org.postgresql.Driver");
        set("spring.datasource.name", "anchor-platform");
        set("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQL9Dialect");
        copy(config, "data.url", "spring.datasource.url");
        set("spring.datasource.username", SecretManager.getInstance().get("secret.data.username"));
        set("spring.datasource.password", SecretManager.getInstance().get("secret.data.password"));
        if (config.getString("data.flyway_enabled", "").equalsIgnoreCase("true")) {
          set("spring.flyway.enabled", true);
          set("spring.flyway.locations", "classpath:/db/migration");
          set("spring.flyway.user", SecretManager.getInstance().get("secret.data.username"));
          set("spring.flyway.password", SecretManager.getInstance().get("secret.data.password"));
          copy(config, "data.url", "spring.flyway.url");
        } else {
          set("spring.jpa.generate-ddl", true);
          set("spring.jpa.hibernate.ddl-auto", "update");
        }
        break;
      default:
        Log.errorF("Invalid config[data.type]={}", type);
        throw new InvalidConfigException(String.format("Invalid config[data.type]=%s", type));
    }

    checkIfAllFieldsAreSet();
  }

  private void checkIfAllFieldsAreSet() {
    allFields.forEach(
        field -> {
          if (get(field) == null) {
            Log.infoF("{} is not set.", field);
          }
        });
  }

  void setSpringDataDefaults() {
    set("spring.datasource.generate-unique-name", "false");

    set("spring.datasource.hikari.connection-timeout ", 20000); // in ms
    set("spring.datasource.hikari.minimum-idle", 10);
    set("spring.datasource.hikari.maximum-pool-size", 10);
    set("spring.datasource.hikari.idle-timeout", 10000);
    set("spring.datasource.hikari.max-lifetime", 1000);
    set("spring.datasource.hikari.auto-commit ", true);

    set("spring.jpa.database", "");
    set("spring.jpa.database-platform", "");
    set("spring.jpa.defer-datasource-initialization", false);
    set("spring.jpa.generate-ddl", false);
    set("spring.jpa.hibernate.use-new-id-generator-mappings", true);
    set("spring.jpa.open-in-view", true);
    set("spring.jpa.show-sql", false);

    set("spring.flyway.enabled", false);
  }
}
