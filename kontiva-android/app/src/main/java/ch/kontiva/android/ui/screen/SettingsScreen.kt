package ch.kontiva.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.AccentTheme
import ch.kontiva.android.core.AppLanguage
import ch.kontiva.android.core.AutoLockInterval
import ch.kontiva.android.core.Canton
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme
import ch.kontiva.android.ui.theme.color

@Composable
fun SettingsScreen(vm: KontivaViewModel, onBack: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var showLangPicker by remember { mutableStateOf(false) }
    var showChangePass by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var name by remember(vm.household) { mutableStateOf(vm.household?.name ?: "") }
    var canton by remember(vm.household) { mutableStateOf(vm.household?.canton) }
    var cantonMenu by remember { mutableStateOf(false) }
    var autoLockMenu by remember { mutableStateOf(false) }

    if (showLangPicker) {
        LanguagePicker(selected = vm.settings.language, onSelect = { vm.setLanguage(it) }, onBack = { showLangPicker = false })
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(KontivaTheme.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back", tint = colors.textPrimary) }
                Text(loc(L10nKey.settingsTitle), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }

        // Profil
        item {
            SettingsCard(loc(L10nKey.settingsProfile)) {
                OutlinedTextField(name, { name = it }, label = { Text(loc(L10nKey.profileName)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().clickable { cantonMenu = true }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                    Text(loc(L10nKey.settingsCanton), color = colors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Text(canton?.let { "${it.name} (${it.abbreviation})" } ?: "—", color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                    DropdownMenu(expanded = cantonMenu, onDismissRequest = { cantonMenu = false }) {
                        DropdownMenuItem(text = { Text("—") }, onClick = { canton = null; cantonMenu = false })
                        Canton.all.forEach { c ->
                            DropdownMenuItem(text = { Text("${c.name} (${c.abbreviation})") }, onClick = { canton = c; cantonMenu = false })
                        }
                    }
                }
                TextButton(onClick = { vm.updateProfile(name, canton) }) { Text(loc(L10nKey.commonSave), color = KontivaTheme.accent) }
            }
        }

        // Sprache
        item {
            SettingsCard(loc(L10nKey.settingsLanguage)) {
                NavRow(vm.settings.language.displayName) { showLangPicker = true }
            }
        }

        // Farbthema
        item {
            SettingsCard(loc(L10nKey.settingsTheme)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AccentTheme.entries.forEach { t ->
                        AccentSwatch(t, selected = vm.settings.accent == t) { vm.setAccent(t) }
                    }
                }
                Spacer(Modifier.height(KontivaTheme.spaceXs))
                Text(loc(vm.settings.accent.labelKey), fontSize = 12.sp, color = colors.textTertiary)
            }
        }

        // Sicherheit
        item {
            SettingsCard(loc(L10nKey.settingsSecurity)) {
                Row(Modifier.fillMaxWidth().clickable { autoLockMenu = true }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                    Text(loc(L10nKey.settingsAutoLock), color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    Text(vm.dataset.securitySettings.autoLock.displayLabel, color = KontivaTheme.accent, fontWeight = FontWeight.Medium)
                    DropdownMenu(expanded = autoLockMenu, onDismissRequest = { autoLockMenu = false }) {
                        AutoLockInterval.entries.forEach { iv ->
                            DropdownMenuItem(text = { Text(iv.displayLabel) }, onClick = { vm.setAutoLock(iv); autoLockMenu = false })
                        }
                    }
                }
                NavRow(loc(L10nKey.settingsChangePassphrase)) { showChangePass = true }
            }
        }

        // Gefahrenzone
        item {
            SettingsCard(loc(L10nKey.settingsDangerZone)) {
                Row(Modifier.fillMaxWidth().clickable { showDeleteConfirm = true }.padding(vertical = KontivaTheme.spaceSm)) {
                    Text(loc(L10nKey.settingsDeleteAll), color = colors.swissRed, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Text("Kontiva 0.0.1 · ${loc(L10nKey.appTagline)}", fontSize = 12.sp, color = colors.textTertiary, modifier = Modifier.padding(KontivaTheme.spaceXs))
        }
    }

    if (showChangePass) ChangePassphraseSheet(onDismiss = { showChangePass = false }, onSubmit = { old, new, done -> vm.changePassphrase(old, new) { ok -> if (ok) showChangePass = false; done(ok) } })

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(loc(L10nKey.settingsDeleteAll)) },
            text = { Text(loc(L10nKey.lockRecoveryWarning)) },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; vm.deleteAllData() }) { Text(loc(L10nKey.settingsDeleteAll), color = colors.swissRed) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(loc(L10nKey.commonCancel)) } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    val colors = KontivaTheme.colors
    Column {
        Text(title.uppercase(), fontSize = 11.sp, color = colors.textTertiary, modifier = Modifier.padding(start = KontivaTheme.spaceXs, bottom = KontivaTheme.spaceXxs))
        Surface(shape = RoundedCornerShape(KontivaTheme.radiusCard), color = colors.cardSurface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(KontivaTheme.spaceMd)) { content() }
        }
    }
}

@Composable
private fun NavRow(value: String, onClick: () -> Unit) {
    val colors = KontivaTheme.colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textTertiary)
    }
}

@Composable
private fun AccentSwatch(theme: AccentTheme, selected: Boolean, onClick: () -> Unit) {
    val c = theme.color(isSystemInDarkTheme())
    Box(Modifier.size(40.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (selected) Box(Modifier.size(40.dp).clip(CircleShape).border(2.5.dp, c, CircleShape))
        Box(Modifier.size(26.dp).clip(CircleShape).background(c))
    }
}

@Composable
private fun LanguagePicker(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, onBack: () -> Unit) {
    val colors = KontivaTheme.colors
    Column(Modifier.fillMaxSize().padding(KontivaTheme.spaceLg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back", tint = colors.textPrimary) }
            Text(LocalLocalizer.current(L10nKey.settingsLanguage), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(AppLanguage.entries) { lang ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(lang); onBack() }.padding(vertical = KontivaTheme.spaceSm), verticalAlignment = Alignment.CenterVertically) {
                    Text(lang.displayName, fontSize = 16.sp, color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    if (lang == selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = KontivaTheme.accent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePassphraseSheet(onDismiss: () -> Unit, onSubmit: (String, String, (Boolean) -> Unit) -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.cardSurface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = KontivaTheme.spaceLg).padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.settingsChangePassphrase), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            OutlinedTextField(old, { old = it; failed = false }, label = { Text(loc(L10nKey.lockEnterPassphrase)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(new, { new = it }, label = { Text(loc(L10nKey.settingsChangePassphrase)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            if (failed) Text(loc(L10nKey.lockWrongPassphrase), fontSize = 12.sp, color = colors.swissRed)
            Button(
                onClick = { onSubmit(old, new) { ok -> failed = !ok } },
                enabled = old.length >= 4 && new.length >= 6,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(KontivaTheme.radiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
            ) { Text(loc(L10nKey.commonSave), fontWeight = FontWeight.SemiBold) }
        }
    }
}
