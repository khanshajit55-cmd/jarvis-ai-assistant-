package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.model.AssistantState
import com.jarvis.assistant.ui.theme.JarvisCyan
import com.jarvis.assistant.ui.theme.JarvisCyanDim
import com.jarvis.assistant.ui.theme.JarvisError
import com.jarvis.assistant.ui.theme.JarvisSpeakingWave
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceOrb(
    state: AssistantState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JarvisOrbTransition")

    // Continuous smooth rotation for the outer cyber rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Reverse rotation for concentric inner reticle
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbReverseRotation"
    )

    // Core breathing pulsation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.LISTENING -> 600
                    AssistantState.PROCESSING -> 400
                    AssistantState.SPEAKING -> 800
                    AssistantState.ERROR -> 300
                    AssistantState.IDLE -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val primaryColor = when (state) {
        AssistantState.IDLE -> JarvisCyan
        AssistantState.LISTENING -> JarvisCyan
        AssistantState.PROCESSING -> Color(0xFFFFFFFF)
        AssistantState.SPEAKING -> JarvisSpeakingWave
        AssistantState.ERROR -> JarvisError
    }

    val secondaryColor = when (state) {
        AssistantState.IDLE -> JarvisCyanDim
        AssistantState.LISTENING -> JarvisCyanDim
        AssistantState.PROCESSING -> JarvisCyan
        AssistantState.SPEAKING -> Color(0xFF00E676)
        AssistantState.ERROR -> Color(0xFFB71C1C)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.42f
            val dynamicBoost = (audioRms.coerceIn(0f, 10f) / 10f) * 20f

            // 1. Outermost Ambient Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.22f * glowAlpha),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 2.2f + dynamicBoost
                ),
                center = center,
                radius = baseRadius * 2.2f + dynamicBoost
            )

            // 2. Rotating Outer Segmented Cyber Ring
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    center = center,
                    radius = baseRadius * 1.55f,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(30f, 18f, 10f, 18f),
                            phase = 0f
                        ),
                        cap = StrokeCap.Round
                    )
                )

                // 4 Orbiting satellite pips
                for (i in 0 until 4) {
                    val angleRad = (i * 90f) * (PI / 180f).toFloat()
                    val pipX = center.x + (baseRadius * 1.55f) * cos(angleRad)
                    val pipY = center.y + (baseRadius * 1.55f) * sin(angleRad)
                    drawCircle(
                        color = primaryColor,
                        center = Offset(pipX, pipY),
                        radius = 3.dp.toPx()
                    )
                }
            }

            // 3. Counter-rotating Reticle Ring
            rotate(reverseRotation, pivot = center) {
                drawCircle(
                    color = secondaryColor.copy(alpha = 0.45f),
                    center = center,
                    radius = baseRadius * 1.28f + (dynamicBoost * 0.5f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(50f, 25f, 15f, 25f),
                            phase = 10f
                        )
                    )
                )
            }

            // 4. Acoustic Waveform Arcs when Listening or Speaking
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                for (arcIndex in 0 until 6) {
                    val angleOffset = arcIndex * 60f + (rotationAngle * 0.5f)
                    val sweep = 35f + (dynamicBoost * 2f).coerceAtMost(30f)
                    drawArc(
                        color = primaryColor.copy(alpha = 0.7f),
                        startAngle = angleOffset,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - baseRadius * 1.15f, center.y - baseRadius * 1.15f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.3f, baseRadius * 2.3f),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 5. Pulsating Glowing Energy Core
            val activeRadius = (baseRadius * pulseScale) + dynamicBoost
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = activeRadius
                ),
                center = center,
                radius = activeRadius
            )

            // 6. Crisp Core Boundary
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                center = center,
                radius = activeRadius * 0.45f
            )
        }
    }
}