import RecievedMassage.Message
import SendDataApi.sendMeth
import server.serverSide


fun main(){

    Thread { serverSide().serv() }.start() // безопасный запуск через поток

    Thread.sleep(1000) // ожидание сервера

    val message = Message("John Snow", "U know nothing")
    val sendMeth = sendMeth()
    sendMeth.sendToS(message)

}