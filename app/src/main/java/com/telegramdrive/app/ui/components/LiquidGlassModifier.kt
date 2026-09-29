package com.telegramdrive.app.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.styropyr0.prismal.PrismalBackdrop
import com.styropyr0.prismal.depth.PrismalDepthInset
import com.styropyr0.prismal.specular.PrismalSpecular
import com.styropyr0.prismal.specular.PrismalSpecularStyle
import com.styropyr0.prismal.drawPrismalGlass
import com.styropyr0.prismal.prismalGlassEffects
import com.styropyr0.prismal.shapes.PrismalCapsule
import com.styropyr0.prismal.shapes.PrismalRoundedRectangle
import com.styropyr0.prismal.shapes.PrismalRoundedRectangularShape

/**
 * Shared finishing layers that make every glass surface read as one material:
 * a crisp lit rim (specular), inner thickness shading (depth inset) and a faint
 * frost lift on the surface. Stable instances so modifier equality checks stay cheap.
 */
object KeepMeGlass {
    val specular: () -> PrismalSpecular? = {
        PrismalSpecular(
            width = 1.2f.dp,
            blurRadius = 0.6f.dp,
            alpha = 0.9f,
            style = PrismalSpecularStyle.Default(
                color = Color.White.copy(alpha = 0.7f),
                angle = 45f,
                falloff = 1.2f
            )
        )
    }
    val depthInset: () -> PrismalDepthInset? = {
        PrismalDepthInset(radius = 8.dp, alpha = 0.3f)
    }
    val surface: DrawScope.() -> Unit = {
        // Faint frost lift so the liquid glass reads against the near-black canvas
        drawRect(Color.White.copy(alpha = 0.07f))
    }
}

/**
 * Standardized KeepMe V3 Liquid Glass material modifier.
 * Uses the exact optical recipe from FullScreenViewerScreen's Media Details Sheet:
 * shadow + drawPrismalGlass with canonical prismalGlassEffects(density).
 * Zero artificial dark backgrounds or fake borders.
 */
@Composable
fun Modifier.tdriveLiquidGlass(
    backdrop: PrismalBackdrop,
    cornerRadius: Dp = 28.dp,
    shadowElevation: Dp = 12.dp,
    isCapsule: Boolean = false
): Modifier {
    val density = LocalDensity.current
    return tdriveLiquidGlass(
        backdrop = backdrop,
        density = density,
        cornerRadius = cornerRadius,
        shadowElevation = shadowElevation,
        isCapsule = isCapsule
    )
}

fun Modifier.tdriveLiquidGlass(
    backdrop: PrismalBackdrop,
    density: Density,
    cornerRadius: Dp = 28.dp,
    shadowElevation: Dp = 12.dp,
    isCapsule: Boolean = false
): Modifier {
    val prismalShape: () -> PrismalRoundedRectangularShape = if (isCapsule) {
        { PrismalCapsule() }
    } else {
        { PrismalRoundedRectangle(cornerRadius) }
    }
    val shadowShape: Shape = if (isCapsule) CircleShape else RoundedCornerShape(cornerRadius)

    return this
        .shadow(shadowElevation, shadowShape)
        .drawPrismalGlass(
            backdrop = backdrop,
            shape = prismalShape,
            effects = prismalGlassEffects(density),
            specular = KeepMeGlass.specular,
            depthInset = KeepMeGlass.depthInset,
            onDrawSurface = KeepMeGlass.surface
        )
}
