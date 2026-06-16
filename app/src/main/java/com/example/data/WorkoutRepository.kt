package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {
    val activeSets: Flow<List<WorkoutSet>> = workoutDao.getActiveSetsFlow()
    val allLogs: Flow<List<WorkoutLog>> = workoutDao.getAllLogsFlow()
    val allHistorySets: Flow<List<WorkoutSet>> = workoutDao.getAllHistorySetsFlow()

    suspend fun getActiveSetsList(): List<WorkoutSet> = workoutDao.getActiveSets()

    suspend fun insertSet(set: WorkoutSet) = workoutDao.insertSet(set)

    suspend fun insertSets(sets: List<WorkoutSet>) = workoutDao.insertSets(sets)

    suspend fun updateSet(set: WorkoutSet) = workoutDao.updateSet(set)

    suspend fun deleteSetById(setId: Long) = workoutDao.deleteSetById(setId)

    suspend fun deleteActiveSetsForExercise(exerciseName: String) = 
        workoutDao.deleteActiveSetsForExercise(exerciseName)

    suspend fun clearActiveSets() = workoutDao.clearActiveSets()

    suspend fun saveWorkoutSession(durationSeconds: Long): Long {
        val active = getActiveSetsList()
        val completedSets = active.filter { it.isCompleted }
        if (completedSets.isEmpty()) return -1

        val totalSetsCompleted = completedSets.size
        val totalWeight = completedSets.sumOf { it.weight * it.reps }

        val log = WorkoutLog(
            durationSeconds = durationSeconds,
            totalSets = totalSetsCompleted,
            totalWeight = totalWeight
        )

        val logId = workoutDao.insertLog(log)
        // Clean up active sets:
        // Delete all uncompleted active sets
        workoutDao.deleteUncompletedActiveSets()
        // Link completed ones to this logId
        workoutDao.assignActiveSetsToLog(logId)
        return logId
    }

    suspend fun deleteLog(logId: Long) {
        workoutDao.deleteSetsForLog(logId)
        workoutDao.deleteLogById(logId)
    }
}
