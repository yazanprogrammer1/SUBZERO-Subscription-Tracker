package com.subzero.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4pt spacing grid. Use these instead of ad-hoc dp values. */
@Immutable
data class SubzeroSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp,
    /** Horizontal screen gutter. */
    val screen: Dp = 20.dp,
)

internal val LocalSubzeroSpacing = staticCompositionLocalOf { SubzeroSpacing() }
