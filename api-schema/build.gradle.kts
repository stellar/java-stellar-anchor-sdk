plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

tasks {
    processResources {
        doFirst {
            val existingFile = file("$buildDir/resources/main/metadata.properties")
            println(existingFile.exists())
            existingFile.delete()
            println(existingFile.exists())
        }
        filter { line -> line.replace("%APP_VERSION_TOKEN%", rootProject.version.toString()) }
    }
}

dependencies {
    api(libs.lombok)

    implementation(libs.google.gson)
    implementation(libs.reactor.core)

    annotationProcessor(libs.lombok)
}
