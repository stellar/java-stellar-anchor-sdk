import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.IOException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.stellar.anchor.LockStatic
import org.stellar.anchor.api.exception.InvalidConfigException
import org.stellar.anchor.util.NetUtil
import org.stellar.anchor.util.Sep1Helper

internal class Sep1HelperTest {

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  @LockStatic([NetUtil::class])
  fun `readToml returns TomlContent on successful fetch`() {
    val mockUrl = "http://example.com/stellar.toml"
    val validTomlContent =
      """
            FEDERATION_SERVER = "https://api.example.com/federation"
            AUTH_SERVER = "https://api.example.com/auth"
            TRANSFER_SERVER = "https://api.example.com/transfer"
            SIGNING_KEY = "GABCDEFGHJKLMNOPQRSTUVWXYZ1234567"
            """
        .trimIndent()
    mockkStatic(NetUtil::class)
    every { NetUtil.fetch(mockUrl) } returns validTomlContent

    val result = Sep1Helper.readToml(mockUrl)
    assertEquals("https://api.example.com/federation", result.getString("FEDERATION_SERVER"))
    assertEquals("https://api.example.com/auth", result.getString("AUTH_SERVER"))
    assertEquals("https://api.example.com/transfer", result.getString("TRANSFER_SERVER"))
    assertEquals("GABCDEFGHJKLMNOPQRSTUVWXYZ1234567", result.getString("SIGNING_KEY"))
  }

  @Test
  @LockStatic([NetUtil::class])
  fun `readToml throws IOException on network error with trycatch`() {
    val mockUrl = "http://example.com/stellar.toml"
    mockkStatic(NetUtil::class)
    every { NetUtil.fetch(mockUrl) } throws IOException("Network error")

    try {
      Sep1Helper.readToml(mockUrl)
      fail("IOException expected")
    } catch (e: IOException) {
      // Expected exception
    }
  }

  @Test
  @LockStatic([NetUtil::class])
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
