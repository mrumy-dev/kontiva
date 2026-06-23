package ch.kontiva.android.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.Money
import ch.kontiva.android.core.SavingsCategory
import ch.kontiva.android.core.SavingsGoal
import ch.kontiva.android.core.total
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme
import ch.kontiva.android.ui.theme.icon

@Composable
fun SavingsScreen(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val goals = vm.dataset.savingsGoals
    var showSheet by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.navSparen), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSheet = true }) { Icon(Icons.Rounded.Add, contentDescription = null, tint = KontivaTheme.accent) }
            }
        }
        if (goals.isEmpty()) {
            item { EmptyCard(loc(L10nKey.sparenEmpty)) }
            return@LazyColumn
        }
        item {
            val accumulated = goals.map { it.accumulated() }.total()
            val monthly = goals.mapNotNull { it.monthlyContribution }.total()
            Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(KontivaTheme.spaceMd)) {
                    Text(loc(L10nKey.sparenAccumulatedTotal).uppercase(), fontSize = 11.sp, color = colors.textTertiary)
                    Text(accumulated.formattedCHF(), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(Modifier.size(KontivaTheme.spaceSm))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(loc(L10nKey.sparenMonthlyTotal), fontSize = 10.sp, color = colors.textTertiary)
                            Text(monthly.formattedCHF(), fontSize = 13.sp, color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text(loc(L10nKey.sparenGoalsLabel), fontSize = 10.sp, color = colors.textTertiary)
                            Text("${goals.size}", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        for (g in goals) {
            item { GoalCard(g, onDelete = { vm.deleteSavingsGoal(g.id) }) }
        }
    }

    if (showSheet) SavingsSheet(onDismiss = { showSheet = false }, onSave = { name, cat, monthly, starting, target ->
        vm.addSavingsGoal(name, cat, monthly, starting, target); showSheet = false
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalCard(g: SavingsGoal, onDelete: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    Surface(
        shape = RoundedCornerShape(KontivaTheme.radiusCard),
        color = colors.cardSurface,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onDelete),
    ) {
        Row(Modifier.padding(KontivaTheme.spaceMd), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(KontivaTheme.accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(g.category.icon(), contentDescription = null, tint = KontivaTheme.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(KontivaTheme.spaceSm))
            Column(Modifier.weight(1f)) {
                Text(g.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                val sub = buildString {
                    g.monthlyContribution?.let { append(it.formattedCHF()).append(" ").append(loc(L10nKey.sparenPerMonth)) }
                    if (g.hasTarget) {
                        if (isNotEmpty()) append("  ·  ")
                        append(g.accumulated().formattedCHF()).append(" / ").append(g.target.formattedCHF())
                    }
                }
                if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp, color = colors.textTertiary)
            }
            if (g.hasTarget) {
                Spacer(Modifier.size(KontivaTheme.spaceSm))
                ProgressRing(g.progressPercent())
            }
        }
    }
}

@Composable
private fun ProgressRing(percent: Int, size: Dp = 48.dp) {
    val colors = KontivaTheme.colors
    val accent = KontivaTheme.accent
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = this.size.minDimension * 0.12f
            val inset = sw / 2
            drawCircle(color = colors.softBorder.copy(alpha = 0.5f), radius = (this.size.minDimension - sw) / 2, style = Stroke(sw))
            drawArc(
                color = accent, startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f), useCenter = false,
                topLeft = Offset(inset, inset), size = Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        Text("$percent%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavingsSheet(onDismiss: () -> Unit, onSave: (String, SavingsCategory, Money?, Money, Money) -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SavingsCategory.EMERGENCY) }
    var menuOpen by remember { mutableStateOf(false) }
    var monthlyText by remember { mutableStateOf("") }
    var startingText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    val canSave = name.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.cardSurface) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KontivaTheme.spaceLg)
                .padding(bottom = KontivaTheme.spaceLg)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.navSparen), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(name, { name = it }, label = { Text(loc(L10nKey.formName)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                Text(loc(L10nKey.formCategory), color = colors.textSecondary)
                Spacer(Modifier.weight(1f))
                Text(loc(category.labelKey), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = colors.textTertiary)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    SavingsCategory.entries.forEach { c ->
                        DropdownMenuItem(text = { Text(loc(c.labelKey)) }, onClick = { category = c; menuOpen = false })
                    }
                }
            }
            MoneyField(loc(L10nKey.formMonthlyContribution), monthlyText) { monthlyText = it }
            MoneyField(loc(L10nKey.formStartingBalance), startingText) { startingText = it }
            MoneyField(loc(L10nKey.formTarget), targetText) { targetText = it }
            Button(
                onClick = {
                    if (canSave) onSave(
                        name.trim(), category,
                        Money.parse(monthlyText), Money.parse(startingText) ?: Money.zero, Money.parse(targetText) ?: Money.zero,
                    )
                },
                enabled = canSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = androidx.compose.ui.graphics.Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, prefix = { Text("CHF ") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
    )
}
