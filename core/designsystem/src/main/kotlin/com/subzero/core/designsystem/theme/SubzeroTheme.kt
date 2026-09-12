package com.subzero.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root theme. Provides SUBZERO tokens and also configures [MaterialTheme] so any Material
 * component we do use (e.g. date pickers) inherits the same palette.
 */
@Composable
fun SubzeroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = subzeroTypography()

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.secondaryAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.outline,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.secondaryAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceElevated,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.outline,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalSubzeroColors provides colors,
        LocalSubzeroTypography provides typography,
        LocalSubzeroSpacing provides SubzeroSpacing(),
        LocalSubzeroShapes provides SubzeroShapes(),
    ) {
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}

/** Access point for SUBZERO design tokens inside composables. */
object SubzeroTheme {
    val colors: SubzeroColors
        @Composable @ReadOnlyComposable get() = LocalSubzeroColors.current

    val typography: SubzeroTypography
        @Composable @ReadOnlyComposable get() = LocalSubzeroTypography.current

    val spacing: SubzeroSpacing
        @Composable @ReadOnlyComposable get() = LocalSubzeroSpacing.current

    val shapes: SubzeroShapes
        @Composable @ReadOnlyComposable get() = LocalSubzeroShapes.current
}
