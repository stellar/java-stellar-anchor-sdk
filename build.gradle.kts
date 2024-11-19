plugins {
  java
  id("com.diffplug.spotless") version "6.2.1"
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "com.diffplug.spotless")

  group = "org.stellar.anchor-sdk"
  version = "0.1.0"

  repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  /** Specifies JDK-11 */
  java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }

  /** Enforces google-java-format at Java compilation. */
  tasks.named("compileJava") { this.dependsOn("spotlessApply") }

  spotless {
    val javaVersion = System.getProperty("java.version")

    if (javaVersion < "11") {
      throw GradleException("Java 11 or greater is required for spotless Gradle plugin.")
    }

    java {
      importOrder("java", "javax", "org.stellar")
      removeUnusedImports()
      googleJavaFormat()
    }

    kotlin { ktfmt("0.30").googleStyle() }
  }

  dependencies {
    // The common dependencies are declared here because we would like to have a uniform unit
    // testing across all subprojects.
    //
    // We need to use the dependency string because the VERSION_CATEGORY feature isn't supported in
    // subproject task as of today.
    //
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.6.10")
    testImplementation("io.mockk:mockk:1.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

    testAnnotationProcessor("org.projectlombok:lombok:1.18.22")
  }

  /** JUnit5 should be used for all subprojects. */
  tasks.test { useJUnitPlatform() }
}
