package dev.breakmode.ui.components

import androidx.compose.material3.Slider
import kotlin.math.roundToInt
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.breakmode.session.AppBucket
import dev.breakmode.session.FocusSchedule
import dev.breakmode.ui.BucketEmblem
import dev.breakmode.ui.glass
import dev.breakmode.ui.formatMinutes

/**
 * Replicates BlockIt's exact schedule/routines interface.
 * Features:
 * - Large segmented digital clock at top showing start and end times
 * - Dotted 24-hour routine timeline bar
 * - Routine tabs (Routine 1, 2, 3) with active accent underline and lock/toggle tile
 * - S M T W T F S day-of-week blocks
 * - Tactile START & END ruler cards with interactive time pickers
 * - Bag assignment & rename support
 */
@Composable
fun RoutinesBlockItView(
    schedules: List<FocusSchedule>,
    buckets: List<AppBucket>,
    onUpdateSchedule: (FocusSchedule) -> Unit,
    onToggleSchedule: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    // Ensure we have at least 3 routines to display the 3-tab layout
    val activeSchedules = remember(schedules) {
        if (schedules.isNotEmpty()) schedules else FocusSchedule.defaultRoutines()
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentSchedule = activeSchedules.getOrNull(selectedIndex) ?: activeSchedules.firstOrNull()

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    if (currentSchedule == null) return

    val startStr = String.format("%02d:%02d", currentSchedule.startHour, currentSchedule.startMinute)
    val endTotalMins = (currentSchedule.startHour * 60 + currentSchedule.startMinute + currentSchedule.durationMinutes) % (24 * 60)
    val endHour = endTotalMins / 60
    val endMinute = endTotalMins % 60
    val endStr = String.format("%02d:%02d", endHour, endMinute)

    val activeBucketName = buckets.firstOrNull { it.id == currentSchedule.bucketId }?.name ?: "All apps"

    val startMinutes = currentSchedule.startHour * 60 + currentSchedule.startMinute
    fun updateTime(start: Int, end: Int) {
        val duration = ((end - start + 1440) % 1440).coerceAtLeast(5)
        onUpdateSchedule(currentSchedule.copy(startHour = start / 60, startMinute = start % 60, durationMinutes = duration))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Schedule", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().glass(24.dp, active = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BucketEmblem(currentSchedule.bucketId, Modifier.size(54.dp))
                    Text(currentSchedule.name, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = startStr,
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (currentSchedule.enabled) colors.primary else colors.onSurfaceVariant,
                    )
                    Text(
                        text = " – ",
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            fontFeatureSettings = "tnum",
                        ),
                        color = colors.outline,
                    )
                    Text(
                        text = endStr,
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (currentSchedule.enabled) colors.primary else colors.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // Dotted Timeline Bar (48 slots for 24 hours of the day)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val startSlot = (currentSchedule.startHour * 2 + currentSchedule.startMinute / 30) % 48
                    val endSlot = (startSlot + currentSchedule.durationMinutes / 30)
                    for (slot in 0 until 48) {
                        val isActive = if (endSlot <= 48) {
                            slot in startSlot until endSlot
                        } else {
                            slot >= startSlot || slot < (endSlot % 48)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    when {
                                        isActive && currentSchedule.enabled -> colors.primary
                                        isActive -> colors.onSurfaceVariant
                                        else -> colors.surfaceVariant
                                    },
                                ),
                        )
                    }
                }
            }
        }

        // 2. ROUTINE TABS ROW (ROUTINE 1 / BEDTIME, ROUTINE 2, ROUTINE 3 + Lock/Toggle Square)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Routine 1, 2, 3 tabs
            activeSchedules.take(3).forEachIndexed { index, schedule ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) colors.surface else Color.Transparent)
                        .clickable { selectedIndex = index }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = schedule.name.uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 1.sp,
                        ),
                        color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = String.format("%02d:%02d", schedule.startHour, schedule.startMinute),
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    // Active Underline Bar (BlockIt style)
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) colors.primary else Color.Transparent),
                    )
                }
            }

            // Lock / Enable Toggle Square on the right
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (currentSchedule.enabled) colors.primary else colors.surfaceVariant,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleSchedule(currentSchedule.id, !currentSchedule.enabled) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (currentSchedule.enabled) "Turn routine off" else "Turn routine on",
                        tint = if (currentSchedule.enabled) colors.onPrimary else colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // 3. DAY-OF-WEEK BLOCK SELECTOR (S M T W T F S)
        DayOfWeekSelector(
            selectedDays = currentSchedule.daysOfWeek,
            onToggleDay = { day ->
                val newDays = if (day in currentSchedule.daysOfWeek) {
                    if (currentSchedule.daysOfWeek.size > 1) currentSchedule.daysOfWeek - day else currentSchedule.daysOfWeek
                } else {
                    currentSchedule.daysOfWeek + day
                }
                onUpdateSchedule(currentSchedule.copy(daysOfWeek = newDays))
            },
            activeColor = colors.primary,
        )

        // 4. DURATION & STATUS INDICATOR
        Text(
            text = if (currentSchedule.enabled) {
                "Active · ${formatMinutes(currentSchedule.durationMinutes)}"
            } else {
                "Off · ${formatMinutes(currentSchedule.durationMinutes)}"
            },
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            ),
            color = if (currentSchedule.enabled) colors.primary else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // 5. TACTILE START & END RULER CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // START CARD
            RulerTimeCard(
                title = "START",
                minutes = startMinutes,
                routineId = currentSchedule.id,
                onCommit = { updateTime(it, endTotalMins) },
                modifier = Modifier.weight(1f),
            )

            // END CARD
            RulerTimeCard(
                title = "END",
                minutes = endTotalMins,
                routineId = currentSchedule.id,
                onCommit = { updateTime(startMinutes, it) },
                modifier = Modifier.weight(1f),
            )
        }

        // 6. ROUTINE RENAME & BAG ASSIGNMENT ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Rename Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        renameInput = currentSchedule.name
                        showRenameDialog = true
                    },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Rename: ${currentSchedule.name}",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                        color = colors.onSurface,
                    )
                }
            }

            // Bag Selector
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                buckets.forEach { b ->
                    FilterChip(
                        selected = currentSchedule.bucketId == b.id,
                        onClick = { onUpdateSchedule(currentSchedule.copy(bucketId = b.id)) },
                        label = { Text(b.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary.copy(alpha = 0.2f),
                            selectedLabelColor = colors.primary,
                        ),
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Routine") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Routine Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onUpdateSchedule(currentSchedule.copy(name = renameInput.trim()))
                        }
                        showRenameDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Tactile ruler tick card matching BlockIt's START and END pickers.
 */
@Composable
private fun RulerTimeCard(
    title: String,
    minutes: Int,
    routineId: String,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var draft by remember(routineId, minutes) { mutableIntStateOf(minutes) }
    Column(modifier.glass(18.dp).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = {
            TimePickerDialog(context, { _, h, m -> onCommit(h * 60 + m) },
                minutes / 60, minutes % 60, DateFormat.is24HourFormat(context)).show()
        }) {
            Text(String.format("%02d:%02d", draft / 60, draft % 60), style = MaterialTheme.typography.titleLarge)
        }
        Slider(
            value = draft.toFloat(),
            onValueChange = { draft = it.roundToInt() },
            onValueChangeFinished = { onCommit(draft) },
            valueRange = 0f..1439f,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title time in minutes after midnight" },
        )
    }
}
