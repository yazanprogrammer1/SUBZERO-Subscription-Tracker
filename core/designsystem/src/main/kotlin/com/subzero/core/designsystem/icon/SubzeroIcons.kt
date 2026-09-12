package com.subzero.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The single icon vocabulary for SUBZERO (design-system.md §8).
 *
 * Features reference these names, never Material icons directly, so the outlined style stays
 * uniform and swapping the source later is a one-file change. Identity glyphs that Material
 * does not provide are drawn here.
 */
object SubzeroIcons {
    val Home: ImageVector = Icons.Outlined.Home
    val Subscriptions: ImageVector = Icons.AutoMirrored.Outlined.List
    val Calendar: ImageVector = Icons.Outlined.DateRange
    val Insights: ImageVector by lazy { insights() }
    val Settings: ImageVector = Icons.Outlined.Settings

    val Add: ImageVector = Icons.Outlined.Add
    val Search: ImageVector = Icons.Outlined.Search
    val Check: ImageVector = Icons.Outlined.Check
    val Close: ImageVector = Icons.Outlined.Close
    val Back: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    val ChevronRight: ImageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight
    val ChevronDown: ImageVector = Icons.Outlined.KeyboardArrowDown
    val Edit: ImageVector = Icons.Outlined.Edit
    val Delete: ImageVector = Icons.Outlined.Delete
    val Notifications: ImageVector = Icons.Outlined.Notifications
    val Info: ImageVector = Icons.Outlined.Info
    val Warning: ImageVector = Icons.Outlined.Warning
    val Pause: ImageVector by lazy { pause() }
    val Resume: ImageVector = Icons.Outlined.PlayArrow
    val Sparkle: ImageVector by lazy { sparkle() }
    val Refresh: ImageVector = Icons.Outlined.Refresh
    val Lock: ImageVector = Icons.Outlined.Lock
    val Person: ImageVector = Icons.Outlined.Person
    val Share: ImageVector = Icons.Outlined.Share
    val More: ImageVector = Icons.Outlined.MoreVert
    val TrendUp: ImageVector by lazy { trendUp() }
    val Savings: ImageVector by lazy { savings() }

    private const val STROKE = 2f

    private fun outlined(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "Subzero.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.stroke(pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathData,
        )

    /** Three rising bars. */
    private fun insights() = outlined("Insights") {
        stroke { moveTo(5f, 20f); lineTo(5f, 13f) }
        stroke { moveTo(12f, 20f); lineTo(12f, 6f) }
        stroke { moveTo(19f, 20f); lineTo(19f, 10f) }
        stroke { moveTo(3f, 20f); lineTo(21f, 20f) }
    }

    private fun pause() = outlined("Pause") {
        stroke { moveTo(9f, 6f); lineTo(9f, 18f) }
        stroke { moveTo(15f, 6f); lineTo(15f, 18f) }
    }

    /** Four-point star used for the AI identity. */
    private fun sparkle() = outlined("Sparkle") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(13.9f, 9.1f)
            lineTo(20f, 11f)
            lineTo(13.9f, 12.9f)
            lineTo(12f, 19f)
            lineTo(10.1f, 12.9f)
            lineTo(4f, 11f)
            lineTo(10.1f, 9.1f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.5f, 16f)
            lineTo(19.3f, 18.2f)
            lineTo(21.5f, 19f)
            lineTo(19.3f, 19.8f)
            lineTo(18.5f, 22f)
            lineTo(17.7f, 19.8f)
            lineTo(15.5f, 19f)
            lineTo(17.7f, 18.2f)
            close()
        }
    }

    /** Line rising to the top-right with an arrow head. */
    private fun trendUp() = outlined("TrendUp") {
        stroke { moveTo(3f, 17f); lineTo(9f, 11f); lineTo(13f, 15f); lineTo(21f, 7f) }
        stroke { moveTo(15f, 7f); lineTo(21f, 7f); lineTo(21f, 13f) }
    }

    /** A coin with a downward arrow: money kept. */
    private fun savings() = outlined("Savings") {
        stroke {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
            curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            close()
        }
        stroke { moveTo(12f, 8f); lineTo(12f, 16f) }
        stroke { moveTo(9f, 13f); lineTo(12f, 16f); lineTo(15f, 13f) }
    }
}
