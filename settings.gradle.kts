rootProject.name = "java-stellar-anchor-sdk"

/** APIs and Schemas */
include("api-schema")

/** Test libraries */
include("test-lib")

/** SDK */
include("core")

/** Anchor Platform */
include("platform")

/** Reference Server */
include("kotlin-reference-server")
include("wallet-reference-server")

/** Integration tests */
include("integration-tests")

/** Service runners */
include("service-runner")
