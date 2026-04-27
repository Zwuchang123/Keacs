package com.keacs.app.ui.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.keacs.app.ui.theme.KeacsColors

@Composable
internal fun WelcomeIllustration(modifier: Modifier = Modifier) {
    val lineColor = KeacsColors.TextSecondary
    val weakLineColor = KeacsColors.TextTertiary
    val paperColor = KeacsColors.Surface
    val accentColor = KeacsColors.Primary

    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thinStroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        val groundY = h * 0.86f

        drawLine(weakLineColor, Offset(w * 0.12f, groundY), Offset(w * 0.9f, groundY), strokeWidth = 1.dp.toPx())

        // 用线稿还原参考图里的账本、笔和小物件，避免引入额外图片资源。
        drawRoundRect(
            color = paperColor,
            topLeft = Offset(w * 0.26f, h * 0.25f),
            size = Size(w * 0.26f, h * 0.48f),
            cornerRadius = CornerRadius(10.dp.toPx()),
        )
        drawRoundRect(
            color = paperColor,
            topLeft = Offset(w * 0.50f, h * 0.25f),
            size = Size(w * 0.25f, h * 0.48f),
            cornerRadius = CornerRadius(10.dp.toPx()),
        )
        drawRoundRect(lineColor, Offset(w * 0.24f, h * 0.28f), Size(w * 0.53f, h * 0.49f), CornerRadius(8.dp.toPx()), style = stroke)
        drawLine(lineColor, Offset(w * 0.50f, h * 0.25f), Offset(w * 0.50f, h * 0.77f), strokeWidth = 1.5.dp.toPx())

        repeat(6) { index ->
            val y = h * (0.36f + index * 0.065f)
            drawLine(weakLineColor, Offset(w * 0.31f, y), Offset(w * 0.45f, y + 4.dp.toPx()), strokeWidth = 1.dp.toPx())
        }
        drawLine(weakLineColor, Offset(w * 0.57f, h * 0.60f), Offset(w * 0.69f, h * 0.60f), strokeWidth = 1.2.dp.toPx())
        drawLine(weakLineColor, Offset(w * 0.56f, h * 0.66f), Offset(w * 0.70f, h * 0.66f), strokeWidth = 1.2.dp.toPx())
        drawLine(accentColor, Offset(w * 0.62f, h * 0.36f), Offset(w * 0.62f, h * 0.51f), strokeWidth = 2.dp.toPx())
        drawLine(accentColor, Offset(w * 0.58f, h * 0.38f), Offset(w * 0.62f, h * 0.44f), strokeWidth = 2.dp.toPx())
        drawLine(accentColor, Offset(w * 0.66f, h * 0.38f), Offset(w * 0.62f, h * 0.44f), strokeWidth = 2.dp.toPx())
        drawLine(accentColor, Offset(w * 0.57f, h * 0.46f), Offset(w * 0.67f, h * 0.46f), strokeWidth = 1.6.dp.toPx())

        val pen = Path().apply {
            moveTo(w * 0.75f, h * 0.72f)
            lineTo(w * 0.84f, h * 0.32f)
            lineTo(w * 0.88f, h * 0.34f)
            lineTo(w * 0.79f, h * 0.74f)
            close()
        }
        drawPath(pen, paperColor)
        drawPath(pen, lineColor, style = stroke)
        drawLine(lineColor, Offset(w * 0.82f, h * 0.42f), Offset(w * 0.86f, h * 0.44f), strokeWidth = 2.dp.toPx())

        drawRoundRect(lineColor, Offset(w * 0.84f, h * 0.53f), Size(w * 0.12f, h * 0.26f), CornerRadius(6.dp.toPx()), style = stroke)
        repeat(3) { row ->
            repeat(3) { col ->
                drawRoundRect(
                    weakLineColor,
                    Offset(w * (0.86f + col * 0.035f), h * (0.63f + row * 0.052f)),
                    Size(8.dp.toPx(), 7.dp.toPx()),
                    CornerRadius(2.dp.toPx()),
                    style = thinStroke,
                )
            }
        }
        drawRoundRect(weakLineColor, Offset(w * 0.86f, h * 0.56f), Size(w * 0.075f, h * 0.035f), CornerRadius(2.dp.toPx()), style = thinStroke)

        drawRoundRect(lineColor, Offset(w * 0.08f, h * 0.63f), Size(w * 0.12f, h * 0.16f), CornerRadius(5.dp.toPx()), style = stroke)
        drawLine(lineColor, Offset(w * 0.14f, h * 0.63f), Offset(w * 0.14f, h * 0.42f), strokeWidth = 1.5.dp.toPx())
        drawPath(Path().apply {
            moveTo(w * 0.14f, h * 0.50f)
            cubicTo(w * 0.05f, h * 0.42f, w * 0.06f, h * 0.61f, w * 0.14f, h * 0.58f)
        }, lineColor, style = stroke)
        drawPath(Path().apply {
            moveTo(w * 0.15f, h * 0.50f)
            cubicTo(w * 0.24f, h * 0.41f, w * 0.23f, h * 0.61f, w * 0.15f, h * 0.58f)
        }, lineColor, style = stroke)

        drawCircle(weakLineColor, radius = 4.dp.toPx(), center = Offset(w * 0.23f, h * 0.08f), style = thinStroke)
        drawCircle(weakLineColor, radius = 3.dp.toPx(), center = Offset(w * 0.68f, h * 0.10f), style = thinStroke)
        drawLine(weakLineColor, Offset(w * 0.48f, h * 0.17f), Offset(w * 0.52f, h * 0.17f), strokeWidth = 1.3.dp.toPx())
        drawLine(weakLineColor, Offset(w * 0.50f, h * 0.15f), Offset(w * 0.50f, h * 0.19f), strokeWidth = 1.3.dp.toPx())
    }
}
