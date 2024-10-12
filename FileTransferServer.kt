import java.io.*
import java.net.*

class FileTransferServer(private val port: Int) {

    fun start() {
        val serverSocket = ServerSocket(port)
        println("Server started on port $port")

        while (true) {
            val socket = serverSocket.accept()
            println("Client connected: ${socket.inetAddress.hostAddress}")
            handleClient(socket)
        }
    }

    private fun handleClient(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = DataOutputStream(socket.getOutputStream())

        thread {
            try {
                while (true) {
                    val command = input.readLine() ?: break
                    when {
                        command.startsWith("UPLOAD") -> {
                            val fileName = command.substringAfter(" ")
                            receiveFile(fileName, input)
                        }
                        command.startsWith("DOWNLOAD") -> {
                            val fileName = command.substringAfter(" ")
                            sendFile(fileName, output)
                        }
                        else -> println("Unknown command: $command")
                    }
                }
            } finally {
                socket.close()
                println("Client disconnected: ${socket.inetAddress.hostAddress}")
            }
        }
    }

    private fun receiveFile(fileName: String, input: BufferedReader) {
        val file = File("server_files/$fileName")
        file.parentFile.mkdirs() // Ensure directory exists
        FileOutputStream(file).use { fos ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.ready()) {
                bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                fos.write(buffer, 0, bytesRead)
            }
            println("Received file: ${file.absolutePath}")
        }
    }

    private fun sendFile(fileName: String, output: DataOutputStream) {
        val file = File("server_files/$fileName")
        if (file.exists()) {
            output.writeBytes("OK\n")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            println("Sent file: ${file.absolutePath}")
        } else {
            output.writeBytes("ERROR: File not found\n")
        }
    }
}

fun main() {
    val server = FileTransferServer(port = 12345)
    server.start()
}