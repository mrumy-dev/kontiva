package ch.kontiva.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Shared motion tokens so every animation in the app feels like one hand tuned it,
 * rather than a pile of ad-hoc springs. The Android counterpart of the iOS app's
 * `.snappy` / `.numericText()` / `.symbolEffect` motion vocabulary.
 */
object KontivaMotion {
    /** Lively but controlled — the default for interactive feedback. */
    fun <T> snappy() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

    /** Tab cross-fade duration (ms). */
    const val TAB_MS = 240

    /** Screen push/pop slide duration (ms). */
    const val NAV_MS = 300

    /** Per-item delay in a staggered entrance (ms). */
    const val STAGGER_STEP_MS = 45

    /** Entrance rise+fade duration (ms). */
    const val ENTER_MS = 360
}

/**
 * iOS-style press feedback: the surface springs down to [scaleTo] while held and
 * back on release. Observes pointer events without consuming them, so it stacks on
 * top of an existing `clickable`/`combinedClickable` and never blocks scrolling.
 */
fun Modifier.pressScale(scaleTo: Float = 0.97f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = KontivaMotion.snappy(),
        label = "pressScale",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation() // null on cancel (e.g. a scroll) → released
                pressed = false
            }
        }
}

/**
 * A one-shot entrance: the element fades up into place, [index] steps after its
 * siblings, giving a screen's cards a settled-in cascade. Plays once per composition.
 */
@Composable
fun Modifier.appearStagger(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(
                durationMillis = KontivaMotion.ENTER_MS,
                delayMillis = index * KontivaMotion.STAGGER_STEP_MS,
                easing = FastOutSlowInEasing,
            ),
        )
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 26.dp.toPx()
    }
}
