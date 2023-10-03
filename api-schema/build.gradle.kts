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
  // Allocate thread count based on available processors
  systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
  // Set default parallel mode to same thread. All tests within a class are run in sequence.
  systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
  // Set default parallel mode for classes to concurrent. All test classes are run in parallel.
  systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")

  // Set default test class order to order annotation. All test classes are run in parallel.
  // Some tests take longer to run. Enabling the order will execute long-running tests first to
  // shorten the overall test time.
  systemProperty(
    "junit.jupiter.testclass.order.default",
    "org.junit.jupiter.api.ClassOrderer\$OrderAnnotation"
  )
  maxParallelForks =
    (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1).also {
      println("junit5 ... setting maxParallelForks to $it")
    }
}
