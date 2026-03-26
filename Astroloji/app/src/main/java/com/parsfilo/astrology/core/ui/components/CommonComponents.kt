package com.parsfilo.astrology.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parsfilo.astrology.R
import com.parsfilo.astrology.core.util.ZodiacSign

@Composable
fun AstrologyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
            ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun CosmicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background,
                            ),
                    ),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    Color.Transparent,
                                ),
                            center =
                                androidx.compose.ui.geometry
                                    .Offset(180f, 120f),
                            radius = 900f,
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                            center =
                                androidx.compose.ui.geometry
                                    .Offset(820f, 340f),
                            radius = 720f,
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.48f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0.38f),
                        ),
                    ),
        )
        content()
    }
}

@Composable
fun PremiumLockedOverlay(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                Text(
                    text = stringResource(R.string.home_premium_badge),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun AstroSectionTitle(
    title: String,
    eyebrow: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        eyebrow?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
fun StreakBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        ),
                    ),
                ).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "🔥", style = MaterialTheme.typography.bodyLarge)
        Text(
            text =
                androidx.compose.ui.res
                    .stringResource(R.string.streak_badge_format, count),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun ZodiacChip(
    sign: ZodiacSign,
    language: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        if (selected) {
            Brush.linearGradient(listOf(sign.element.color, sign.element.color.copy(alpha = 0.65f)))
        } else {
            Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface),
            )
        }
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .background(background)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = sign.symbol, style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = sign.localizedName(language),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = sign.dateRange,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun BlurredPremiumBlock(content: @Composable () -> Unit) {
    Box {
        Box(modifier = Modifier.blur(10.dp)) { content() }
    }
}

@Composable
fun DetailChip(
    text: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    AssistChip(
        onClick = {},
        modifier = modifier,
        enabled = false,
        border = null,
        colors =
            AssistChipDefaults.assistChipColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            ),
        label = {
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        },
        leadingIcon =
            leadingContent?.let { content ->
                {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                    )
                }
            },
    )
}
