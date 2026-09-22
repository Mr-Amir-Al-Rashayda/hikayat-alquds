package ps.hikayatalquds.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ps.hikayatalquds.R
import ps.hikayatalquds.core.designsystem.HikayatTheme
import ps.hikayatalquds.core.designsystem.component.ArchivePhoto
import ps.hikayatalquds.core.designsystem.component.animationsEnabled
import ps.hikayatalquds.core.ui.LocalAppLanguage
import ps.hikayatalquds.domain.model.LocalizedText

/**
 * One photograph of Jerusalem at a time, with what it shows.
 *
 * The web app opens on this, and it does real work: before any list or map, a
 * reader sees the city. Every frame is a bundled photograph of an actual place
 * in the archive and taps through to it, so it is a way in rather than
 * decoration.
 *
 * It advances itself, and stops when the reader has turned animations off -
 * a moving image is the first thing somebody with vestibular sensitivity wants
 * gone, and it is also what makes the screen testable.
 */
@Composable
fun JerusalemPulse(
    onOpenLocation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    val animate = animationsEnabled()
    var index by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(true) }
    val scene = PULSE_SCENES[index]

    LaunchedEffect(playing, animate) {
        if (!playing || !animate) return@LaunchedEffect
        while (true) {
            delay(SCENE_MILLIS)
            index = (index + 1) % PULSE_SCENES.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable { onOpenLocation(scene.locationId) },
    ) {
        AnimatedContent(
            targetState = scene,
            transitionSpec = {
                fadeIn(tween(if (animate) 500 else 0)) togetherWith
                    fadeOut(tween(if (animate) 500 else 0))
            },
            label = "jerusalem-pulse",
        ) { current ->
            ArchivePhoto(
                model = "file:///android_asset/${current.assetPath}",
                contentDescription = current.title[language],
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (animate) {
                IconButton(onClick = { playing = !playing }) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (playing) R.string.narration_pause else R.string.narration_resume,
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = HikayatTheme.colors.amber,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = scene.title[language],
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = scene.caption[language],
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.86f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tapping a dot is how somebody who turned motion off still moves
                // through the scenes.
                PULSE_SCENES.forEachIndexed { dotIndex, _ ->
                    Box(
                        Modifier
                            .size(if (dotIndex == index) 16.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (dotIndex == index) {
                                    HikayatTheme.colors.amber
                                } else {
                                    Color.White.copy(alpha = 0.45f)
                                },
                            )
                            .clickable { index = dotIndex },
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(13.dp)
                        .clearAndSetSemantics { },
                )
            }
        }
    }
}

/** A photograph, the place it shows, and what the record says about it. */
private data class PulseScene(
    val locationId: String,
    val assetPath: String,
    val title: LocalizedText,
    val caption: LocalizedText,
)

/** Advanced slowly: this is something to look at, not a ticker. */
private const val SCENE_MILLIS = 6_000L

/**
 * The same eight scenes and captions the web home page opens with, so both
 * clients introduce Jerusalem the same way.
 */
private val PULSE_SCENES = listOf(
    PulseScene(
        locationId = "at-tur",
        assetPath = "images/jerusalem/at-tur.jpg",
        title = LocalizedText("Jerusalem from the Mount of Olives", "القدس من جبل الزيتون"),
        caption = LocalizedText(
            "The Old City and the Dome of the Rock seen from the eastern ridge.",
            "مشهد بانورامي للبلدة القديمة وقبة الصخرة من السفح الشرقي.",
        ),
    ),
    PulseScene(
        locationId = "bab-al-amud",
        assetPath = "images/jerusalem/bab-al-amud.jpg",
        title = LocalizedText("Bab al-Amud", "باب العامود"),
        caption = LocalizedText(
            "The Old City's northern gate and its living Jerusalemite square.",
            "البوابة الشمالية للبلدة القديمة وساحتها المقدسية الحيّة.",
        ),
    ),
    PulseScene(
        locationId = "muslim-quarter",
        assetPath = "images/jerusalem/muslim-quarter.jpg",
        title = LocalizedText("Markets of the Muslim Quarter", "أسواق حارة المسلمين"),
        caption = LocalizedText(
            "Stone vaults and illuminated shops along routes toward Al-Aqsa.",
            "عقود حجرية ودكاكين مضاءة على الطريق إلى المسجد الأقصى.",
        ),
    ),
    PulseScene(
        locationId = "silwan",
        assetPath = "images/jerusalem/silwan.jpg",
        title = LocalizedText("Silwan and its valley", "سلوان وواديها"),
        caption = LocalizedText(
            "Stone homes, terraced slopes and the memory of Silwan spring.",
            "بيوت الحجر والبساتين المدرّجة وذاكرة عين سلوان.",
        ),
    ),
    PulseScene(
        locationId = "armenian-quarter",
        assetPath = "images/jerusalem/armenian-quarter.jpg",
        title = LocalizedText("The Armenian Quarter", "حارة الأرمن"),
        caption = LocalizedText(
            "Artistic and devotional detail from Jerusalem's enduring Armenian presence.",
            "تفاصيل فنية وطقسية من الحضور الأرمني العريق في القدس.",
        ),
    ),
    PulseScene(
        locationId = "christian-quarter",
        assetPath = "images/jerusalem/christian-quarter.jpg",
        title = LocalizedText("The Christian Quarter", "حارة النصارى"),
        caption = LocalizedText(
            "Churches, monasteries and stone markets around the Holy Sepulchre and Muristan.",
            "كنائس وأديرة وأسواق حجرية تتجمع حول كنيسة القيامة والمورستان.",
        ),
    ),
    PulseScene(
        locationId = "maghariba-quarter",
        assetPath = "images/jerusalem/maghariba-quarter.jpg",
        title = LocalizedText("Memory of the Maghariba Quarter", "ذاكرة حارة المغاربة"),
        caption = LocalizedText(
            "A visual record of the historic Palestinian quarter beside al-Buraq Wall before its 1967 demolition.",
            "سجل بصري للحارة الفلسطينية التاريخية بجوار حائط البراق قبل هدمها سنة 1967.",
        ),
    ),
    PulseScene(
        locationId = "sheikh-jarrah",
        assetPath = "images/jerusalem/sheikh-jarrah.jpg",
        title = LocalizedText("Sheikh Jarrah", "الشيخ جراح"),
        caption = LocalizedText(
            "Stone homes, gardens and Jerusalemite family memory in a living neighbourhood.",
            "بيوت الحجر والحدائق وذاكرة العائلات المقدسية في حي ما زال حياً.",
        ),
    ),
)
