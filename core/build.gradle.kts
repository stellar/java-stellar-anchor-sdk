// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  `maven-publish`
  signing
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  compileOnly(libs.servlet.api)
  compileOnly(libs.slf4j.api)

  api(libs.lombok)

  // Lombok should be used by all sub-projects to reduce Java verbosity
  annotationProcessor(libs.lombok)

  implementation(libs.spring.kafka)

  implementation(libs.bundles.kafka)

  // TODO: Consider to simplify
  implementation(libs.micrometer.prometheus)
  implementation(libs.javax.transaction.api)

  implementation(libs.commons.beanutils)
  implementation(libs.apache.commons.lang3)
  implementation(libs.log4j2.core)
  implementation(libs.httpclient)
  implementation(libs.google.gson)
  implementation(libs.toml4j)
  implementation(libs.okhttp3)
  implementation(libs.commons.codec)
  implementation(libs.jjwt)
  implementation(libs.reactor.core)
  implementation(libs.javax.jaxb.api)
  implementation(libs.java.stellar.sdk)

  implementation(project(":api-schema"))

  testImplementation(libs.okhttp3.mockserver)
  testImplementation(libs.servlet.api)
  testImplementation(libs.slf4j.api)
}

publishing {
  apply<MavenPublishPlugin>()

  configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
  }

  configure<PublishingExtension> {
    publications {
      val main by
          creating(MavenPublication::class) {
            from(components["java"])
            groupId = "org.stellar.anchor-sdk"

            pom {
              name.set("stellar-anchor-sdk")
              description.set("Stellar Anchor SDK - Java")
              url.set("https://github.com/stellar/java-stellar-anchor-sdk")
              licenses {
                license {
                  name.set("Apache 2.0")
                  url.set("https://github.com/stellar/java-stellar-anchor-sdk/blob/main/LICENSE")
                }
              }
              developers {
                developer {
                  id.set("lijamie98")
                  name.set("Jamie Li")
                  email.set("jamie@stellar.org")
                }
                developer {
                  id.set("JakeUrban")
                  name.set("Jake Urban")
                  email.set("jake@stellar.org")
                }
                developer {
                  id.set("marcelosalloum")
                  name.set("Marcelo Salloum")
                  email.set("marcelo@stellar.org")
                }
              }
              scm {
                connection.set("scm:git:git://github.com/stellar/java-stellar-anchor-sdk")
                developerConnection.set("scm:git:git://github.com/stellar/java-stellar-anchor-sdk")
                url.set("https://github.com/stellar/java-stellar-anchor-sdk")
              }
            }
          }
    }
    repositories {
      maven {
        name = "OSSRH"
        setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
        credentials {
          username = System.getenv("OSSRH_USER") ?: return@credentials
          password = System.getenv("OSSRH_PASSWORD") ?: return@credentials
        }
      }
    }
  }

  apply<SigningPlugin>()
  configure<SigningExtension> { sign(publishing.publications) }
}
