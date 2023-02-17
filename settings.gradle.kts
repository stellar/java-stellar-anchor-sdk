rootProject.name = "java-stellar-anchor-sdk"

/** APIs and Schemas */
include("api-schema")

/** SDK */
include("core")

/** Anchor Platform */
include("platform")

/** Anchor Reference Server */
include("anchor-reference-server")
include("kotlin-reference-server")

/** Integration tests */
include("integration-tests")

/** E2E tests */
include("end-to-end-tests")

/** Service runners */
include("service-runner")
