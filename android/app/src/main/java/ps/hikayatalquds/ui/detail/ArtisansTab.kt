package ps.hikayatalquds.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ArchivePhoto
import ps.hikayatalquds.core.designsystem.component.EmptyNote
import ps.hikayatalquds.core.designsystem.component.HikayatCard
import ps.hikayatalquds.core.designsystem.component.MetaTag
import ps.hikayatalquds.core.designsystem.component.PhotoCredit
import ps.hikayatalquds.core.designsystem.component.SectionHeader
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.Artisan
import ps.hikayatalquds.domain.model.ArtisanCategory

/**
 * The crafts, food and markets around a place.
 *
 * Each entry carries a "how to support this" note, and an orientation note
 * rather than an address - the archive does not claim to know exactly where a
 * family stall stands this week, and pretending otherwise would send visitors
 * to the wrong door.
 */
@Composable
fun ArtisansTabContent(
    artisans: List<Artisan>,
    allowRemote: Boolean,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val colors = HikayatTheme.colors

    if (artisans.isEmpty()) {
        EmptyNote(
            text = stringResource(R.string.empty_artisans),
            icon = Icons.Filled.Storefront,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.tab_artisans),
            icon = Icons.Filled.Storefront,
        )
        Spacer(Modifier.height(12.dp))

        artisans.forEach { artisan ->
            HikayatCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(170.dp)) {
                        ArchivePhoto(
                            model = artisan.displayModel(allowRemote),
                            contentDescription = artisan.imageAlt[language].ifBlank { null },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(Modifier.padding(14.dp)) {
                        MetaTag(
                            label = artisan.category.label(language),
                            color = artisan.category.accent(colors),
                        )
                        Spacer(Modifier.height(9.dp))
                        Text(
                            text = artisan.name[language],
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = artisan.description[language],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (artisan.locationNote[language].isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = artisan.locationNote[language],
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted,
                            )
                        }
                        if (artisan.supportNote[language].isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            MetaTag(
                                label = artisan.supportNote[language],
                                icon = Icons.Filled.Handshake,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    PhotoCredit(
                        credit = artisan.imageCredit,
                        license = artisan.imageLicense,
                        sourceUrl = artisan.imageSourceUrl,
                        bundled = artisan.assetPath != null,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.about_photographs_body),
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun ArtisanCategory.label(language: ps.hikayatalquds.domain.model.AppLanguage): String =
    when (this) {
        ArtisanCategory.CRAFT -> if (language.isArabic) "حرفة" else "Craft"
        ArtisanCategory.FOOD -> if (language.isArabic) "طعام" else "Food"
        ArtisanCategory.MARKET -> if (language.isArabic) "سوق" else "Market"
    }

private fun ArtisanCategory.accent(
    colors: ps.hikayatalquds.core.designsystem.HikayatExtendedColors,
) = when (this) {
    ArtisanCategory.CRAFT -> colors.quarter
    ArtisanCategory.FOOD -> colors.amber
    ArtisanCategory.MARKET -> colors.souq
}
