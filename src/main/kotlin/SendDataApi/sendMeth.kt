package SendDataApi

import RecievedMassage.Message
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class sendMeth {

    // отправка метода POST
    fun sendToS(sender: Message) {

        //тело запроса и подстановка через созданный класс
        val requestBody = """
            {
                "sender": "${sender.sender}",
                "text": "${sender.text}"
            }
        """.trimIndent() //обработка строк

        // связь клиента
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/post"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .header("Content-Type", "application/json")
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        println("Ответ сервера: ${response.statusCode()} ${response.body()}")
    }
}