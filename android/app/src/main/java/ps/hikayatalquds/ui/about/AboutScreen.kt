package ps.hikayatalquds.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.BuildConfig
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.OliveHeader
import ps.hikayatalquds.core.designsystem.component.SectionHeader

/**
 * What this app is, and the rules it holds itself to.
 *
 * Written as commitments rather than marketing: the generator does not invent
 * facts, the archive works with no connection, and every photograph carries its
 * photographer. If any of those stops being true, this page becomes wrong,
 * which is the point of stating them.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = HikayatTheme.colors

    Scaffold(
        containerColor = colors.pageBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 30.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("header") {
                OliveHeader(
                    title = stringResource(R.string.app_name),
                    eyebrow = stringResource(R.string.track_badge),
                    subtitle = stringResource(R.string.app_tagline),
                )
            }

            item("rule") {
                AboutCard(
                    title = stringResource(R.string.about_rule_title),
                    body = stringResource(R.string.about_rule_body),
                    icon = Icons.Filled.Rule,
                )
            }

            item("offline") {
                AboutCard(
                    title = stringResource(R.string.about_offline_title),
                    body = stringResource(R.string.about_offline_body),
                    icon = Icons.Filled.CloudOff,
                )
            }

            item("photographs") {
                AboutCard(
                    title = stringResource(R.string.about_photographs_title),
                    body = stringResource(R.string.about_photographs_body),
                    icon = Icons.Filled.PhotoCamera,
                )
            }

            item("team") {
                HikayatCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.about_team),
                            icon = Icons.Filled.Groups,
                        )
                        Spacer(Modifier.height(12.dp))
                        TEAM.forEach { (name, role) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                MetaTag(label = role, color = colors.muted)
                            }
                        }
                    }
                }
            }

            item("licences") {
                HikayatCard {
                    Column(Modifier.padding(16.dp)) {
                        SectionHeader(title = stringResource(R.string.about_licences))
                        Spacer(Modifier.height(10.dp))
                        LICENCES.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            item("version") {
                Text(
                    text = "${stringResource(R.string.brand_lockup)} · " +
                        stringResource(
                            R.string.settings_about_app,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun AboutCard(title: String, body: String, icon: ImageVector) {
    HikayatCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title = title, icon = icon)
            Spacer(Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val TEAM = listOf(
    "اوس حماد" to "Team lead",
    "يزيد الحداد" to "Software developer",
    "امير الرشايدة" to "Software developer",
    "اسماء عبداللطيف" to "Software developer",
    "عبير شبانة" to "Marketing",
)

/**
 * The licences that matter to a reader: the photographs are Creative Commons
 * and the fonts are the Thmanyah family, both credited in place as well as here.
 */
private val LICENCES = listOf(
    "Photographs · Wikimedia Commons contributors, CC BY-SA / CC BY, credited per image",
    "Typography · Thmanyah Sans, Serif Text and Serif Display",
    "Jetpack Compose, Room, Hilt, Retrofit, OkHttp, Coil · Apache License 2.0",
    "kotlinx.serialization, kotlinx.coroutines · Apache License 2.0",
)
