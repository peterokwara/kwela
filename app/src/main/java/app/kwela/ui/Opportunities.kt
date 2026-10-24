package app.kwela.ui

data class JobOpportunity(val title: String, val company: String, val location: String, val summary: String) {
    val discussPrompt: String
        get() = "I'd like to talk about the $title role at $company in $location. Help me decide if it fits me and what to put in my CV."
}

/** Placeholder listings until a real jobs source is connected. */
val sampleOpportunities = listOf(
    JobOpportunity("Graduate Trainee, Data", "Example Bank", "Nairobi", "Entry-level analyst programme for recent graduates who are comfortable with spreadsheets."),
    JobOpportunity("Customer Support Agent", "Example Telecom", "Remote, Kenya", "Answer customer questions by phone and chat. Shift work, training provided."),
    JobOpportunity("Junior Accountant", "Example Logistics", "Mombasa", "Support the finance team with invoices and monthly reports. CPA part one preferred."),
    JobOpportunity("Marketing Intern", "Example Studio", "Kisumu", "Three-month internship helping with social media and local events.")
)
