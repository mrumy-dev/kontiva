package ch.kontiva.android.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import ch.kontiva.android.core.AccentTheme
import ch.kontiva.android.core.AppAppearance

/** Fixed brand / danger constants (1:1 with iOS KontivaTheme). */
object KontivaBrand {
    val SwissRed = Color(0xFFE11D2E)
    val Charcoal = Color(0xFF121A22)
    val OffWhite = Color(0xFFF6F7F8)
}

/** The full semantic palette for one appearance (light or dark). */
data class KontivaColors(
    val pageBackground: Color,
    val cardSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val softBorder: Color,
    val positive: Color,
    val warning: Color,
    val chartFixed: Color,
    val chartVariable: Color,
    val chartBills: Color,
    val chartSavings: Color,
    val chartAvailable: Color,
    val swissRed: Color = KontivaBrand.SwissRed,
)

private val LightColors = KontivaColors(
    pageBackground = Color(0xFFF8F7F4),
    cardSurface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF121A22),
    textSecondary = Color(0xFF5A6672),
    textTertiary = Color(0xFF8A95A0),
    softBorder = Color(0xFFDCE1E5),
    positive = Color(0xFF1F7A4D),
    warning = Color(0xFFB26A00),
    chartFixed = Color(0xFF3E5C76),
    chartVariable = Color(0xFF8AA0B0),
    chartBills = KontivaBrand.SwissRed,
    chartSavings = Color(0xFF6A4C93),
    chartAvailable = Color(0xFF1F7A4D),
)

private val DarkColors = KontivaColors(
    pageBackground = Color(0xFF0F151B),
    cardSurface = Color(0xFF1A222B),
    textPrimary = Color(0xFFF2F4F6),
    textSecondary = Color(0xFF9BA7B3),
    textTertiary = Color(0xFF707C88),
    softBorder = Color(0xFF2A333D),
    positive = Color(0xFF44C088),
    warning = Color(0xFFE0A042),
    chartFixed = Color(0xFF6E94B4),
    chartVariable = Color(0xFFAEC0CD),
    chartBills = KontivaBrand.SwissRed,
    chartSavings = Color(0xFF9B7FC4),
    chartAvailable = Color(0xFF44C088),
)

/** Accent colour for each theme, tuned per appearance (1:1 with iOS AccentTheme.color). */
fun AccentTheme.color(dark: Boolean): Color = when (this) {
    AccentTheme.SWISS_RED -> if (dark) Color(0xFFF24A57) else Color(0xFFE11D2E)
    AccentTheme.ORANGE -> if (dark) Color(0xFFF2894E) else Color(0xFFE2622A)
    AccentTheme.SAND -> if (dark) Color(0xFFCBA06A) else Color(0xFFA87A3D)
    AccentTheme.GREEN -> if (dark) Color(0xFF53C485) else Color(0xFF2E8B57)
    AccentTheme.TEAL -> if (dark) Color(0xFF3FBEBE) else Color(0xFF0E8C8C)
    AccentTheme.BLUE -> if (dark) Color(0xFF6098F0) else Color(0xFF2563EB)
    AccentTheme.PURPLE -> if (dark) Color(0xFFB07AD6) else Color(0xFF7E3AA8)
    AccentTheme.PINK -> if (dark) Color(0xFFF06CB0) else Color(0xFFD6337F)
}

val LocalKontivaColors = staticCompositionLocalOf { LightColors }
val LocalAccentColor = staticCompositionLocalOf { KontivaBrand.SwissRed }

/** Design tokens + ergonomic accessors (mirrors iOS KontivaTheme.Space / Radius). */
object KontivaTheme {
    val spaceXxs = 4.dp
    val spaceXs = 8.dp
    val spaceSm = 12.dp
    val spaceMd = 16.dp
    val spaceLg = 24.dp
    val spaceXl = 32.dp
    val spaceXxl = 48.dp

    val radiusCard = 16.dp
    val radiusControl = 10.dp
    val radiusTile = 14.dp

    val colors: KontivaColors
        @Composable get() = LocalKontivaColors.current
    val accent: Color
        @Composable get() = LocalAccentColor.current

    /** Accent-tinted page background — a soft vertical wash so the whole app (not just
     *  the symbols) takes on the chosen colour. Reads the live (animated) accent. */
    val pageBrush: Brush
        @Composable get() {
            val bg = LocalKontivaColors.current.pageBackground
            val a = LocalAccentColor.current
            return Brush.verticalGradient(
                listOf(
                    a.copy(alpha = 0.18f).compositeOver(bg),
                    bg,
                    a.copy(alpha = 0.06f).compositeOver(bg),
                ),
            )
        }
}

private val KontivaTypography = Typography()

/** Wraps content in Material3 + the Kontiva palette/accent for the chosen appearance. */
@Composable
fun KontivaTheme(
    appearance: AppAppearance = AppAppearance.SYSTEM,
    accent: AccentTheme = AccentTheme.SWISS_RED,
    content: @Composable () -> Unit,
) {
    val dark = when (appearance) {
        AppAppearance.SYSTEM -> isSystemInDarkTheme()
        AppAppearance.LIGHT -> false
        AppAppearance.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    // Smoothly morph the accent (and thus the whole-app tint) when the theme changes.
    val accentColor by animateColorAsState(accent.color(dark), animationSpec = tween(450), label = "accent")
    val scheme = if (dark) {
        darkColorScheme(
            primary = accentColor,
            background = colors.pageBackground,
            surface = colors.cardSurface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            error = colors.swissRed,
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            background = colors.pageBackground,
            surface = colors.cardSurface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            error = colors.swissRed,
        )
    }
    CompositionLocalProvider(
        LocalKontivaColors provides colors,
        LocalAccentColor provides accentColor,
    ) {
        MaterialTheme(colorScheme = scheme, typography = KontivaTypography, content = content)
    }
}
