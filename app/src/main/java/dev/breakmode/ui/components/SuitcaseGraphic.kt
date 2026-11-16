package dev.breakmode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breakmode.session.AppBucket

/**
 * Renders the 3 stylized suitcases inspired by BlockIt's "The Suitcase / Bags" selector.
 * Highlights the active bag and displays app slot indicators under each bag.
 */
@Composable
fun SuitcasesRow(
    buckets: List<AppBucket>,
    selectedBucketId: String?,
    onSelectBucket: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF8FE3C8),
    inactiveColor: Color = Color(0xFF2C2C32),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141417))
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buckets.forEachIndexed { index, bucket ->
            val isSelected = bucket.id == selectedBucketId
            val bagColor = if (isSelected) activeColor else inactiveColor

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectBucket(bucket.id) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                // Suitcase graphic
                SuitcaseIcon(
                    color = bagColor,
                    appCount = bucket.appPackages.size,
                    maxApps = AppBucket.MAX_APPS_PER_BUCKET,
                    isSelected = isSelected,
                )

                Spacer(Modifier.height(8.dp))

                // Bag label
                Text(
                    text = bucket.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeColor else Color(0xFF888890),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
fun SuitcaseIcon(
    color: Color,
    appCount: Int,
    maxApps: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        // Suitcase handle (top)
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                .background(color),
        )
        // Little gap under handle
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(3.dp)
                .background(Color.Transparent),
        )

        // Suitcase Body (tri-block shape like BlockIt)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Left wing
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 2.dp))
                    .background(color),
            )
            // Center main column
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(color),
            ) {
                // Inner latch detail
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                        .width(10.dp)
                        .height(3.dp)
                        .background(Color(0xFF141417)),
                )
            }
            // Right wing
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 2.dp))
                    .background(color),
            )
        }

        Spacer(Modifier.height(6.dp))

        // App slots dots/squares below suitcase
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (i in 0 until maxApps) {
                val isFilled = i < appCount
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            when {
                                isFilled -> color
                                isSelected -> color.copy(alpha = 0.3f)
                                else -> Color(0xFF38383E)
                            },
                        ),
                )
            }
        }
    }
}
