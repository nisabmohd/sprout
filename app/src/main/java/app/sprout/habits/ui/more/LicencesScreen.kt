package app.sprout.habits.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import app.sprout.habits.ui.components.GroupGap
import app.sprout.habits.ui.components.groupedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sprout.habits.R

/** Everything bundled into the app, with its licence. Keep in step with the version catalog and res/font. */
private val LICENCES = listOf(
    "AndroidX (Compose, Material 3, Room, DataStore, Navigation, Lifecycle, Activity, Core, Glance)" to "Apache License 2.0",
    "Kotlin standard library and kotlinx.coroutines" to "Apache License 2.0",
    "kotlinx.serialization" to "Apache License 2.0",
    "Material Symbols (habit icons)" to "Apache License 2.0",
    "Space Grotesk font" to "SIL Open Font License 1.1",
    "Outfit font" to "SIL Open Font License 1.1",
    "Lexend font" to "SIL Open Font License 1.1",
    "Atkinson Hyperlegible Next font" to "SIL Open Font License 1.1",
    "Roboto Serif font" to "SIL Open Font License 1.1",
    "Space Mono font" to "SIL Open Font License 1.1",
)

@Composable
fun LicencesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.licences), onBack)
        // One group, like the Journal: rows 2 dp apart, big corners only on the outside.
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(GroupGap)) {
            itemsIndexed(LICENCES, key = { _, it -> it.first }) { i, (name, licence) ->
                SettingsCard(groupedShape(i, LICENCES.size)) { SettingsRow(name, licence) }
            }
        }
    }
}
