package com.Privvideo.app

import android.content.Context
import android.os.Build
import android.util.Log
import io.github.muntashirakon.adb.AdbPairingRequiredException
import io.github.muntashirakon.adb.AdbStream
import io.github.muntashirakon.adb.LocalServices
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets

/** ADB Wireless Debugging/TCP: pareamento, conexão, shell e desconexão. */
class AdbManager(private val context: Context) {
    private var manager: AdbConnectionManager? = null
    private var shell: AdbStream? = null
    @Volatile var lastError: String = ""
        private set

    private fun getManager(): AdbConnectionManager =
        manager ?: AdbConnectionManager.getInstance(context).also { manager = it }

    fun pair(host: String, port: Int, code: String): Boolean {
        return try {
        lastError = ""
        require(host.isNotBlank() && port in 1..65535 && code.length in 4..8)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val endpoint = if (host == "localhost" || host == "127.0.0.1") localIpv4() else host

        getManager().pair(endpoint, port, code)
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName

            false
        }
    }

    fun connect(): Boolean {
        return try {
        lastError = ""
        val m = getManager()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (m.autoConnect(context, 5000)) return true
            } catch (_: AdbPairingRequiredException) {
                return false
            }
        }
        m.connect(5555)
        } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName

            false
        }
    }

    fun executeCommand(command: String): String? = try {
        require(command.isNotBlank())
        val stream = shell ?: getManager().openStream(LocalServices.SHELL).also { shell = it }
        stream.openOutputStream().use { out ->
            out.write((command + "\n\n").toByteArray(StandardCharsets.UTF_8))
            out.flush()
        }
        BufferedReader(InputStreamReader(stream.openInputStream())).use { reader ->
            buildString {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    append(line).append('\n')
                }
            }
        }
    } catch (e: Exception) {

        null
    }

    fun disconnect() {
        try {
            shell?.close()
            manager?.disconnect()
        } catch (_: Exception) {
        }
        shell = null
    }

    fun suggestedHost(): String = localIpv4()

    private fun localIpv4(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                if (!network.isUp || network.isLoopback) continue
                val addresses = network.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível descobrir o IP local", e)
        }
        return "127.0.0.1"
    }

    private companion object { const val TAG = "AdbManager" }
}
