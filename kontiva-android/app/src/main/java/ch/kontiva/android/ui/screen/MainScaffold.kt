package ch.kontiva.android.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
                2 -> BillsScreen(vm)
                3 -> SavingsScreen(vm)
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

private enum class MoreDest { MENU, SETTINGS, DEBTS, INSIGHTS }

@Composable
private fun MoreTab(vm: KontivaViewModel) {
    var dest by remember { mutableStateOf(MoreDest.MENU) }
    when (dest) {
        MoreDest.MENU -> MoreMenu(vm, onOpen = { dest = it })
        MoreDest.SETTINGS -> SettingsScreen(vm, onBack = { dest = MoreDest.MENU })
        MoreDest.DEBTS -> DebtsScreen(vm, onBack = { dest = MoreDest.MENU })
        MoreDest.INSIGHTS -> InsightsScreen(vm, onBack = { dest = MoreDest.MENU })
    }
}

@Composable
private fun MoreMenu(vm: KontivaViewModel, onOpen: (MoreDest) -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    Column(Modifier.fillMaxSize().padding(KontivaTheme.spaceLg), verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd)) {
        Text(loc(L10nKey.navMore), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
            Column {
                MenuRow(Icons.Rounded.CreditCard, loc(L10nKey.navSchulden)) { onOpen(MoreDest.DEBTS) }
                MenuRow(Icons.Rounded.Lightbulb, loc(L10nKey.navInsights)) { onOpen(MoreDest.INSIGHTS) }
                MenuRow(Icons.Rounded.Settings, loc(L10nKey.settingsTitle)) { onOpen(MoreDest.SETTINGS) }
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(KontivaTheme.radiusControl),
            color = colors.cardSurface,
            modifier = Modifier.fillMaxWidth().clickable { vm.lock() },
        ) {
            Row(Modifier.padding(KontivaTheme.spaceMd), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.swissRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Text(loc(L10nKey.actionLock), color = colors.swissRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = KontivaTheme.colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(KontivaTheme.spaceMd), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(KontivaTheme.spaceSm))
        Text(label, fontSize = 16.sp, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textTertiary)
    }
}
