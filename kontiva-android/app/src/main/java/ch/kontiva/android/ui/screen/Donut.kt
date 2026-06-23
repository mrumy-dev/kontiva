package ch.kontiva.android.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.ui.theme.KontivaTheme

data class DonutSegment(val color: Color, val value: Long)

/** A ring chart with centered labels (1:1 with the iOS Overview donut). */
@Composable
fun Donut(
    segments: List<DonutSegment>,
    centerTop: String,
    centerValue: String,
    centerBottom: String,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = 190.dp,
) {
    val colors = KontivaTheme.colors
    val total = segments.sumOf { it.value }.coerceAtLeast(1L)
    // Clockwise reveal that re-runs whenever the breakdown changes (month switch, edit).
    val key = remember(segments) { segments.joinToString(",") { "${it.value}" } }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(key) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val strokeW = size.minDimension * 0.16f
            val inset = strokeW / 2
            val arcSize = Size(size.width - strokeW, size.height - strokeW)
            val topLeft = Offset(inset, inset)
            // track
            drawArc(
                color = colors.softBorder.copy(alpha = 0.5f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokeW, cap = StrokeCap.Butt),
            )
            val revealEnd = -90f + 360f * reveal.value
            var start = -90f
            val gap = 2.5f
            for (seg in segments) {
                if (seg.value <= 0) continue
                val sweep = 360f * seg.value / total
                val visibleEnd = minOf(start + sweep, revealEnd)
                if (visibleEnd > start) {
                    drawArc(
                        color = seg.color,
                        startAngle = start + gap / 2,
                        sweepAngle = (visibleEnd - start - gap).coerceAtLeast(0.1f),
                        useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round),
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text(centerTop, fontSize = 10.sp, color = colors.textTertiary)
            AnimatedAmount(centerValue, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            androidx.compose.material3.Text(centerBottom, fontSize = 10.sp, color = colors.textTertiary)
        }
    }
}
