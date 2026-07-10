package org.jetbrains.jewel.intui.standalone.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.focused
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.intui.core.theme.IntUiDarkTheme
import org.jetbrains.jewel.ui.theme.JewelStyles
import org.jetbrains.jewel.ui.theme.colors

@OptIn(ExperimentalFoundationStyleApi::class)
public object IntUiThemeStyles : JewelStyles {
    override val defaultButton: Style = Style {
        // Colors
        val gray1 = SolidColor(IntUiDarkTheme.colors.gray(1))
        val gray14 = IntUiDarkTheme.colors.gray(14)
        val blue6 = SolidColor(IntUiDarkTheme.colors.blue(6))

        LocalLayoutDirection.currentValue

        val focusedBorder = gray1
        val border = blue6

        background(blue6)
        contentColor(gray14)
        borderBrush(border)

        disabled {
            background(SolidColor(Color.Unspecified))
            contentColor(IntUiDarkTheme.colors.gray(6))
            borderBrush(SolidColor(IntUiDarkTheme.colors.gray(4)))
        }

        focused {
            background(blue6)
            contentColor(gray14)
            borderBrush(focusedBorder)

            dropShadow(Shadow(color = colors.outlines.focused, offset = DpOffset.Zero, radius = 0.dp, spread = 1.5.dp))

            hovered {
                background(SolidColor(IntUiDarkTheme.colors.blue(5)))
                contentColor(gray14)
            }
        }

        pressed {
            background(SolidColor(IntUiDarkTheme.colors.blue(4)))
            contentColor(gray14)
            borderBrush(focusedBorder)
        }

        // Metrics
        shape(RoundedCornerShape(CornerSize(4.dp)))
        contentPaddingVertical(6.dp)
        contentPaddingHorizontal(12.dp)
        minSize(72.dp, 28.dp)
        borderWidth(1.dp)
    }
}
