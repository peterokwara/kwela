package app.kwela.domain

object DemoOtpVerifier {
    const val DISPLAYED_CODE = "123456"

    fun isValid(code: String): Boolean = code == DISPLAYED_CODE
}
