// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(libs.lombok)

  implementation(libs.google.gson)
  implementation(libs.reactor.core)

  annotationProcessor(libs.lombok)
}

tasks.test {
  // Enable parallel test execution
  systemProperty("junit.jupiter.execution.parallel.enabled", false)
  systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
  systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
  systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
  systemProperty(
    "junit.jupiter.testclass.order.default",
    "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation"
  )
  maxParallelForks =
    (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1).also {
      println("junit5 ... setting maxParallelForks to $it")
    }
}
