package org.stellar.reference

// Used in ServiceRunner
@JvmOverloads // Annotation required to call from Java with optional argument
fun start(waitServer: Boolean = false) = startServer(waitServer)
fun stop() = stopServer()

class KotlinTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello, world!")
        }
    }
}