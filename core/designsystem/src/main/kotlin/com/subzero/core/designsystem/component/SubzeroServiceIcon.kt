package com.subzero.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.core.designsystem.preview.PreviewTheme
import com.subzero.core.designsystem.preview.SubzeroPreviews
import com.subzero.core.designsystem.theme.SubzeroTheme
import com.subzero.core.designsystem.theme.SubzeroTone

enum class ServiceIconSize(val container: Dp, val glyph: Dp, val letter: Int) {
    Row(44.dp, 22.dp, 18),
    Header(56.dp, 28.dp, 24),
}

/** One of eight desaturated hues; chosen deterministically from the service name. */
@Immutable
private data class MonogramHue(val dark: Color, val light: Color)

private val MonogramHues = listOf(
    MonogramHue(Color(0xFF7CB7FF), Color(0xFF2563EB)), // blue
    MonogramHue(Color(0xFFA78BFA), Color(0xFF6D28D9)), // violet
    MonogramHue(Color(0xFF5EEAD4), Color(0xFF0D9488)), // teal
    MonogramHue(Color(0xFF86EFAC), Color(0xFF15803D)), // green
    MonogramHue(Color(0xFFFCD34D), Color(0xFFB45309)), // amber
    MonogramHue(Color(0xFFFCA5A5), Color(0xFFC2410C)), // coral
    MonogramHue(Color(0xFFF9A8D4), Color(0xFFBE185D)), // rose
    MonogramHue(Color(0xFFB4C0DB), Color(0xFF475569)), // slate
)
private const val CONTAINER_ALPHA = 0.22f

/**
 * A service identity without brand assets: the first letter on a hue derived from the name,
 * so Netflix is always the same color everywhere in the app (design-system.md §7).
 */
@Composable
fun SubzeroServiceIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: ServiceIconSize = ServiceIconSize.Row,
) {
    val colors = SubzeroTheme.colors
    val hue = MonogramHues[monogramIndex(name)]
    val foreground = if (colors.isDark) hue.dark else hue.light
    val letter = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
    val shape = if (size == ServiceIconSize.Header) SubzeroTheme.shapes.md else SubzeroTheme.shapes.sm

    Box(
        modifier = modifier
            .size(size.container)
            .clip(shape)
            .background(foreground.copy(alpha = CONTAINER_ALPHA))
            .border(1.dp, colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = foreground,
            fontSize = size.letter.sp,
            fontWeight = FontWeight.SemiBold,
            style = SubzeroTheme.typography.title,
        )
    }
}

/** A toned icon in the same container as the monograms; used for categories and insights. */
@Composable
fun SubzeroIconContainer(
    icon: ImageVector,
    tone: SubzeroTone,
    modifier: Modifier = Modifier,
    size: ServiceIconSize = ServiceIconSize.Row,
    contentDescription: String? = null,
) {
    val colors = SubzeroTheme.colors
    val shape = if (size == ServiceIconSize.Header) SubzeroTheme.shapes.md else SubzeroTheme.shapes.sm
    Box(
        modifier = modifier
            .size(size.container)
            .clip(shape)
            .background(colors.containerForTone(tone))
            .border(1.dp, colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.forTone(tone),
            modifier = Modifier.size(size.glyph),
        )
    }
}

/** Stable across processes and app versions: a plain string hash, not `hashCode()`. */
internal fun monogramIndex(name: String): Int {
    var hash = 0
    for (ch in name.trim().lowercase()) hash = (hash * 31 + ch.code) and 0x7FFFFFFF
    return hash % MonogramHues.size
}

@SubzeroPreviews
@Composable
private fun SubzeroServiceIconPreview() {
    PreviewTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            listOf("Netflix", "ChatGPT", "Spotify", "Google One", "Adobe", "YouTube", "1Password", "").forEach {
                SubzeroServiceIcon(name = it, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

@SubzeroPreviews
@Composable
private fun SubzeroIconContainerPreview() {
    PreviewTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            SubzeroTone.entries.forEach {
                SubzeroIconContainer(
                    icon = com.subzero.core.designsystem.icon.SubzeroIcons.Savings,
                    tone = it,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            SubzeroServiceIcon(name = "Netflix", size = ServiceIconSize.Header)
        }
    }
}
