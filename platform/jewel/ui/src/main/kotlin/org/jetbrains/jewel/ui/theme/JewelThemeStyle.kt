package org.jetbrains.jewel.ui.theme

import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.jewel.foundation.GlobalColors
import org.jetbrains.jewel.foundation.GlobalMetrics
import org.jetbrains.jewel.foundation.LocalGlobalColors
import org.jetbrains.jewel.foundation.LocalGlobalMetrics
import org.jetbrains.jewel.foundation.theme.JewelTheme

@OptIn(ExperimentalFoundationStyleApi::class)
public interface JewelStyles {
    public val defaultButton: Style
}

@OptIn(ExperimentalFoundationStyleApi::class)
public val StyleScope.colors: GlobalColors
    get() = LocalGlobalColors.currentValue

@OptIn(ExperimentalFoundationStyleApi::class)
public val StyleScope.metrics: GlobalMetrics
    get() = LocalGlobalMetrics.currentValue

public val LocalJewelStyles: ProvidableCompositionLocal<JewelStyles> = staticCompositionLocalOf {
    error("No JewelStyles provided. Have you forgotten the theme?") // TODO: change this error.
}

@OptIn(ExperimentalFoundationStyleApi::class)
public val StyleScope.jewelStyles: JewelStyles
    get() = LocalJewelStyles.currentValue

public val JewelTheme.Companion.styles: JewelStyles
    @Composable @ReadOnlyComposable get() = LocalJewelStyles.current
