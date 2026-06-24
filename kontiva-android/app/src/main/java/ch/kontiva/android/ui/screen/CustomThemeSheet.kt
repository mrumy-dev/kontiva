package ch.kontiva.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.ThemeStyle
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme
import ch.kontiva.android.ui.theme.parseHexColor

private fun Color.toHex6(): String = String.format("%06X", toArgb() and 0xFFFFFF)
private fun hueOf(c: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    return hsv[0] / 360f
}

/** Build-your-own theme: pick any colour(s) + a style, with a live preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeSheet(vm: KontivaViewModel, onDismiss: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val seed = parseHexColor(vm.settings.customAccent) ?: KontivaTheme.accent

    var h1 by remember { mutableFloatStateOf(hueOf(seed)) }
    var s1 by remember { mutableFloatStateOf(0.78f) }
    var v1 by remember { mutableFloatStateOf(0.85f) }
    var style by remember { mutableStateOf(vm.settings.themeStyle.takeIf { it != ThemeStyle.SOLID } ?: ThemeStyle.GRADIENT) }
    var h2 by remember { mutableFloatStateOf((hueOf(seed) + 0.1f) % 1f) }
    var s2 by remember { mutableFloatStateOf(0.78f) }
    var v2 by remember { mutableFloatStateOf(0.85f) }

    val primary = Color.hsv(h1 * 360f, s1, v1)
    val secondary = Color.hsv(h2 * 360f, s2, v2)
    val preview = when (style) {
        ThemeStyle.SOLID -> Brush.horizontalGradient(listOf(primary, primary))
        ThemeStyle.GRADIENT -> Brush.horizontalGradient(listOf(primary, lerp(primary, Color.White, 0.5f)))
        ThemeStyle.DUAL -> Brush.horizontalGradient(listOf(primary, secondary))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.cardSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = KontivaTheme.spaceLg)
                .padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.themeCustom), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Box(Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(KontivaTheme.radiusCard)).background(preview))

            // Style choices (pick by look).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KontivaTheme.spaceSm)) {
                StyleDot(Brush.linearGradient(listOf(primary, primary)), style == ThemeStyle.SOLID) { style = ThemeStyle.SOLID }
                StyleDot(Brush.linearGradient(listOf(primary, lerp(primary, Color.White, 0.5f))), style == ThemeStyle.GRADIENT) { style = ThemeStyle.GRADIENT }
                StyleDot(Brush.linearGradient(listOf(primary, secondary)), style == ThemeStyle.DUAL) { style = ThemeStyle.DUAL }
            }

            HsvSliders(h1, s1, v1, { h1 = it }, { s1 = it }, { v1 = it })
            if (style == ThemeStyle.DUAL) {
                HorizontalDivider(color = colors.softBorder.copy(alpha = 0.4f))
                HsvSliders(h2, s2, v2, { h2 = it }, { s2 = it }, { v2 = it })
            }

            Button(
                onClick = {
                    vm.applyCustomTheme(primary.toHex6(), style, if (style == ThemeStyle.DUAL) secondary.toHex6() else null)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun StyleDot(brush: Brush, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(44.dp).clip(CircleShape).background(KontivaTheme.colors.softBorder))
        Box(Modifier.size(34.dp).clip(CircleShape).background(brush))
    }
}

@Composable
private fun HsvSliders(h: Float, s: Float, v: Float, onH: (Float) -> Unit, onS: (Float) -> Unit, onV: (Float) -> Unit) {
    val rainbow = Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f, 1f, 1f) })
    val satTrack = Brush.horizontalGradient(listOf(Color.hsv(h * 360f, 0f, v), Color.hsv(h * 360f, 1f, v)))
    val valTrack = Brush.horizontalGradient(listOf(Color.Black, Color.hsv(h * 360f, s, 1f)))
    Column(verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceXs)) {
        TrackSlider(h, rainbow, onH)
        TrackSlider(s, satTrack, onS)
        TrackSlider(v, valTrack, onV)
    }
}

/** A slider whose track shows a colour gradient (rainbow / saturation / brightness). */
@Composable
private fun TrackSlider(value: Float, track: Brush, onValueChange: (Float) -> Unit) {
    Box(contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(12.dp).padding(horizontal = 10.dp).clip(CircleShape).background(track))
        Slider(
            value = value, onValueChange = onValueChange, valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent,
                thumbColor = Color.White,
            ),
        )
    }
}
