package ch.kontiva.android.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.R
import ch.kontiva.android.ui.theme.KontivaBrand
import ch.kontiva.android.ui.theme.KontivaTheme
import kotlin.math.roundToInt

private const val PIN_LENGTH = 6

/**
 * Reusable PIN entry — wordmark, title, requirement note, 6 dots, and a numeric
 * keypad. Calls [onComplete] when 6 digits are entered. Bump [resetSignal] to clear
 * the field (e.g. set → confirm); bump [errorSignal] to shake + clear (mismatch /
 * wrong code). 1:1 in spirit with the iOS PinEntry.
 */
@Composable
fun PinEntry(
    title: String,
    note: String?,
    busy: Boolean,
    resetSignal: Int,
    errorSignal: Int,
    onComplete: (String) -> Unit,
    onBiometric: (() -> Unit)? = null,
) {
    val colors = KontivaTheme.colors
    val haptics = LocalHapticFeedback.current
    var code by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(resetSignal) { code = "" }
    LaunchedEffect(errorSignal) {
        if (errorSignal > 0) {
            code = ""
            for (dx in listOf(-14f, 12f, -9f, 7f, -4f, 0f)) shake.animateTo(dx, tween(45))
        }
    }

    fun press(digit: Int) {
        if (busy || code.length >= PIN_LENGTH) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = code + digit
        code = next
        if (next.length == PIN_LENGTH) onComplete(next)
    }

    fun backspace() {
        if (busy || code.isEmpty()) return
        code = code.dropLast(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KontivaTheme.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(KontivaTheme.spaceXxl))
        Image(
            painter = painterResource(R.drawable.wordmark),
            contentDescription = "Kontiva",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(30.dp),
        )
        Spacer(Modifier.height(KontivaTheme.spaceMd))
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        if (note != null) {
            Spacer(Modifier.height(KontivaTheme.spaceXxs))
            Text(note, fontSize = 13.sp, color = colors.textTertiary, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(KontivaTheme.spaceXl))
        Row(
            modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            repeat(PIN_LENGTH) { i -> PinDot(filled = i < code.length, accent = KontivaTheme.accent) }
        }

        Spacer(Modifier.weight(1f))
        Keypad(onDigit = ::press, onBackspace = ::backspace, onBiometric = onBiometric, textColor = colors.textPrimary)
        Spacer(Modifier.height(KontivaTheme.spaceLg))
    }
}

@Composable
private fun PinDot(filled: Boolean, accent: Color) {
    val colors = KontivaTheme.colors
    val scale by animateFloatAsState(if (filled) 1f else 0.82f, spring(dampingRatio = 0.5f), label = "dot")
    Box(
        modifier = Modifier
            .size(15.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (filled) accent else colors.softBorder),
    )
}

@Composable
private fun Keypad(onDigit: (Int) -> Unit, onBackspace: () -> Unit, onBiometric: (() -> Unit)?, textColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { d -> KeyButton(label = d.toString(), textColor = textColor) { onDigit(d) } }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (onBiometric != null) BiometricKey(onBiometric) else Spacer(Modifier.size(72.dp))
            KeyButton(label = "0", textColor = textColor) { onDigit(0) }
            KeyButton(label = null, textColor = textColor, onClick = onBackspace)
        }
    }
}

@Composable
private fun BiometricKey(onClick: () -> Unit) {
    val colors = KontivaTheme.colors
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(colors.cardSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Fingerprint, contentDescription = "biometric", tint = KontivaTheme.accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun KeyButton(label: String?, textColor: Color, onClick: () -> Unit) {
    val colors = KontivaTheme.colors
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(colors.cardSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(label, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = textColor)
        } else {
            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "delete", tint = colors.textSecondary)
        }
    }
}
