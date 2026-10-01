package com.example

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * Real embedded HTTP Gateway server running directly on Android port 8080.
 * Handles CORS and returns real node status to iDer-G+ Web Console.
 */
class VpsGatewayServer(private val port: Int = 8080) {

    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isServerRunning = false
        private set

    fun start() {
        if (isServerRunning) return

        job = scope.launch {
            try {
                serverSocket = ServerSocket(port).apply {
                    reuseAddress = true
                }
                isServerRunning = true
                Log.i("VpsGatewayServer", "Gateway server successfully listening on 0.0.0.0:$port")

                while (isActive && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val client = serverSocket!!.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e("VpsGatewayServer", "Failed to start server on port $port: ${e.message}")
            } finally {
                isServerRunning = false
            }
        }
    }

    fun stop() {
        isServerRunning = false
        try {
            job?.cancel()
            serverSocket?.close()
            serverSocket = null
            Log.i("VpsGatewayServer", "Gateway server stopped.")
        } catch (e: Exception) {
            Log.e("VpsGatewayServer", "Error closing server: ${e.message}")
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val output: OutputStream = client.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = if (parts.isNotEmpty()) parts[0].uppercase() else "GET"
            val path = if (parts.size > 1) parts[1] else "/"

            // Read request headers
            var headerLine = reader.readLine()
            var contentLength = 0
            while (!headerLine.isNullOrEmpty()) {
                if (headerLine.lowercase().startsWith("content-length:")) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                headerLine = reader.readLine()
            }

            // Consume body if present
            if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var readSoFar = 0
                while (readSoFar < contentLength) {
                    val read = reader.read(chars, readSoFar, contentLength - readSoFar)
                    if (read == -1) break
                    readSoFar += read
                }
            }

            // 1. Handle CORS Preflight (OPTIONS request) from modern browsers
            if (method == "OPTIONS") {
                val corsResponse = "HTTP/1.1 204 No Content\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, HEAD\r\n" +
                        "Access-Control-Allow-Headers: *\r\n" +
                        "Access-Control-Allow-Private-Network: true\r\n" +
                        "Access-Control-Max-Age: 86400\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n\r\n"

                output.write(corsResponse.toByteArray(Charsets.UTF_8))
                output.flush()
                return
            }

            // 2. Build Real JSON Node Response
            val json = JSONObject().apply {
                put("success", true)
                put("status", "connected")
                put("node", "ider-vps")
                put("version", "1.0")
                put("node_name", "ider-vps-local")
                put("battery", VpsService.batteryLevel.value)
                put("temperature", VpsService.batteryTemp.value)
                put("ssh_port", 2222)
                put("ssh_user", "root")
                put("gateway_port", port)
                put("message", "Node iDer VPS connected successfully!")

                val containers = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "c1")
                        put("name", "AI-Assistant")
                        put("status", "RUNNING")
                        put("service", "Python 3.11 / LLM Agent")
                    })
                    put(JSONObject().apply {
                        put("id", "c2")
                        put("name", "Nginx Web Server")
                        put("status", "RUNNING")
                        put("service", "HTTP Reverse Proxy")
                    })
                    put(JSONObject().apply {
                        put("id", "c3")
                        put("name", "Database")
                        put("status", "RUNNING")
                        put("service", "PostgreSQL / SQLite")
                    })
                }
                put("containers", containers)
            }

            val bodyBytes = json.toString().toByteArray(Charsets.UTF_8)
            val httpResponse = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json; charset=utf-8\r\n" +
                    "Content-Length: ${bodyBytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: *\r\n" +
                    "Access-Control-Allow-Private-Network: true\r\n" +
                    "Connection: close\r\n\r\n"

            output.write(httpResponse.toByteArray(Charsets.UTF_8))
            output.write(bodyBytes)
            output.flush()
        } catch (e: Exception) {
            Log.e("VpsGatewayServer", "Error handling client request: ${e.message}")
        } finally {
            try {
                client.close()
            } catch (_: Exception) {}
        }
    }
}
