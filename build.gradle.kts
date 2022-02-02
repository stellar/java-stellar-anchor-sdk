plugins {
    `java`
}

/**
 * The following block is applied to all sub-projects
 */
subprojects {
    apply(plugin = "java")

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
}
