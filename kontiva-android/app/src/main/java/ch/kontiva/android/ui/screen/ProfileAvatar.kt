package ch.kontiva.android.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.theme.KontivaTheme

/** The bundled local profile pictures — identical names to the iOS asset catalog so
 *  a household's avatar survives a cross-platform backup/restore. */
object ProfileIcons {
    val all: List<String> = listOf(
        "human-01-charcoal", "human-02-warm", "human-03-outline", "human-04-soft-square",
        "human-05-arc", "human-06-offset", "human-07-quiet", "human-08-premium",
        "household-01-couple", "household-02-family", "household-03-home", "household-04-shared",
        "monogram-01-a", "monogram-02-m", "monogram-03-r", "monogram-04-s",
    )
}

private val charcoal = Color(0xFF1B242D)
private val offWhite = Color(0xFFF4F2EC)

/** Renders the chosen profile picture, or a neutral charcoal placeholder. 1:1 with iOS ProfileAvatar. */
@Composable
fun ProfileAvatar(name: String?, size: Dp = 40.dp) {
    val shape = RoundedCornerShape(size * 0.24f)
    val context = LocalContext.current
    val resId = remember(name) {
        name?.let { context.resources.getIdentifier(it.replace('-', '_'), "drawable", context.packageName) } ?: 0
    }
    if (name != null && resId != 0) {
        Image(
            painter = painterResource(resId), contentDescription = null,
            modifier = Modifier.size(size).clip(shape), contentScale = ContentScale.Crop,
        )
    } else {
        Box(Modifier.size(size).clip(shape).background(charcoal), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = offWhite.copy(alpha = 0.9f), modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** A grid sheet to pick a bundled profile picture (or none). 1:1 with iOS AvatarPickerSheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.padding(horizontal = KontivaTheme.spaceLg).padding(bottom = KontivaTheme.spaceLg).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd),
        ) {
            Text(loc(L10nKey.profileChoosePicture), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            ProfileIcons.all.chunked(4).forEach { rowNames ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KontivaTheme.spaceMd)) {
                    rowNames.forEach { name ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(if (selected == name) 3.dp else 0.dp, if (selected == name) KontivaTheme.accent else Color.Transparent, RoundedCornerShape(16.dp))
                                    .clickable { onSelect(name) }
                                    .padding(2.dp),
                            ) { ProfileAvatar(name, 64.dp) }
                        }
                    }
                    repeat(4 - rowNames.size) { Box(Modifier.weight(1f)) }
                }
            }
            TextButton(onClick = { onSelect(null) }) { Text(loc(L10nKey.profileNoPicture), color = KontivaTheme.accent) }
        }
    }
}
