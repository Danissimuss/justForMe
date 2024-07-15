package RecievedMassage

import java.lang.IllegalArgumentException

data class Message(val sender: String, val text: String) {
    val regex = Regex("[^\\p{IsAlphabetic}\\p{IsDigit} ]") // разрешает буквы (латиница и кириллица), цифры и пробелы

    init {
        if (regex.containsMatchIn(sender)) {
            throw IllegalArgumentException("Введите другое имя: имя может содержать только буквы, цифры и пробелы")
        }
    }
}