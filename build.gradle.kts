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
        java {
            importOrder("java", "javax", "org.stellar")
            removeUnusedImports()
            googleJavaFormat()

            if (System.getProperty("java.version").startsWith("17")) {
                logger.warn("!!! WARNING !!!")
                logger.warn("=================")
                logger.warn("    You are running Java version:[{}]. Spotless may not work well with JDK 17.",
                    System.getProperty("java.version"))
                logger.warn("    In IntelliJ, go to [File -> Build -> Execution, Build, Deployment -> Gradle] and check Gradle JVM")
            }
        }
    }
}
