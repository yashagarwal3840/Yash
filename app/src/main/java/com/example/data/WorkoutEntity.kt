package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalSets: Int = 0,
    val totalWeight: Double = 0.0
)

@Entity(tableName = "workout_sets")
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutLogId: Long? = null, // null space means active session sets
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
