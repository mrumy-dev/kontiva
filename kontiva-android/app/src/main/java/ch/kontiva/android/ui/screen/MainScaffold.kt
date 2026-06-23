package ch.kontiva.android.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme

private data class Tab(val labelKey: L10nKey, val icon: ImageVector)

/** The unlocked app shell: bottom tab bar. Overview + Planning are functional;
 *  Bills/Savings are placeholders until their stages land. */
@Composable
fun MainScaffold(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var tab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab(L10nKey.navOverview, Icons.Rounded.GridView),
        Tab(L10nKey.navPlanning, Icons.Rounded.CalendarMonth),
        Tab(L10nKey.navBills, Icons.Rounded.ReceiptLong),
        Tab(L10nKey.navSparen, Icons.Rounded.Savings),
        Tab(L10nKey.navMore, Icons.Rounded.MoreHoriz),
    )

    Scaffold(
        containerColor = colors.pageBackground,
        bottomBar = {
            NavigationBar(containerColor = colors.cardSurface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(loc(t.labelKey), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = KontivaTheme.accent,
                            selectedTextColor = KontivaTheme.accent,
                            indicatorColor = KontivaTheme.accent.copy(alpha = 0.12f),
                            unselectedIconColor = colors.textTertiary,
                            unselectedTextColor = colors.textTertiary,
                        ),
                    )
                }
            }
        },
    ) { inner ->
        Box(
            Modifier
                .padding(inner)
                .statusBarsPadding()
                .fillMaxSize(),
        ) {
            when (tab) {
                0 -> OverviewScreen(vm)
                1 -> PlanningScreen(vm)
                2 -> ComingSoon(loc(L10nKey.navBills))
                3 -> ComingSoon(loc(L10nKey.navSparen))
                else -> MoreTab(vm)
            }
        }
    }
}

@Composable
private fun ComingSoon(title: String) {
    val colors = KontivaTheme.colors
    Column(
        Modifier.fillMaxSize().padding(KontivaTheme.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.height(KontivaTheme.spaceXs))
        Text("Folgt als nächste Stufe.", fontSize = 14.sp, color = colors.textTertiary)
    }
}

@Composable
private fun MoreTab(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    Column(
        Modifier.fillMaxSize().padding(KontivaTheme.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        vm.household?.name?.let {
            Text(it, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Spacer(Modifier.height(KontivaTheme.spaceLg))
        }
        PrimaryButton(loc(L10nKey.actionLock), onClick = { vm.lock() })
    }
}
