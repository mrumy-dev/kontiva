package ch.kontiva.android.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ch.kontiva.android.core.l10n.L10nKey
import ch.kontiva.android.core.l10n.LocalLocalizer
import ch.kontiva.android.ui.theme.KontivaTheme

/**
 * The long-press menu for a list row: Bearbeiten / Löschen — instead of the old
 * long-press = instant (accidental) delete. Drop inside a Box next to the row and
 * drive [expanded] from the row's onLongClick. 1:1 with the iOS `.contextMenu`.
 */
@Composable
fun RowActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val loc = LocalLocalizer.current
    val colors = KontivaTheme.colors
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(loc(L10nKey.commonEdit)) },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textSecondary) },
            onClick = { onDismiss(); onEdit() },
        )
        DropdownMenuItem(
            text = { Text(loc(L10nKey.commonDelete), color = colors.swissRed) },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = colors.swissRed) },
            onClick = { onDismiss(); onDelete() },
        )
    }
}
