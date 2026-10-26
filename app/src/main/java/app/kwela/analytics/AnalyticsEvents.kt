package app.kwela.analytics

/**
 * Every event the phone sends. Names are fixed strings in the form category:object_action, in lowercase snake case.
 * Never build a name from a variable; put the variable in a property. The server sends sign-up, sign-in, delete, application and PDF events itself.
 */
object AnalyticsEvents {
    const val sessionStart = "app:session_start"
    const val googleButtonClick = "login_flow:google_button_click"
    const val emailButtonClick = "login_flow:email_button_click"
    const val messageSend = "chat:message_send"
    const val messageResend = "chat:message_resend"
    const val messageDelete = "chat:message_delete"
    const val fileOpen = "document:file_open"
    const val pdfDownload = "document:pdf_download"
    const val pdfShare = "document:pdf_share"

    val all = listOf(sessionStart, googleButtonClick, emailButtonClick, messageSend, messageResend, messageDelete, fileOpen, pdfDownload, pdfShare)
}
