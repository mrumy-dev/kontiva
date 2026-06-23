package ch.kontiva.android.ui.screen

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.BugReport
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme

/** The app areas offered in the report's "where" picker (1:1 with iOS ReportArea). */
private enum class ReportArea(val titleKey: L10nKey) {
    OVERVIEW(L10nKey.overviewTitle),
    PLANNING(L10nKey.planningTitle),
    BILLS(L10nKey.billsTitle),
    SPAREN(L10nKey.navSparen),
    SCHULDEN(L10nKey.navSchulden),
    INSIGHTS(L10nKey.navInsights),
    SETTINGS(L10nKey.navSettings),
}

/** Problem melden: compose a redacted, copy-to-clipboard bug report — fully local,
 *  no network (the app has no INTERNET permission). 1:1 with iOS ReportProblemView. */
@Composable
fun ReportProblemScreen(vm: KontivaViewModel, onBack: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    val clipboard = LocalClipboardManager.current
    var area by remember { mutableStateOf(ReportArea.OVERVIEW) }
    var areaMenu by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    var composed by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back", tint = colors.textPrimary) }
            Text(loc(L10nKey.navReport), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }

        Row(
            Modifier.fillMaxWidth().clickable { areaMenu = true }.padding(vertical = KontivaTheme.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(loc(L10nKey.reportArea), color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(loc(area.titleKey), color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
            DropdownMenu(expanded = areaMenu, onDismissRequest = { areaMenu = false }) {
                ReportArea.entries.forEach { a ->
                    DropdownMenuItem(text = { Text(loc(a.titleKey)) }, onClick = { area = a; areaMenu = false })
                }
            }
        }

        ReportField(loc(L10nKey.reportSummary), summary) { summary = it }
        ReportField(loc(L10nKey.reportExpected), expected) { expected = it }
        ReportField(loc(L10nKey.reportActual), actual) { actual = it }
        ReportField(loc(L10nKey.reportSteps), steps) { steps = it }

        Text(loc(L10nKey.reportRedactionNote), fontSize = 12.sp, color = colors.textTertiary)

        Button(
            onClick = {
                composed = BugReport(
                    summary = summary, expectedBehavior = expected, actualBehavior = actual, reproductionSteps = steps,
                    appVersion = "0.0.1", systemVersion = "Android ${Build.VERSION.RELEASE}",
                    appLanguage = vm.settings.language.code, selectedArea = loc(area.titleKey),
                ).composeRedacted()
                copied = false
            },
            enabled = summary.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
        ) { Text(loc(L10nKey.commonDone), fontWeight = FontWeight.SemiBold) }

        Text(loc(L10nKey.reportCopyHint), fontSize = 12.sp, color = colors.textTertiary)

        composed?.let { text ->
            Text(loc(L10nKey.reportTitle), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = colors.textPrimary, modifier = Modifier.padding(KontivaTheme.spaceMd))
                }
            }
            Button(
                onClick = { clipboard.setText(AnnotatedString(text)); copied = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) {
                Icon(if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(KontivaTheme.spaceXs))
                Text(loc(if (copied) L10nKey.commonDone else L10nKey.commonCopy), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.size(KontivaTheme.spaceLg))
    }
}

@Composable
private fun ReportField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 4,
    )
}
