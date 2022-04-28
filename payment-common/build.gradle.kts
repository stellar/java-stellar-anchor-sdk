plugins {
    `java-library`
}

dependencies {
    api(libs.lombok)

    implementation(libs.google.gson)
    implementation(libs.reactor.core)
    implementation(project(":api-schema"))

    annotationProcessor(libs.lombok)
}
