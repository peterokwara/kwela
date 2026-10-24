package app.kwela.ui

data class ChatSuggestion(val label: String, val prompt: String)

val chatSuggestions = listOf(
    ChatSuggestion("Create a CV", "I need help creating a CV. Please ask me for the details you need."),
    ChatSuggestion("Improve my CV", "I want to improve my CV. I will upload it now."),
    ChatSuggestion(
        "Update my CV for a job",
        "I want to update my CV for a job. I will share the job link and my CV."
    ),
    ChatSuggestion("Write a cover letter", "Help me write a cover letter. I will share the job details.")
)
