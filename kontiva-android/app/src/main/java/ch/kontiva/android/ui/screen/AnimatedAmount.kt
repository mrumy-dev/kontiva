package ch.kontiva.android.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/** A CHF amount that cross-fades when its value changes — the Android analog of the
 *  iOS `.contentTransition(.numericText())` used on the big totals. */
@Composable
fun AnimatedAmount(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "amount",
    ) { value ->
        Text(value, fontSize = fontSize, fontWeight = fontWeight, color = color, maxLines = 1, modifier = modifier)
    }
}
