plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
    // ***************************
    // Production dependencies
    // ***************************
    // From mavenCentral
    implementation(libs.apache.commons.lang3)
    implementation(libs.log4j.core)
    implementation(libs.httpclient)
    implementation(libs.google.gson)
    implementation(libs.toml4j)
    implementation(libs.okhttp3)
    implementation(libs.commons.codec)
    implementation(libs.jjwt)
    implementation(libs.reactor.core)
    implementation(libs.slf4j.log4j12)
    implementation(libs.javax.jaxb.api)

    api(libs.lombok)
    api(libs.servlet.api)

    // Lombok should be used by all sub-projects to reduce Java verbosity
    annotationProcessor(libs.lombok)

    // From jitpack.io
    implementation(libs.java.stellar.sdk)

    // ***************************
    // Test dependencies
    // ***************************
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.junit5)
    testImplementation(libs.kotlin.mockk)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testAnnotationProcessor(libs.lombok)
}

tasks.test {
    useJUnitPlatform()
}
