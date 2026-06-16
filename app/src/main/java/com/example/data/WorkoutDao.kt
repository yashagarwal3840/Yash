package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // --- Active Sets Queries (workoutLogId is NULL) ---
    @Query("SELECT * FROM workout_sets WHERE workoutLogId IS NULL ORDER BY timestamp ASC")
    fun getActiveSetsFlow(): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE workoutLogId IS NULL ORDER BY timestamp ASC")
    suspend fun getActiveSets(): List<WorkoutSet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSet>)

    @Update
    suspend fun updateSet(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSetById(setId: Long)

    @Query("DELETE FROM workout_sets WHERE workoutLogId IS NULL AND exerciseName = :exerciseName")
    suspend fun deleteActiveSetsForExercise(exerciseName: String)

    @Query("DELETE FROM workout_sets WHERE workoutLogId IS NULL")
    suspend fun clearActiveSets()

    @Query("DELETE FROM workout_sets WHERE workoutLogId IS NULL AND isCompleted = 0")
    suspend fun deleteUncompletedActiveSets()

    @Query("UPDATE workout_sets SET workoutLogId = :logId WHERE workoutLogId IS NULL")
    suspend fun assignActiveSetsToLog(logId: Long)

    // --- Workout Logs (Saved Sessions) ---
    @Query("SELECT * FROM workout_logs ORDER BY dateMillis DESC")
    fun getAllLogsFlow(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WorkoutLog): Long

    @Query("DELETE FROM workout_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)

    @Query("DELETE FROM workout_sets WHERE workoutLogId = :logId")
    suspend fun deleteSetsForLog(logId: Long)

    // --- History Sets ---
    @Query("SELECT * FROM workout_sets WHERE workoutLogId = :logId ORDER BY timestamp ASC")
    fun getSetsForLogFlow(logId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE workoutLogId IS NOT NULL ORDER BY timestamp ASC")
    fun getAllHistorySetsFlow(): Flow<List<WorkoutSet>>
}
