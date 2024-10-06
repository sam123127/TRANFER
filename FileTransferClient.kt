import java.io.*
import java.net.*
import kotlin.concurrent.thread

class FileTransferClient(private val host: String, private val port: Int) {

    fun start() {
        val socket = Socket(host, port)
        println("Connected to server: $host:$port")
        val input = BufferedReader(InputStreamReader(System.`in`))
        val output = DataOutputStream(socket.getOutputStream())

        thread { receiveMessages(socket) }

        while (true) {
            print("Enter command (UPLOAD <filename> | DOWNLOAD <filename> | EXIT): ")
            val command = input.readLine() ?: break
            if (command == "EXIT") break
            output.writeBytes(command + "\n")
            when {
                command.startsWith("UPLOAD") -> {
                    val fileName = command.substringAfter(" ")
                    sendFile(fileName, output)
                }
                command.startsWith("DOWNLOAD") -> {
                    val fileName = command.substringAfter(" ")
                    receiveFile(fileName, socket.getInputStream())
                }
            }
        }
        socket.close()
        println("Disconnected from server.")
    }

    private fun sendFile(fileName: String, output: DataOutputStream) {
        val file = File(fileName)
        if (file.exists()) {
            output.writeBytes("UPLOAD $fileName\n")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
            println("Uploaded file: $fileName")
        } else {
            println("File not found: $fileName")
        }
    }

    private fun receiveFile(fileName: String, input: InputStream) {
        val response = BufferedReader(InputStreamReader(input)).readLine()
        if (response.startsWith("OK")) {
            val file = File("downloaded_$fileName")
            FileOutputStream(file).use { fos ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                }
            }
            println("Downloaded file: ${file.absolutePath}")
        } else {
            println(response)
        }
    }

    private fun receiveMessages(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        var message: String?
        while (true) {
            message = input.readLine() ?: break
            println(message)
        }
    }
}

fun main() {
    val client = FileTransferClient(host = "127.0.0.1", port = 12345)
    client.start()
}
