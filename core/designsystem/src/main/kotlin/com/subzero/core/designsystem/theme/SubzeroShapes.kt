package com.subzero.core.designsystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Consistent corner radii. Cards use [md]/[lg]; sheets use [xl]; chips and pills use [full]. */
@Immutable
data class SubzeroShapes(
    val sm: CornerBasedShape = RoundedCornerShape(10.dp),
    val md: CornerBasedShape = RoundedCornerShape(16.dp),
    val lg: CornerBasedShape = RoundedCornerShape(22.dp),
    val xl: CornerBasedShape = RoundedCornerShape(28.dp),
    val full: CornerBasedShape = RoundedCornerShape(percent = 50),
)

internal val LocalSubzeroShapes = staticCompositionLocalOf { SubzeroShapes() }
