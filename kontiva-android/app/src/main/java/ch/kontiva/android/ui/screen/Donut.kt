package ch.kontiva.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
            var start = -90f
            val gap = 2.5f
            for (seg in segments) {
                if (seg.value <= 0) continue
                val sweep = 360f * seg.value / total
                drawArc(
                    color = seg.color,
                    startAngle = start + gap / 2,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.1f),
                    useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(strokeW, cap = StrokeCap.Round),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Text(centerTop, fontSize = 10.sp, color = colors.textTertiary)
            androidx.compose.material3.Text(
                centerValue, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary,
            )
            androidx.compose.material3.Text(centerBottom, fontSize = 10.sp, color = colors.textTertiary)
        }
    }
}
