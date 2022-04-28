plugins {
    `java-library`
}

dependencies {
    api(libs.lombok)

    implementation(libs.commons.beanutils)
    implementation(libs.google.gson)
    implementation(libs.reactor.core)
    implementation(libs.java.stellar.sdk)
    implementation(project(":api-schema"))

    annotationProcessor(libs.lombok)
}
