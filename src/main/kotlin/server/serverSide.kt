package server

import RecievedMassage.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.net.ServerSocket
import java.net.Socket

class serverSide {

    //запуск сервера, установка порта
    fun serv() {
        val serverSocket = ServerSocket(8080)
        println("Сервер слушает на порту 8080")

        while (true) { // задаем цикл для подключений
            val clientSocket = serverSocket.accept()
            println("Новое подключение")
            Thread { handlerClient(clientSocket) }.start() // запускаем через поток, чтобы ничего не блокировалось
        }
    }

    fun parseJSON(json: String): Message {
        // Проверяем, что в JSON содержатся только "sender" и "text"
        if (!json.contains("\"sender\"") || !json.contains("\"text\"")) {
            throw IllegalArgumentException("Неверный формат JSON: должны присутствовать только поля 'sender' и 'text'")
        }

        val senderRegex = """"sender"\s*:\s*"([^"]*)"""".toRegex() //создаем регулярное выражение
        // поиск соответствия регулярному выражению
        val senderMatch = senderRegex.find(json) ?: throw IllegalArgumentException("Что-то не так с именем пользователя")
        val sender = senderMatch.groupValues[1] //извлечение захваченных групп

        val textRegex = """"text"\s*:\s*"([^"]*)"""".toRegex()
        val textMatch = textRegex.find(json) ?: throw IllegalArgumentException("Что-то не так с текстом")
        val text = textMatch.groupValues[1]

        // Проверяем, что нет других полей
        val additionalFieldsRegex = """(?:"sender"|"text"|[\s,]?"[^"]*"\s*:\s*"[^"]*")+""".toRegex()
        val additionalFields = additionalFieldsRegex.findAll(json)
            .map { it.value.trim() }
            .filter { it.isNotBlank() && !it.startsWith("\"sender\"") && !it.startsWith("\"text\"") }
            .toList()

        if (additionalFields.isNotEmpty()) {
            throw IllegalArgumentException("Неверный формат JSON: присутствуют дополнительные поля в запросе")
        }

        return Message(sender, text)
    }
    // функция для отправки ответа сервера
    fun sendResponse(output: OutputStream, status: String, body: String) {
        val httpResponse = "HTTP/1.1 $status\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${body.toByteArray().size}\r\n" +
                "\r\n" +
                body

        output.write(httpResponse.toByteArray())
        output.flush() // очистка потока вывода
    }
    // основная работа с сокетом
    fun handlerClient(clientSocket: Socket) {
        var getOut: OutputStream? = null
        var getIn: BufferedReader? = null

        try {
            getOut = clientSocket.getOutputStream() // запись в выходной поток
            getIn = BufferedReader(InputStreamReader(clientSocket.getInputStream())) // считывание данных от клиента

            // чтение HTTP запроса
            val requestLine = getIn.readLine()
            if (requestLine == null || requestLine.isEmpty()) {
                sendResponse(getOut, "400 Bad Request", "Пустой запрос")
                return
            }

            println("Получен запрос: $requestLine")

            val requestParts = requestLine.split(" ") // Разделение на метод, путь и версию
            if (requestParts.size != 3) {
                sendResponse(getOut, "400 Bad Request", "Неверный формат")
                return
            }

            val method = requestParts[0]
            val path = requestParts[1]

            var contentType: String? = null
            var contentLength = 0

            //Чтение заголовков (headers) и проверка типа
            while (true) {
                val line = getIn.readLine()
                if (line.isNullOrBlank()) break
                println("Заголовок: $line")
                if (line.startsWith("Content-Type:", true)) {
                    contentType = line.split(":")[1].trim() //сохранение типа
                }
                if (line.startsWith("Content-Length:", true)) {
                    contentLength = line.split(":")[1].trim().toInt()
                }
            }

            println("Content-Type: $contentType, Content-Length: $contentLength")

            if (contentType != "application/json") {
                sendResponse(getOut, "415 Unsupported Media Type", "Подходит только JSON")
                return
            }

            val body = CharArray(contentLength)
            getIn.read(body, 0, contentLength) // считывание символов
            val bodyString = String(body)

            println("Получено тело запроса: $bodyString")

            when (method) {
                "POST" -> handlePost(getOut, path, bodyString)
                else -> sendResponse(getOut, "405 Method Not Allowed", "Неподдерживаемый HTTP метод")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                getIn?.close() // закрытие сокета
                getOut?.close()
                clientSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // создание обращения к серверу
    fun handlePost(output: OutputStream, path: String, body: String) {

        when (path) {
            "/post" -> {
                try {
                    val data = parseJSON(body) // парсим в нужный объект
                    sendResponse(output, "200 OK", "Данные получены: $data")
                } catch (e: IllegalArgumentException) {
                    sendResponse(output, "400 Bad Request", "Неверный формат: ${e.message}")
                }
            }
            else -> sendResponse(output, "404 Not Found", "Проверьте правильность написания запроса")
        }
    }
}