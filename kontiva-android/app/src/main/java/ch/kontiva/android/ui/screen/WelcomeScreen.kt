package ch.kontiva.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.theme.KontivaBrand
import ch.kontiva.android.ui.theme.KontivaTheme

/** Onboarding welcome — 1:1 with the iOS welcome hero (wordmark, intro, cues, CTA). */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val accent = KontivaTheme.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KontivaTheme.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(KontivaTheme.spaceXxl))
        Spacer(Modifier.weight(0.6f))

        // Wordmark: "Kontiva" with the brand-red accent dot.
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = loc(L10nKey.appName),
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Text(
                text = ".",
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = KontivaBrand.SwissRed,
            )
        }

        Spacer(Modifier.height(KontivaTheme.spaceMd))
        Text(
            text = loc(L10nKey.onboardingIntroBody),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = KontivaTheme.spaceMd),
        )

        Spacer(Modifier.height(KontivaTheme.spaceXl))
        Surface(
            shape = RoundedCornerShape(KontivaTheme.radiusCard),
            color = colors.cardSurface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(KontivaTheme.spaceMd),
                verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
            ) {
                FeatureRow(Icons.Rounded.CloudOff, colors.chartFixed, loc(L10nKey.onboardingFeaturePrivate))
                FeatureRow(Icons.Rounded.Lock, colors.swissRed, loc(L10nKey.onboardingFeatureSecure))
                FeatureRow(Icons.Rounded.Payments, colors.positive, loc(L10nKey.onboardingFeatureMoney))
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(KontivaTheme.radiusControl),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
        ) {
            Text(loc(L10nKey.onboardingStart), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(KontivaTheme.spaceMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(KontivaTheme.spaceXxs))
            Text("AES-256-GCM", color = colors.textTertiary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(KontivaTheme.spaceMd))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(KontivaTheme.spaceSm))
        Text(text, color = KontivaTheme.colors.textPrimary, fontSize = 15.sp)
    }
}
