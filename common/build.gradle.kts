plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
    api(libs.lombok)

    implementation(libs.commons.beanutils)
    implementation(libs.google.gson)
    implementation(libs.okhttp3)
    implementation(libs.reactor.core)
    implementation(libs.toml4j)
    implementation(libs.java.stellar.sdk)
    implementation(project(":api-schema"))

    annotationProcessor(libs.lombok)
}
