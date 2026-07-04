package com.terrobytes.cybermanaver2.network

import java.net.Socket

class MikrotikRawClient(
    val host: String,
    port: Int = 8728
) {

    private val socket = Socket(host, port)
    private val out = socket.getOutputStream()
    private val input = socket.getInputStream()

    fun testPort(): Boolean {
        return try {
            val socket = Socket(host, 8728)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun writeWord(word: String) {
        val bytes = word.toByteArray()
        out.write(bytes.size)
        out.write(bytes)
    }

    fun writeSentence(words: List<String>) {
        words.forEach { writeWord(it) }
        writeWord("")
    }

    fun read(): String {
        val sb = StringBuilder()

        while (true) {
            val len = input.read()
            if (len == -1) break

            val bytes = ByteArray(len)
            input.read(bytes)

            val word = String(bytes)
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

    fun close() {
        try {
            socket.close()
        } catch (_: Exception) {}
    }
}