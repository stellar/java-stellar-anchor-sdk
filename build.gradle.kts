// The alias call in plugins scope produces IntelliJ false error which is suppressed here.
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  java
  alias(libs.plugins.spotless)
  alias(libs.plugins.kotlin.jvm) apply false
}

tasks {
  register<Copy>("installLocalGitHook") {
    from("scripts/pre-commit.sh") { rename { it.removeSuffix(".sh") } }
    into(".git/hooks")

    doLast { project.exec { commandLine("chmod", "+x", ".git/hooks/pre-commit") } }
  }

  "build" { dependsOn("installLocalGitHook") }
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "com.diffplug.spotless")

  repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://packages.confluent.io/maven") }
  }

  /** Specifies JDK-11 */
  java { toolchain { languageVersion.set(JavaLanguageVersion.of(11)) } }

  spotless {
    val javaVersion = System.getProperty("java.version")
    if (javaVersion >= "17") {
      logger.warn("!!! WARNING !!!")
      logger.warn("=================")
      logger.warn(
          "    You are running Java version:[{}]. Spotless may not work well with JDK 17.",
          javaVersion)
      logger.warn(
          "    In IntelliJ, go to [File -> Build -> Execution, Build, Deployment -> Gradle] and check Gradle JVM")
    }

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
    // This is to fix the missing implementation in JSR305 that causes "unknown enum constant
    // When.MAYBE" warning.
    implementation(rootProject.libs.findbugs.jsr305)
    implementation(rootProject.libs.aws.sqs)
    implementation(rootProject.libs.postgresql)
    implementation(rootProject.libs.bundles.kafka)
    implementation(rootProject.libs.spring.kafka)

    // TODO: we should use log4j2
    implementation(rootProject.libs.log4j.template.json)

    // The common dependencies are declared here because we would like to have a uniform unit
    // testing across all subprojects.
    //
    // We need to use the dependency string because the VERSION_CATEGORY feature isn't supported in
    // subproject task as of today.
    //
    testImplementation(rootProject.libs.bundles.junit)
    testImplementation(rootProject.libs.jsonassert)

    testAnnotationProcessor(rootProject.libs.lombok)
  }

  /**
   * This is to fix the Windows default cp-1252 character encoding that may potentially cause
   * compilation error
   */
  tasks {
    compileJava {
      options.encoding = "UTF-8"

      /** Enforces google-java-format at Java compilation. */
      dependsOn("spotlessApply")
    }

    compileTestJava { options.encoding = "UTF-8" }

    javadoc { options.encoding = "UTF-8" }

    test {
      useJUnitPlatform()

      testLogging {
        events("SKIPPED", "FAILED")
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
      }
    }
  }

  configurations {
    all {
      exclude(group = "ch.qos.logback", module = "logback-classic")
      exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
      exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
  }
}

allprojects {
  group = "org.stellar.anchor-sdk"
  version = "1.2.2"

  tasks.jar {
    manifest {
      attributes(
        mapOf(
          "Implementation-Title" to project.name,
          "Implementation-Version" to project.version
        )
      )
    }
  }
}

tasks.register("printVersionName") {
  println(rootProject.version.toString())
}

