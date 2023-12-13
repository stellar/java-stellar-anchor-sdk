import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.util.NetUtil
import org.stellar.anchor.util.Sep1Helper

internal class Sep1HelperTest {

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `readToml throws InvalidConfigException when fetching instance metadata instead of TOML`() {
    val mockUrl = "http://169.254.169.254/latest/meta-data/local-hostname"
    val instanceMetadataContent =
      """
        hostname: ec2-instance-hostname
        ami-id: ami-1234567890abcdef0
        instance-id: i-1234567890abcdef0
        instance-type: t2.micro
        """
        .trimIndent()
    mockkStatic(NetUtil::class)
    every { NetUtil.fetch(mockUrl) } returns instanceMetadataContent

    var exceptionThrown = false
    try {
      Sep1Helper.readToml(mockUrl)
    } catch (e: InvalidConfigException) {
      exceptionThrown = true
    }
    assertTrue(exceptionThrown, "InvalidConfigException was expected but not thrown")
  }
}
