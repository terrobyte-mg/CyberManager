package com.terrobytes.cybermanaver2.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Talks FTP to a Mikrotik router (built-in FTP server on port 21).
 *
 * We need this alongside [MikrotikRawClient] because the RouterOS binary API
 * (port 8728) isn't meant for file transfer - uploading the .rsc scripts used
 * by `run-after-reset`, and downloading a copy of a `/system backup save`
 * file so it doesn't live only on the router, both go through FTP.
 *
 * RouterOS' FTP server can be picky behind NAT, so passive mode is forced.
 */
class MikrotikFtpClient(
    private val host: String,
    private val username: String,
    private val password: String,
    private val port: Int = 21,
) {
    private val client = FTPClient()

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            client.connect(host, port)
            if (!FTPReply.isPositiveCompletion(client.replyCode)) {
                client.disconnect()
                return@withContext false
            }

            if (!client.login(username, password)) {
                client.disconnect()
                return@withContext false
            }

            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Uploads a text script (e.g. an .rsc file for run-after-reset) as UTF-8. */
    suspend fun uploadScript(fileName: String, content: String): Boolean =
        uploadBytes(fileName, content.toByteArray(Charsets.UTF_8))

    suspend fun uploadBytes(fileName: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            ByteArrayInputStream(bytes).use { input ->
                client.storeFile(fileName, input)
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Downloads a file from the router (e.g. a `.backup` produced by /system backup save). */
    suspend fun downloadFile(fileName: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val output = ByteArrayOutputStream()
            val ok = output.use { client.retrieveFile(fileName, it) }
            if (ok) output.toByteArray() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.deleteFile(fileName)
        } catch (_: Exception) {
            false
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            if (client.isConnected) {
                client.logout()
                client.disconnect()
            }
        } catch (_: Exception) {
        }
    }
}