package ch.kontiva.android.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildFriendly
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Tram
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import ch.kontiva.android.core.FixedExpenseCategory
import ch.kontiva.android.core.VariableBudgetCategory

/** Material-icon equivalents of the iOS SF Symbols per category. */
fun FixedExpenseCategory.icon(): ImageVector = when (this) {
    FixedExpenseCategory.RENT -> Icons.Rounded.Home
    FixedExpenseCategory.MORTGAGE -> Icons.Rounded.Key
    FixedExpenseCategory.HEALTH_INSURANCE -> Icons.Rounded.LocalHospital
    FixedExpenseCategory.INSURANCE -> Icons.Rounded.Shield
    FixedExpenseCategory.UTILITIES -> Icons.Rounded.Bolt
    FixedExpenseCategory.TELECOM -> Icons.Rounded.Wifi
    FixedExpenseCategory.SUBSCRIPTION -> Icons.Rounded.Autorenew
    FixedExpenseCategory.SERAFE -> Icons.Rounded.Tv
    FixedExpenseCategory.LEASING -> Icons.Rounded.DirectionsCar
    FixedExpenseCategory.PUBLIC_TRANSPORT -> Icons.Rounded.Tram
    FixedExpenseCategory.CHILDCARE -> Icons.Rounded.ChildFriendly
    FixedExpenseCategory.EDUCATION -> Icons.Rounded.School
    FixedExpenseCategory.MEMBERSHIP -> Icons.Rounded.Group
    FixedExpenseCategory.ALIMONY -> Icons.Rounded.Favorite
    FixedExpenseCategory.TAXES -> Icons.Rounded.AccountBalance
    FixedExpenseCategory.OTHER -> Icons.Rounded.MoreHoriz
}

fun VariableBudgetCategory.icon(): ImageVector = when (this) {
    VariableBudgetCategory.GROCERIES -> Icons.Rounded.ShoppingCart
    VariableBudgetCategory.DINING -> Icons.Rounded.Restaurant
    VariableBudgetCategory.HOUSEHOLD -> Icons.Rounded.Home
    VariableBudgetCategory.CLOTHING -> Icons.Rounded.Checkroom
    VariableBudgetCategory.PERSONAL -> Icons.Rounded.HealthAndSafety
    VariableBudgetCategory.HEALTH -> Icons.Rounded.LocalHospital
    VariableBudgetCategory.FUEL -> Icons.Rounded.LocalGasStation
    VariableBudgetCategory.TRANSPORT -> Icons.Rounded.DirectionsBus
    VariableBudgetCategory.LEISURE -> Icons.Rounded.SportsEsports
    VariableBudgetCategory.ENTERTAINMENT -> Icons.Rounded.Theaters
    VariableBudgetCategory.CHILDREN -> Icons.Rounded.ChildFriendly
    VariableBudgetCategory.PETS -> Icons.Rounded.Pets
    VariableBudgetCategory.GIFTS -> Icons.Rounded.CardGiftcard
    VariableBudgetCategory.TRAVEL -> Icons.Rounded.Flight
    VariableBudgetCategory.EDUCATION -> Icons.Rounded.School
    VariableBudgetCategory.CHARITY -> Icons.Rounded.VolunteerActivism
    VariableBudgetCategory.OTHER -> Icons.Rounded.MoreHoriz
}
