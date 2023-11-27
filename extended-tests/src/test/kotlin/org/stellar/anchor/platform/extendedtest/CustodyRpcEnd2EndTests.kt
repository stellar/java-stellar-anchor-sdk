package org.stellar.anchor.platform.extendedtest

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.stellar.anchor.platform.e2etest.Sep24End2EndTests
import org.stellar.anchor.platform.e2etest.Sep31End2EndTests
import org.stellar.anchor.platform.e2etest.Sep6End2EndTest

// use TEST_PROFILE_NAME = "rpc"
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class CustodyRpcSep31End2EndTests : Sep31End2EndTests()

// use TEST_PROFILE_NAME = "rpc"
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class CustodyRpcSep24End2EndTests : Sep24End2EndTests()

// use TEST_PROFILE_NAME = "rpc"
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class CustodyRpcSep6End2EndTests : Sep6End2EndTest()
