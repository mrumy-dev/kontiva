package ch.kontiva.android.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme
import java.time.format.DateTimeFormatter

/** ‹ Juni 2026 › — drives the app-wide selected month (localized month name). */
@Composable
fun MonthSelector(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val label = remember(vm.selectedMonth, loc.language) {
        vm.selectedMonth
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", loc.language.locale))
            .replaceFirstChar { it.uppercase() }
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.softBorder),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.ChevronLeft, contentDescription = "previous month",
                tint = KontivaTheme.accent,
                modifier = Modifier.clickable { vm.previousMonth() }.padding(KontivaTheme.spaceXs).size(22.dp),
            )
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Icon(
                Icons.Rounded.ChevronRight, contentDescription = "next month",
                tint = KontivaTheme.accent,
                modifier = Modifier.clickable { vm.nextMonth() }.padding(KontivaTheme.spaceXs).size(22.dp),
            )
        }
    }
}
