plugins {
    java
    id("com.diffplug.spotless") version "6.2.1"
}

/**
 * The following block is applied to all sub-projects
 */
subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }

    /**
     * Specifies JDK-11
     */
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    }

    /**
     * Enforces google-java-format at Java compilation.
     */
    tasks.named("compileJava") {
        this.dependsOn("spotlessApply")
    }

    spotless {
        val javaVersion = System.getProperty("java.version")
        if (javaVersion >= "17") {
            logger.warn("!!! WARNING !!!")
            logger.warn("=================")
            logger.warn(
                "    You are running Java version:[{}]. Spotless may not work well with JDK 17.",
                javaVersion
            )
            logger.warn("    In IntelliJ, go to [File -> Build -> Execution, Build, Deployment -> Gradle] and check Gradle JVM")
        }

        if (javaVersion < "11") {
            throw GradleException("Java 11 or greater is required for spotless Gradle plugin.")
        }

        java {
            importOrder("java", "javax", "org.stellar")
            removeUnusedImports()
            googleJavaFormat()
        }

        kotlin {
            ktfmt("0.30").googleStyle()
        }
    }
}
