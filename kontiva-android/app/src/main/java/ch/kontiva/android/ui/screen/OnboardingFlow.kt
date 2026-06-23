package ch.kontiva.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.AppLanguage
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.KontivaViewModel
import ch.kontiva.android.ui.theme.KontivaTheme

private enum class OnbStep { Welcome, Language, Profile, Code }

/** Onboarding: welcome → language → profile → code (which creates the vault). */
@Composable
fun OnboardingFlow(vm: KontivaViewModel) {
    val loc = LocalLocalizer.current
    var step by remember { mutableStateOf(OnbStep.Welcome) }
    var name by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf<String?>(null) }
    var firstCode by remember { mutableStateOf<String?>(null) }
    var resetSignal by remember { mutableIntStateOf(0) }
    var errorSignal by remember { mutableIntStateOf(0) }

    when (step) {
        OnbStep.Welcome -> WelcomeScreen(onStart = { step = OnbStep.Language })

        OnbStep.Language -> LanguageStep(
            selected = vm.settings.language,
            onSelect = vm::setLanguage,
            onBack = { step = OnbStep.Welcome },
            onNext = { step = OnbStep.Profile },
        )

        OnbStep.Profile -> ProfileStep(
            name = name,
            onName = { name = it },
            avatar = avatar,
            onAvatar = { avatar = it },
            onBack = { step = OnbStep.Language },
            onContinue = { step = OnbStep.Code },
            onSkip = { name = ""; avatar = null; step = OnbStep.Code },
        )

        OnbStep.Code -> PinEntry(
            title = loc(if (firstCode == null) L10nKey.pinSetTitle else L10nKey.pinConfirmTitle),
            note = if (firstCode == null) loc(L10nKey.pinRequirementNote) else null,
            busy = vm.busy,
            resetSignal = resetSignal,
            errorSignal = errorSignal,
            onComplete = { code ->
                when {
                    firstCode == null -> {
                        firstCode = code
                        resetSignal++ // clear for the confirm entry
                    }
                    code == firstCode -> vm.completeSetup(code, name, avatar = avatar)
                    else -> {
                        firstCode = null
                        errorSignal++ // shake + clear, back to "set"
                    }
                }
            },
        )
    }
}

@Composable
private fun LanguageStep(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    Column(Modifier.fillMaxSize().padding(KontivaTheme.spaceLg)) {
        StepHeader(loc(L10nKey.settingsLanguage), onBack)
        LazyColumn(Modifier.weight(1f)) {
            items(AppLanguage.entries) { lang ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang) }
                        .padding(vertical = KontivaTheme.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lang.displayName, fontSize = 16.sp, color = colors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    if (lang == selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = KontivaTheme.accent)
                    }
                }
            }
        }
        PrimaryButton(loc(L10nKey.commonNext), onClick = onNext)
    }
}

@Composable
private fun ProfileStep(
    name: String,
    onName: (String) -> Unit,
    avatar: String?,
    onAvatar: (String?) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    var showPicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(KontivaTheme.spaceLg)) {
        StepHeader(loc(L10nKey.onboardingProfileTitle), onBack)
        Text(loc(L10nKey.onboardingProfileBody), color = colors.textSecondary, fontSize = 15.sp)
        Spacer(Modifier.height(KontivaTheme.spaceXl))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clip(CircleShape).clickable { showPicker = true }) {
                ProfileAvatar(avatar, 84.dp)
            }
            Spacer(Modifier.height(KontivaTheme.spaceXs))
            TextButton(onClick = { showPicker = true }) {
                Text(loc(L10nKey.profileChoosePicture), color = KontivaTheme.accent, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(KontivaTheme.spaceMd))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text(loc(L10nKey.profileName)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(loc(L10nKey.commonNext), onClick = onContinue)
        Spacer(Modifier.height(KontivaTheme.spaceXs))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(loc(L10nKey.onboardingSkip), color = colors.textSecondary)
        }
    }
    if (showPicker) {
        AvatarPickerSheet(selected = avatar, onSelect = { onAvatar(it); showPicker = false }, onDismiss = { showPicker = false })
    }
}

@Composable
private fun StepHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "back", tint = KontivaTheme.colors.textPrimary)
        }
        Spacer(Modifier.weight(1f))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = KontivaTheme.colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(KontivaTheme.radiusControl),
        colors = ButtonDefaults.buttonColors(containerColor = KontivaTheme.accent, contentColor = Color.White),
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
