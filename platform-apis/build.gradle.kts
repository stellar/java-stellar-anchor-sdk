plugins {
    `java-library`
}

dependencies {
    api(libs.lombok)

    implementation(libs.google.gson)

    annotationProcessor(libs.lombok)
}
