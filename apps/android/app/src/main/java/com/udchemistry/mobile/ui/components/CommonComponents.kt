package com.udchemistry.mobile.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.udchemistry.mobile.model.DashboardMetric
import com.udchemistry.mobile.model.QrCodeRecord
import com.udchemistry.mobile.model.TrendPoint
import com.udchemistry.mobile.ui.theme.AppAccent
import com.udchemistry.mobile.ui.theme.AppBackground
import com.udchemistry.mobile.ui.theme.AppBlueGlow
import com.udchemistry.mobile.ui.theme.AppDanger
import com.udchemistry.mobile.ui.theme.AppDivider
import com.udchemistry.mobile.ui.theme.AppGlass
import com.udchemistry.mobile.ui.theme.AppGlassStrong
import com.udchemistry.mobile.ui.theme.AppMutedText
import com.udchemistry.mobile.ui.theme.AppOrangeGlow
import com.udchemistry.mobile.ui.theme.AppPrimary
import com.udchemistry.mobile.ui.theme.AppPurpleGlow
import com.udchemistry.mobile.ui.theme.AppSuccess
import com.udchemistry.mobile.ui.theme.AppSuccessSoft
import com.udchemistry.mobile.util.QrCodeUtils

@Composable
fun AppGradientBackground(content: @Composable () -> Unit) {
    val backgroundColor = AppBackground
    val purpleGlow = AppPurpleGlow
    val blueGlow = AppBlueGlow
    val orangeGlow = AppOrangeGlow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080B15),
                        Color(0xFF0B1020),
                        backgroundColor,
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(purpleGlow.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.18f),
                    radius = size.minDimension * 0.55f,
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(blueGlow.copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.14f),
                    radius = size.minDimension * 0.62f,
                ),
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(orangeGlow.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.82f),
                    radius = size.minDimension * 0.50f,
                ),
            )
        }
        content()
    }
}

@Composable
fun AdaptiveContentFrame(
    modifier: Modifier = Modifier,
    maxWidth: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = maxWidth),
        ) {
            content()
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    glowColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        if (glowColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .blur(22.dp)
                    .background(glowColor.copy(alpha = 0.18f), shape),
            )
        }

        Surface(
            shape = shape,
            color = AppGlass.copy(alpha = 0.86f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 20.dp,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                AppGlassStrong.copy(alpha = 0.92f),
                            ),
                        ),
                    )
                    .padding(contentPadding),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!description.isNullOrBlank()) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = AppMutedText)
        }
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageHeroCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = AppPrimary,
    highlights: List<String> = emptyList(),
) {
    GlassPanel(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(22.dp),
        glowColor = accentColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(0.7.dp, accentColor.copy(alpha = 0.34f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = description,
                    color = AppMutedText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (highlights.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                highlights.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(0.7.dp, Color.White.copy(alpha = 0.12f)),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = AppMutedText,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(metric: DashboardMetric, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, glowColor = AppPrimary) {
        Text(metric.label, color = AppMutedText, style = MaterialTheme.typography.labelLarge)
        Text(metric.value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(metric.hint, style = MaterialTheme.typography.bodySmall, color = AppMutedText)
    }
}

@Composable
fun StatusChip(label: String, positive: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (positive) AppSuccessSoft else AppDanger.copy(alpha = 0.16f),
        border = BorderStroke(0.6.dp, if (positive) AppSuccess.copy(alpha = 0.28f) else AppDanger.copy(alpha = 0.28f)),
        contentColor = if (positive) AppSuccess else AppDanger,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun EmptyState(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(0.7.dp, AppDivider),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = message, color = AppMutedText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun TrendChart(
    title: String,
    subtitle: String,
    points: List<TrendPoint>,
    barMode: Boolean = false,
) {
    val dividerColor = AppDivider
    val barStartColor = AppBlueGlow

    SectionCard(title = title, description = subtitle) {
        if (points.isEmpty()) {
            EmptyState("No data available yet.")
            return@SectionCard
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val maxValue = (points.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)
            val leftPadding = 24.dp.toPx()
            val rightPadding = 10.dp.toPx()
            val bottomPadding = 30.dp.toPx()
            val topPadding = 18.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - bottomPadding - topPadding
            val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
            val linePath = Path()
            val fillPath = Path()

            drawLine(
                color = dividerColor,
                start = Offset(leftPadding, size.height - bottomPadding),
                end = Offset(size.width - rightPadding, size.height - bottomPadding),
                strokeWidth = 1.5.dp.toPx(),
            )

            if (barMode) {
                points.forEachIndexed { index, point ->
                    val x = leftPadding + (chartWidth / points.size) * index + 6.dp.toPx()
                    val barWidth = (chartWidth / points.size) - 12.dp.toPx()
                    val barHeight = ((point.value / maxValue) * chartHeight).toFloat()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barStartColor, AppPrimary),
                        ),
                        topLeft = Offset(x, size.height - bottomPadding - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    )
                }
            } else {
                points.forEachIndexed { index, point ->
                    val x = leftPadding + stepX * index
                    val y = size.height - bottomPadding - ((point.value / maxValue) * chartHeight).toFloat()
                    if (index == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, size.height - bottomPadding)
                        fillPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                val lastX = leftPadding + stepX * (points.lastIndex.coerceAtLeast(0))
                fillPath.lineTo(lastX, size.height - bottomPadding)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(AppPrimary.copy(alpha = 0.32f), Color.Transparent),
                        startY = topPadding,
                        endY = size.height - bottomPadding,
                    ),
                )
                drawPath(path = linePath, color = AppPrimary, style = Stroke(width = 4.dp.toPx()))

                points.forEachIndexed { index, point ->
                    val x = leftPadding + stepX * index
                    val y = size.height - bottomPadding - ((point.value / maxValue) * chartHeight).toFloat()
                    drawCircle(color = AppPrimary, radius = 5.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            sampleAxisLabels(points).forEach { point ->
                Text(point.label, color = AppMutedText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun QrPreviewDialog(
    qrCode: QrCodeRecord?,
    studentName: String,
    onDismiss: () -> Unit,
) {
    if (qrCode == null) return

    val context = LocalContext.current
    val imageBitmap = remember(qrCode.qrImageUrl, qrCode.qrData) {
        QrCodeUtils.decodeDataUrl(qrCode.qrImageUrl)
            ?: QrCodeUtils.decodeDataUrl(QrCodeUtils.createPngDataUrl(qrCode.qrData))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppGlassStrong,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        confirmButton = {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, qrCode.qrLink.toUri()).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                },
            ) {
                Text("Open page")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text(studentName) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Student QR",
                        modifier = Modifier
                            .size(220.dp)
                            .border(1.dp, AppDivider, RoundedCornerShape(22.dp))
                            .padding(14.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                Text(
                    "Students can scan this QR image to open the class check-in page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppMutedText,
                )
            }
        },
    )
}

@Composable
fun ClickableInfoRow(
    headline: String,
    supporting: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = AppMutedText)
            }
            trailing?.invoke()
        }
    }
}

private fun sampleAxisLabels(points: List<TrendPoint>): List<TrendPoint> {
    return when {
        points.isEmpty() -> emptyList()
        points.size <= 3 -> points
        else -> listOf(points.first(), points[points.lastIndex / 2], points.last())
    }
}
