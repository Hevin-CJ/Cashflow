package com.hevincj.cashflow.utils

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class SmoothNotchedShape(
    private val cornerRadius: Dp = 16.dp,
    private val fabRadius: Dp = 32.dp,
    private val notchPadding: Dp = 6.dp
) : Shape {
    private var cachedOutline: Outline? = null
    private var cachedSize: Size? = null
    private var cachedDensity: Float? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val dpScale = density.density
        if (cachedOutline != null && cachedSize == size && cachedDensity == dpScale) {
            return cachedOutline!!
        }

        val path = Path().apply {
            val width = size.width
            val height = size.height

            val cornerRadPx = with(density) { cornerRadius.toPx() }
            val fabRadPx = with(density) { fabRadius.toPx() }
            val paddingPx = with(density) { notchPadding.toPx() }

            val cutoutRadius = fabRadPx + paddingPx
            val center = width / 2f


            moveTo(0f, cornerRadPx)
            quadraticTo(0f, 0f, cornerRadPx, 0f)

            // Line to the start of the notch
            lineTo(center - cutoutRadius * 1.5f, 0f)

            // Smooth curve DOWN into the cutout
            cubicTo(
                center - cutoutRadius * 0.8f, 0f,
                center - cutoutRadius, cutoutRadius * 1.2f,
                center, cutoutRadius * 1.2f
            )

            // Smooth curve UP out of the cutout
            cubicTo(
                center + cutoutRadius, cutoutRadius * 1.2f,
                center + cutoutRadius * 0.8f, 0f,
                center + cutoutRadius * 1.5f, 0f
            )

            // Line to top-right, applying corner radius
            lineTo(width - cornerRadPx, 0f)
            quadraticTo(width, 0f, width, cornerRadPx)

            // Close the path around the bottom
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        val newOutline = Outline.Generic(path)
        cachedOutline = newOutline
        cachedSize = size
        cachedDensity = dpScale
        return newOutline
    }
}