rootProject.name = "java-stellar-anchor-sdk"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            alias("apache.commons.lang3").to("org.apache.commons:commons-lang3:3.12.0")
            alias("log4j.core").to("org.apache.logging.log4j:log4j-core:2.17.1")
            alias("httpclient").to("org.apache.httpcomponents:httpclient:4.5.13")
            alias("google.gson").to("com.google.code.gson:gson:2.8.9")
            alias("toml4j").to("com.moandjiezana.toml:toml4j:0.7.2")
            alias("okhttp3").to("com.squareup.okhttp3:okhttp:4.9.3")
            alias("commons.codec").to("commons-codec:commons-codec:1.15")
            alias("jjwt").to("io.jsonwebtoken:jjwt:0.9.1")
            alias("reactor.core").to("io.projectreactor:reactor-core:3.4.14")
            alias("slf4j.log4j12").to("org.slf4j:slf4j-log4j12:1.7.33")
            alias("lombok").to("org.projectlombok:lombok:1.18.22")
            alias("servlet.api").to("javax.servlet:servlet-api:2.5")
            alias("okhttp3.mockserver").to("com.squareup.okhttp3:mockwebserver:4.9.3")
            alias("reactor.netty").to("io.projectreactor.netty:reactor-netty:1.0.15")
            alias("java.stellar.sdk").to("com.github.stellar:java-stellar-sdk:0.29.0")
            alias("jsonallert").to("org.skyscreamer:jsonassert:1.5.0")

            alias("kotlin.mockk").to("io.mockk:mockk:1.12.2")
            alias("kotlin.stdlib").to("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
            alias("kotlin.junit5").to("org.jetbrains.kotlin:kotlin-test-junit5:1.6.10")

            alias("javax.jaxb.api").to("javax.xml.bind:jaxb-api:2.3.1")
            alias("junit5.api").to("org.junit.jupiter:junit-jupiter-api:5.8.2")
            alias("junit5.engine").to("org.junit.jupiter:junit-jupiter-engine:5.8.2")
            alias("junit5.params").to("org.junit.jupiter:junit-jupiter-params:5.8.2")
            alias("jsonassert").to("org.skyscreamer:jsonassert:1.5.0")
        }
    }
}


include("core")
include("payment-circle")