package app.kwela.domain

/** A name to show for the person, from what Kwela knows: the name Google gave, or the start of their email. */
object DisplayName {
    fun of(knownName: String?, email: String?): String = knownName?.trim()?.takeIf { it.isNotEmpty() } ?: fromEmail(email)

    fun fromEmail(email: String?): String {
        if (email.isNullOrBlank()) return "Kwela user"
        val words = email.substringBefore("@").substringBefore("+")
            .split('.', '_', '-')
            .map { word -> word.takeWhile { it.isLetter() } }
            .filter { it.length >= 2 }
        if (words.isEmpty()) return email
        return words.joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
    }

    fun initial(name: String): String = name.trim().firstOrNull { it.isLetter() }?.uppercase() ?: "K"
}
