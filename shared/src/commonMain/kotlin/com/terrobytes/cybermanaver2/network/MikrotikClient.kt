package com.terrobytes.cybermanaver2.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Speaks the RouterOS binary API protocol (TCP port 8728/8729).
 *
 * Words are length-prefixed, but the length itself uses a variable-length
 * encoding: the number of leading 1-bits in the first byte says how many
 * bytes make up the length field (1 to 5 bytes), letting short words (the
 * vast majority: "/login", "=name=admin"...) cost a single byte while still
 * allowing lengths up to ~4 billion.
 *
 *   0xxxxxxx                                      -> 7-bit length  (1 byte header)
 *   10xxxxxx xxxxxxxx                             -> 14-bit length (2 byte header)
 *   110xxxxx xxxxxxxx xxxxxxxx                    -> 21-bit length (3 byte header)
 *   1110xxxx xxxxxxxx xxxxxxxx xxxxxxxx           -> 28-bit length (4 byte header)
 *   11110000 xxxxxxxx xxxxxxxx xxxxxxxx xxxxxxxx  -> 32-bit length (5 byte header)
 */
class MikrotikRawClient(private val socket: Socket) {

    private val out: OutputStream = socket.getOutputStream()
    private val input: InputStream = socket.getInputStream()

    private fun writeLength(length: Int) {
        when {
            length < 0x80 -> {
                out.write(length)
            }
            length < 0x4000 -> {
                out.write(0x80 or (length shr 8 and 0x3F))
                out.write(length and 0xFF)
            }
            length < 0x200000 -> {
                out.write(0xC0 or (length shr 16 and 0x1F))
                out.write(length shr 8 and 0xFF)
                out.write(length and 0xFF)
            }
            length < 0x10000000 -> {
                out.write(0xE0 or (length shr 24 and 0x0F))
                out.write(length shr 16 and 0xFF)
                out.write(length shr 8 and 0xFF)
                out.write(length and 0xFF)
            }
            else -> {
                out.write(0xF0)
                out.write(length shr 24 and 0xFF)
                out.write(length shr 16 and 0xFF)
                out.write(length shr 8 and 0xFF)
                out.write(length and 0xFF)
            }
        }
    }

    private fun readLength(): Int {
        val c = input.read()
        if (c == -1) return -1

        return when {
            c and 0x80 == 0x00 -> c

            c and 0xC0 == 0x80 -> {
                val b2 = input.read()
                ((c and 0x3F) shl 8) or b2
            }

            c and 0xE0 == 0xC0 -> {
                val b2 = input.read()
                val b3 = input.read()
                ((c and 0x1F) shl 16) or (b2 shl 8) or b3
            }

            c and 0xF0 == 0xE0 -> {
                val b2 = input.read()
                val b3 = input.read()
                val b4 = input.read()
                ((c and 0x0F) shl 24) or (b2 shl 16) or (b3 shl 8) or b4
            }

            c == 0xF0 -> {
                val b2 = input.read()
                val b3 = input.read()
                val b4 = input.read()
                val b5 = input.read()
                (b2 shl 24) or (b3 shl 16) or (b4 shl 8) or b5
            }

            else -> throw IllegalStateException("Invalid RouterOS API length byte: $c")
        }
    }

    /** input.read(byteArray) can return fewer bytes than requested on a socket - loop until full. */
    private fun readFully(length: Int): ByteArray {
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(bytes, offset, length - offset)
            if (read == -1) throw EOFException("Connection closed while reading a word")
            offset += read
        }
        return bytes
    }

    fun writeWord(word: String) {
        val bytes = word.toByteArray(Charsets.UTF_8)
        writeLength(bytes.size)
        out.write(bytes)
    }

    fun writeSentence(words: List<String>) {
        words.forEach { writeWord(it) }
        writeWord("")
        out.flush()
    }

    fun read(): String {
        val sb = StringBuilder()
        while (true) {
            val len = readLength()
            if (len == -1) break
            val word = if (len == 0) "" else String(readFully(len), Charsets.UTF_8)
            sb.append(word).append("\n")
            if (word == "!done") break
        }
        return sb.toString()
    }

    fun login(user: String, pass: String): Boolean {
        return try {
            writeSentence(
                listOf(
                    "/login",
                    "=name=$user",
                    "=password=$pass"
                )
            )
            val response = read()
            response.contains("!done") && !response.contains("!trap")
        } catch (_: Exception) {
            false
        }
    }

    fun execute(cmd: String): String {
        writeSentence(listOf(cmd))
        return read()
    }

    /** For commands that need parameters, e.g. listOf("/export", "file=name"). */
    fun execute(words: List<String>): String {
        writeSentence(words)
        return read()
    }

    fun close() {
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}

/**
 * Opens a connection to a Mikrotik device's API port, bound to [networkTarget]
 * when known (see [openSocket]). Pass `networkTarget = null` for manual
 * connections where you don't have a scanned NetworkTarget on hand.
 */
suspend fun MikrotikRawClient(
    networkTarget: NetworkTarget?,
    host: String,
    port: Int = 8728,
): MikrotikRawClient = withContext(Dispatchers.IO) {
    MikrotikRawClient(openSocket(networkTarget, host, port))
}