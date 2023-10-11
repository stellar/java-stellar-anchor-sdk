// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  application
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(libs.lombok)

  implementation("org.springframework.boot:spring-boot")
  implementation("org.springframework.boot:spring-boot-autoconfigure")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation(libs.snakeyaml) // used to force the version of snakeyaml (used by springboot) to a safer one.
  implementation("org.springframework.boot:spring-boot-starter-web")

  implementation(libs.spring.aws.messaging)
  implementation(libs.spring.kafka)
  implementation(libs.h2database)
  implementation(libs.sqlite.jdbc)
  implementation(libs.google.gson)
  implementation(variantOf(libs.java.stellar.sdk) { classifier("uber") })
  implementation(libs.okhttp3)

  implementation(project(":api-schema"))
  implementation(project(":core"))

  annotationProcessor(libs.lombok)
}

application { mainClass.set("org.stellar.anchor.reference.AnchorReferenceServer") }
