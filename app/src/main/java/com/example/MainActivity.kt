package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.WorkoutLog
import com.example.data.WorkoutRepository
import com.example.data.WorkoutSet
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WorkoutViewModel
import com.example.viewmodel.WorkoutViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GymAppContent()
            }
        }
    }
}

@Composable
fun GymAppContent() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { WorkoutRepository(db.workoutDao()) }
    val viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModelFactory(repository))

    // Collect variables from ViewModel
    val activeSets by viewModel.activeSets.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val allHistorySets by viewModel.allHistorySets.collectAsStateWithLifecycle()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val celebrationSessionId by viewModel.celebrationSessionId.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showCancelConfirmationDialog by remember { mutableStateOf(false) }

    // Map history sets grouped by workout log ID for nested drill-down
    val groupedHistorySets = remember(allHistorySets) {
        allHistorySets.groupBy { it.workoutLogId }
    }

    // Keep track of which historical log IDs are expanded in the list
    var expandedLogIds by remember { mutableStateOf(setOf<Long>()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "APEX ENERGY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Iron Forge",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Streak and total workouts stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔥", fontSize = 16.sp)
                                Text(
                                    text = "${calculateStreak(allLogs)} Days",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🏋️", fontSize = 16.sp)
                                Text(
                                    text = "${allLogs.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isWorkoutActive) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Progress description
                        val completedSets = activeSets.count { it.isCompleted }
                        val totalSets = activeSets.size
                        val progressPercent = if (totalSets > 0) completedSets.toFloat() / totalSets else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Progress",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$completedSets / $totalSets Sets Done (${(progressPercent * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { progressPercent },
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Cancel button
                            OutlinedButton(
                                onClick = { showCancelConfirmationDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("app_cancel_workout_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                                Spacer(Modifier.width(4.dp))
                                Text("Discard", fontWeight = FontWeight.SemiBold)
                            }

                            // Finish button (requires at least one set completed to save)
                            val canFinish = activeSets.any { it.isCompleted }
                            Button(
                                onClick = { viewModel.finishWorkout() },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("app_finish_workout_button"),
                                enabled = canFinish,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Finish")
                                Spacer(Modifier.width(4.dp))
                                Text("Complete Workout", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TIMER AND ACTIVE TRACKING BANNER
            if (isWorkoutActive) {
                item {
                    ActiveTimerBanner(durationSeconds = durationSeconds)
                }

                // GROUPED EXERCISES LIST
                val exerciseGroups = activeSets.groupBy { it.exerciseName }
                if (exerciseGroups.isNotEmpty()) {
                    items(exerciseGroups.keys.toList()) { exerciseName ->
                        val sets = exerciseGroups[exerciseName] ?: emptyList()
                        val muscleGroup = sets.firstOrNull()?.muscleGroup ?: "Cardio"
                        ActiveExerciseCard(
                            exerciseName = exerciseName,
                            muscleGroup = muscleGroup,
                            sets = sets,
                            onUpdateSet = { viewModel.updateSet(it) },
                            onToggleComplete = { viewModel.toggleSetCompletion(it) },
                            onAddSet = { viewModel.addSetToExercise(exerciseName, muscleGroup) },
                            onRemoveSet = { viewModel.removeSet(it) },
                            onDeleteExercise = { viewModel.deleteExercise(exerciseName) }
                        )
                    }
                }

                // ADD EXTRA EXERCISE CONTROLLER DURING SESSON
                item {
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("app_add_custom_exercise_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                        Spacer(Modifier.width(6.dp))
                        Text("Add Custom Exercise / Lift", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // INACTIVE STATE: INITIAL WORKOUT CONTROLLER
                item {
                    InactiveWorkoutBanner(
                        onResumeWorkout = { viewModel.startWorkout() },
                        workoutCount = allLogs.size
                    )
                }

                // RECENT HISTORY HEADER
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WORKOUT HISTORY",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (allLogs.isNotEmpty()) {
                            Text(
                                text = "${allLogs.size} logs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // EMPTY HISTORY PLACEHOLDER
                if (allLogs.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💪", fontSize = 36.sp)
                                Text(
                                    text = "Ready to Begin Your Journey?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap 'Start Dynamic Session' to activate your program, log lifts and log progress locally with absolute ease.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    // HISTORY LIST ITEMS WITH EXPANSION DRILL-DOWN
                    items(allLogs) { log ->
                        val logSets = groupedHistorySets[log.id] ?: emptyList()
                        val isExpanded = expandedLogIds.contains(log.id)

                        HistoryLogCard(
                            log = log,
                            sets = logSets,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedLogIds = if (isExpanded) {
                                    expandedLogIds - log.id
                                } else {
                                    expandedLogIds + log.id
                                }
                            },
                            onDelete = { viewModel.deleteLog(log.id) }
                        )
                    }
                }
            }

            // SPACE AT THE BOTTOM
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // CELEBRATION CONGRATS OVERLAY
    if (celebrationSessionId != null) {
        val statsId = celebrationSessionId!!
        val matchingLog = allLogs.find { it.id == statsId }
        val matchingSets = groupedHistorySets[statsId] ?: emptyList()

        CelebrationDialog(
            matchingLog = matchingLog,
            sets = matchingSets,
            onDismiss = { viewModel.closeCelebration() }
        )
    }

    // ADD EXERCISE SELECTOR DIALOG
    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { name, group, sets ->
                viewModel.addCustomExercise(name, group, sets)
                showAddExerciseDialog = false
                Toast.makeText(context, "$name added to active list!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // CANCEL CONFIRMATION DIALOG
    if (showCancelConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmationDialog = false },
            title = { Text("Discard Active Session?", fontWeight = FontWeight.Bold) },
            text = { Text("This will abandon your active lift logs and wipe current progress. Are you sure you want to discard your workout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelWorkout()
                        showCancelConfirmationDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard Workout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmationDialog = false }) {
                    Text("Keep Sweating", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// Calculates consecutive active days workout streak based on historical workout logs
fun calculateStreak(logs: List<WorkoutLog>): Int {
    if (logs.isEmpty()) return 0
    val logDates = logs.map {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.dateMillis
        // Strip out hours/minutes
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.distinct().sortedDescending()

    var streak = 0
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    var compareDate = today
    val oneDayMillis = 24 * 60 * 60 * 1000L

    // If no workout today, check if they worked out yesterday to maintain the streak
    if (!logDates.contains(compareDate)) {
        compareDate -= oneDayMillis
        if (!logDates.contains(compareDate)) {
            return 0
        }
    }

    for (date in logDates) {
        if (date == compareDate) {
            streak++
            compareDate -= oneDayMillis
        } else if (date < compareDate) {
            // Streak broken
            break
        }
    }
    return streak
}

@Composable
fun ActiveTimerBanner(durationSeconds: Int) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing Green LED indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF3DDC84), CircleShape)
                )
                Text(
                    text = "ACTIVE SESSION",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InactiveWorkoutBanner(
    onResumeWorkout: () -> Unit,
    workoutCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TODAY'S WORKOUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Full-Body Strength Routine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "A core structural workout: squatted push/pull compound pairs with localized arm finishes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }

            // Muscle group target chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Chest", "Back", "Legs", "Shoulders", "Arms").forEach { muscle ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = muscle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onResumeWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("app_start_workout_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Active Workout")
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (workoutCount > 0) "START DYNAMIC SESSION" else "BEGIN INITIAL WORKOUT",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ActiveExerciseCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<WorkoutSet>,
    onUpdateSet: (WorkoutSet) -> Unit,
    onToggleComplete: (WorkoutSet) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onDeleteExercise: () -> Unit
) {
    var isDeleteConfirmVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // EXERCISE HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = muscleGroup.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { isDeleteConfirmVisible = !isDeleteConfirmVisible },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = if (isDeleteConfirmVisible) Icons.Default.Close else Icons.Default.Delete,
                        contentDescription = "Remove exercise templates"
                    )
                }
            }

            // Exercise delete warning confirmation
            AnimatedVisibility(visible = isDeleteConfirmVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remove exercise?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            onDeleteExercise()
                            isDeleteConfirmVisible = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Delete", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // REPS AND WEIGHT LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "SET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    "WEIGHT (KG)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(100.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(90.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "DONE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sets.forEach { setItem ->
                    SetRowItem(
                        set = setItem,
                        onUpdateSet = onUpdateSet,
                        onToggleComplete = { onToggleComplete(setItem) },
                        onRemoveSet = { onRemoveSet(setItem.id) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ADD SET TRIGGER
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.align(Alignment.Start),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Set")
                Spacer(Modifier.width(4.dp))
                Text("Add Set", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SetRowItem(
    set: WorkoutSet,
    onUpdateSet: (WorkoutSet) -> Unit,
    onToggleComplete: () -> Unit,
    onRemoveSet: () -> Unit
) {
    // Add long-press delete or hover hints for polished tactile feel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                if (set.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(36.dp)
        ) {
            IconButton(
                onClick = onRemoveSet,
                modifier = Modifier.size(16.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete set", modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (set.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        // Weight control panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.width(110.dp)
        ) {
            AdjustButton(
                text = "-",
                onClick = {
                    if (set.weight > 0) {
                        val nextWeight = maxOf(0.0, set.weight - 2.5)
                        onUpdateSet(set.copy(weight = nextWeight))
                    }
                }
            )

            Text(
                text = if (set.weight % 1.0 == 0.0) "${set.weight.toInt()}" else "${set.weight}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )

            AdjustButton(
                text = "+",
                onClick = {
                    onUpdateSet(set.copy(weight = set.weight + 2.5))
                }
            )
        }

        // Reps control panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.width(100.dp)
        ) {
            AdjustButton(
                text = "-",
                onClick = {
                    if (set.reps > 1) {
                        onUpdateSet(set.copy(reps = set.reps - 1))
                    }
                }
            )

            Text(
                text = "${set.reps}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )

            AdjustButton(
                text = "+",
                onClick = {
                    onUpdateSet(set.copy(reps = set.reps + 1))
                }
            )
        }

        // Satisfaction checkbox
        Box(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (set.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.dp,
                        if (set.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (set.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed set indicator",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistoryLogCard(
    log: WorkoutLog,
    sets: List<WorkoutSet>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("EEEE, MMM dd • h:mm a", Locale.getDefault()) }
    val readableDate = remember(log.dateMillis) { formatter.format(Date(log.dateMillis)) }

    // Parse duration details
    val durationMinutes = log.durationSeconds / 60
    val durationSeconds = log.durationSeconds % 60
    val durationString = if (durationMinutes > 0) "${durationMinutes}m ${durationSeconds}s" else "${durationSeconds}s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = readableDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Full-Body Forge Completed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete log history", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // STATS ROW FOR THIS CARD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsMiniIndicator(emoji = "⏱️", label = "Time", value = durationString)
                StatsMiniIndicator(emoji = "📊", label = "Sets Logged", value = "${log.totalSets} completed")
                
                val weightFormatted = if (log.totalWeight % 1.0 == 0.0) "${log.totalWeight.toInt()}" else String.format("%.1f", log.totalWeight)
                StatsMiniIndicator(emoji = "⚡", label = "Total Volume", value = "$weightFormatted kg")
            }

            // DRILL DOWN SECTION (EXPANDABLE) SHOWING ACTUAL PERFORMANCE
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "COMPLETED SET DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    val exerciseGroups = sets.groupBy { it.exerciseName }
                    if (exerciseGroups.isEmpty()) {
                        Text(
                            "No set details captured for this log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        exerciseGroups.forEach { (exerciseName, setList) ->
                            Column {
                                Text(
                                    text = exerciseName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                // Map individual reps/weights representation
                                val representation = setList.joinToString(", ") { s ->
                                    val wStr = if (s.weight % 1.0 == 0.0) "${s.weight.toInt()}" else "${s.weight}"
                                    "${wStr}kg x ${s.reps}"
                                }
                                Text(
                                    text = representation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    lineHeight = 16.sp
                                )
                            }
                            if (setList != exerciseGroups.values.lastOrNull()) {
                                Divider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Tap to collapse details" else "Tap to expand details",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StatsMiniIndicator(
    emoji: String,
    label: String,
    value: String
) {
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 12.sp)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CelebrationDialog(
    matchingLog: WorkoutLog?,
    sets: List<WorkoutSet>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🔥💪🏆", fontSize = 48.sp)
                Text(
                    text = "SESSION COMPLETED!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Congratulations! You forged local lift history. Your muscles have been successfully stressed and stimulated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                if (matchingLog != null) {
                    val minutes = matchingLog.durationSeconds / 60
                    val seconds = matchingLog.durationSeconds % 60
                    val durStr = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CelebrationStatRow(label = "Workout Time", value = durStr)
                        CelebrationStatRow(label = "Total Sets Finished", value = "${matchingLog.totalSets} sets")

                        val weightFormatted = if (matchingLog.totalWeight % 1.0 == 0.0) "${matchingLog.totalWeight.toInt()}" else String.format("%.1f", matchingLog.totalWeight)
                        CelebrationStatRow(label = "Estimated Volume Injected", value = "$weightFormatted kg")
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("app_close_celebration_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("LOCK IT IN", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
fun CelebrationStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf("Chest") }
    var setsCount by remember { mutableStateOf(3) }
    var errorText by remember { mutableStateOf("") }

    val muscleGroups = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Custom Lift",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotEmpty()) errorText = ""
                    },
                    label = { Text("Exercise Name (e.g., Incline DB Press)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                if (errorText.isNotEmpty()) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Muscle target category chips selector
                Text(
                    "Target Muscle Group",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Grid layout of muscle groups
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            muscleGroups.take(3).forEach { group ->
                                MuscleChip(
                                    group = group,
                                    selected = selectedMuscleGroup == group,
                                    onSelect = { selectedMuscleGroup = group }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            muscleGroups.drop(3).forEach { group ->
                                MuscleChip(
                                    group = group,
                                    selected = selectedMuscleGroup == group,
                                    onSelect = { selectedMuscleGroup = group }
                                )
                            }
                        }
                    }
                }

                // Sets adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Starting Sets Count", fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdjustButton(text = "-", onClick = { if (setsCount > 1) setsCount-- })
                        Text("$setsCount", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        AdjustButton(text = "+", onClick = { if (setsCount < 10) setsCount++ })
                    }
                }

                // Confirm / Actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Exercise name cannot be empty."
                            } else {
                                onAdd(name.trim(), selectedMuscleGroup, setsCount)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Inject Exercise", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleChip(
    group: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = group,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
