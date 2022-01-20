package org.stellar.anchor.util

import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileOutputStream
import java.io.OutputStream

internal class StellarKeyToolTest {
    companion object {
        const val TEST_PRIVATE_KEY = "SCIIA2T7GNITTZFF43P5SWAZFGV3CTMTXVCVMXPLI2ZLHADXYKOKXNU2"
    }

    @BeforeEach
    fun setup() {
        mockkStatic(StellarKeyTool::class)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun testMain() {
        every { StellarKeyTool.getOutputStream(any()) } returns OutputStream.nullOutputStream()

        StellarKeyTool.main(arrayOf("-k", "-o", "/output_file", "-s", TEST_PRIVATE_KEY))

        verify {
            StellarKeyTool.writeKeyPair(any(), "/output_file")
        }

        StellarKeyTool.main(arrayOf("-o", "/output_file", "-s", TEST_PRIVATE_KEY))
        verify {
            StellarKeyTool.writeKeyPair(any(), "/output_file")
        }

        unmockkAll()
        assertThrows<NullPointerException> {
            StellarKeyTool.main(arrayOf("-k", "-s", TEST_PRIVATE_KEY))
        }
    }

    @Test
    fun testGetOutputStream() {
        mockkConstructor(FileOutputStream::class)
        assertThrows<NullPointerException> {
            StellarKeyTool.getOutputStream(null)
        }
    }

    @Test
    fun testBadArgs() {
        every { StellarKeyTool.writeKeyPair(any(), any()) } answers {}

        StellarKeyTool.main(arrayOf("-k", "-o", "/output_file", "-s"))

        verify (exactly = 0) {
            StellarKeyTool.writeKeyPair(any(), any())
        }
    }

    @Test
    fun testKeyPair() {
        assertNotNull(StellarKeyTool.getKeyPair(null))
        assertNotNull(StellarKeyTool.getKeyPair(TEST_PRIVATE_KEY))
        assertThrows<IllegalArgumentException> {
            StellarKeyTool.getKeyPair("S123")
        }
    }
}
