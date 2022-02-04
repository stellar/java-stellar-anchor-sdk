plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

dependencies {
    compileOnly(libs.servlet.api)

    compileOnly(libs.slf4j.api)
    api(libs.lombok)

    implementation(libs.apache.commons.lang3)
    implementation(libs.log4j.core)
    implementation(libs.httpclient)
    implementation(libs.google.gson)
    implementation(libs.toml4j)
    implementation(libs.okhttp3)
    implementation(libs.commons.codec)
    implementation(libs.jjwt)
    implementation(libs.reactor.core)
    implementation(libs.javax.jaxb.api)
    implementation(libs.java.stellar.sdk)

    // Lombok should be used by all sub-projects to reduce Java verbosity
    annotationProcessor(libs.lombok)

    testImplementation(libs.servlet.api)
    testImplementation(libs.slf4j.api)
}
