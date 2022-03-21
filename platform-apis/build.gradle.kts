plugins {
    `java-library`
}

dependencies {
    api(libs.lombok)

    implementation(libs.google.gson)
    implementation(libs.jackson.annotations)

    annotationProcessor(libs.lombok)
}
