plugins {
    `java-library`
}

dependencies {
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // From projects
    implementation(project(":core"))
}

