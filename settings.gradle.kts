rootProject.name = "java-stellar-anchor-sdk"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      alias("apache.commons.lang3").to("org.apache.commons:commons-lang3:3.12.0")
      alias("commons.beanutils").to("commons-beanutils:commons-beanutils:1.9.4")
      alias("commons.cli").to("commons-cli:commons-cli:1.5.0")
      alias("commons.codec").to("commons-codec:commons-codec:1.15")
      alias("commons.io").to("commons-io:commons-io:2.11.0")
      alias("commons.validator").to("commons-validator:commons-validator:1.7")
      alias("google.gson").to("com.google.code.gson:gson:2.8.9")
      alias("httpclient").to("org.apache.httpcomponents:httpclient:4.5.13")
      alias("hibernate.types").to("com.vladmihalcea:hibernate-types-52:2.16.3")
      alias("jackson.dataformat.yaml")
          .to("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.2")
      alias("java.stellar.sdk").to("com.github.stellar:java-stellar-sdk:0.34.1")
      alias("javax.jaxb.api").to("javax.xml.bind:jaxb-api:2.3.1")
      alias("jjwt").to("io.jsonwebtoken:jjwt:0.9.1")
      alias("log4j2.api").to("org.apache.logging.log4j:log4j-api:.17.1")
      alias("log4j2.core").to("org.apache.logging.log4j:log4j-core:2.17.1")
      alias("log4j2.slf4j").to("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
      alias("lombok").to("org.projectlombok:lombok:1.18.22")
      alias("okhttp3").to("com.squareup.okhttp3:okhttp:4.9.3")
      alias("okhttp3.mockserver").to("com.squareup.okhttp3:mockwebserver:4.9.3")
      alias("reactor.core").to("io.projectreactor:reactor-core:3.4.14")
      alias("reactor.netty").to("io.projectreactor.netty:reactor-netty:1.0.15")
      alias("servlet.api").to("javax.servlet:servlet-api:2.5")
      alias("sqlite.jdbc").to("org.xerial:sqlite-jdbc:3.34.0")
      alias("slf4j.api").to("org.slf4j:slf4j-api:1.7.35")
      alias("slf4j.log4j12").to("org.slf4j:slf4j-log4j12:1.7.33")
      alias("toml4j").to("com.moandjiezana.toml:toml4j:0.7.2")
    }
  }
}

/** APIs and Schemas */
include("api-schema")

include("core")

/** Payment subprojects */
include("payment")

/** Anchor Platform */
include("platform")

/** Anchor Reference Server */
include("anchor-reference-server")


/** Integration tests */
include("integration-tests")

include("service-runner")
