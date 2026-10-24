package app.kwela.ui.theme

import app.kwela.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontVariation.weight
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalTextApi::class)
private val notoSans = FontFamily(
    Font(R.font.noto_sans_variable, FontWeight.Thin, variationSettings = FontVariation.Settings(weight(100))),
    Font(R.font.noto_sans_variable, FontWeight.ExtraLight, variationSettings = FontVariation.Settings(weight(200))),
    Font(R.font.noto_sans_variable, FontWeight.Light, variationSettings = FontVariation.Settings(weight(300))),
    Font(R.font.noto_sans_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(weight(400))),
    Font(R.font.noto_sans_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(weight(500))),
    Font(R.font.noto_sans_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(weight(600))),
    Font(R.font.noto_sans_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(weight(700))),
    Font(R.font.noto_sans_variable, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(weight(800))),
    Font(R.font.noto_sans_variable, FontWeight.Black, variationSettings = FontVariation.Settings(weight(900)))
)

private fun TextStyle.withNotoSans() = copy(fontFamily = notoSans)

private val defaultTypography = Typography()

val AppTypography = Typography(
    displayLarge = defaultTypography.displayLarge.withNotoSans(),
    displayMedium = defaultTypography.displayMedium.withNotoSans(),
    displaySmall = defaultTypography.displaySmall.withNotoSans(),
    headlineLarge = defaultTypography.headlineLarge.withNotoSans(),
    headlineMedium = defaultTypography.headlineMedium.withNotoSans(),
    headlineSmall = defaultTypography.headlineSmall.withNotoSans(),
    titleLarge = defaultTypography.titleLarge.withNotoSans(),
    titleMedium = defaultTypography.titleMedium.withNotoSans(),
    titleSmall = defaultTypography.titleSmall.withNotoSans(),
    bodyLarge = defaultTypography.bodyLarge.withNotoSans(),
    bodyMedium = defaultTypography.bodyMedium.withNotoSans(),
    bodySmall = defaultTypography.bodySmall.withNotoSans(),
    labelLarge = defaultTypography.labelLarge.withNotoSans(),
    labelMedium = defaultTypography.labelMedium.withNotoSans(),
    labelSmall = defaultTypography.labelSmall.withNotoSans()
)
