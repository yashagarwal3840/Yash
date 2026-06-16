package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WorkoutLog
import com.example.data.WorkoutRepository
import com.example.data.WorkoutSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    // Active session sets (flows directly from Room)
    val activeSets: StateFlow<List<WorkoutSet>> = repository.activeSets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Historical workout logs
    val allLogs: StateFlow<List<WorkoutLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All historic sets for statistics and calculations
    val allHistorySets: StateFlow<List<WorkoutSet>> = repository.allHistorySets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for workout timer
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    // Show celebration dialog state
    private val _celebrationSessionId = MutableStateFlow<Long?>(null)
    val celebrationSessionId: StateFlow<Long?> = _celebrationSessionId.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Auto-detect if there is an active session in progress on app startup
        viewModelScope.launch {
            val list = repository.getActiveSetsList()
            if (list.isNotEmpty()) {
                _isWorkoutActive.value = true
                // Approximate timer start if we resume (e.g. use default or 1 min for continuity)
                _durationSeconds.value = 60 
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _durationSeconds.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun startWorkout() {
        viewModelScope.launch {
            // Check if active sets already exist
            val rawActive = repository.getActiveSetsList()
            if (rawActive.isEmpty()) {
                // Populate with standard full-body routine
                val defaultSets = listOf(
                    // Bench Press (Chest)
                    WorkoutSet(exerciseName = "Bench Press", muscleGroup = "Chest", setNumber = 1, weight = 60.0, reps = 10),
                    WorkoutSet(exerciseName = "Bench Press", muscleGroup = "Chest", setNumber = 2, weight = 60.0, reps = 10),
                    WorkoutSet(exerciseName = "Bench Press", muscleGroup = "Chest", setNumber = 3, weight = 60.0, reps = 8),
                    
                    // Barbell Squat (Legs)
                    WorkoutSet(exerciseName = "Barbell Squat", muscleGroup = "Legs", setNumber = 1, weight = 80.0, reps = 8),
                    WorkoutSet(exerciseName = "Barbell Squat", muscleGroup = "Legs", setNumber = 2, weight = 80.0, reps = 8),
                    WorkoutSet(exerciseName = "Barbell Squat", muscleGroup = "Legs", setNumber = 3, weight = 80.0, reps = 6),
                    
                    // Pull-Up (Back)
                    WorkoutSet(exerciseName = "Pull-Up", muscleGroup = "Back", setNumber = 1, weight = 0.0, reps = 10),
                    WorkoutSet(exerciseName = "Pull-Up", muscleGroup = "Back", setNumber = 2, weight = 0.0, reps = 8),
                    WorkoutSet(exerciseName = "Pull-Up", muscleGroup = "Back", setNumber = 3, weight = 0.0, reps = 6),

                    // Overhead Press (Shoulders)
                    WorkoutSet(exerciseName = "Overhead Press", muscleGroup = "Shoulders", setNumber = 1, weight = 40.0, reps = 10),
                    WorkoutSet(exerciseName = "Overhead Press", muscleGroup = "Shoulders", setNumber = 2, weight = 40.0, reps = 8),
                    WorkoutSet(exerciseName = "Overhead Press", muscleGroup = "Shoulders", setNumber = 3, weight = 40.0, reps = 8),

                    // Dumbbell Curl (Arms)
                    WorkoutSet(exerciseName = "Dumbbell Curl", muscleGroup = "Arms", setNumber = 1, weight = 12.5, reps = 12),
                    WorkoutSet(exerciseName = "Dumbbell Curl", muscleGroup = "Arms", setNumber = 2, weight = 12.5, reps = 12),
                    WorkoutSet(exerciseName = "Dumbbell Curl", muscleGroup = "Arms", setNumber = 3, weight = 12.5, reps = 10)
                )
                repository.insertSets(defaultSets)
            }
            _durationSeconds.value = 0
            _isWorkoutActive.value = true
            startTimer()
        }
    }

    fun addSetToExercise(exerciseName: String, muscleGroup: String) {
        viewModelScope.launch {
            val sets = repository.getActiveSetsList().filter { it.exerciseName == exerciseName }
            val nextSetNum = if (sets.isEmpty()) 1 else sets.maxOf { it.setNumber } + 1
            val lastSet = sets.lastOrNull()
            
            val newSet = WorkoutSet(
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                setNumber = nextSetNum,
                weight = lastSet?.weight ?: 20.0,
                reps = lastSet?.reps ?: 10
            )
            repository.insertSet(newSet)
        }
    }

    fun addCustomExercise(name: String, muscleGroup: String, setsCount: Int) {
        viewModelScope.launch {
            val sets = (1..setsCount).map { setNum ->
                WorkoutSet(
                    exerciseName = name,
                    muscleGroup = muscleGroup,
                    setNumber = setNum,
                    weight = 20.0,
                    reps = 10
                )
            }
            repository.insertSets(sets)
        }
    }

    fun removeSet(setId: Long) {
        viewModelScope.launch {
            repository.deleteSetById(setId)
        }
    }

    fun deleteExercise(exerciseName: String) {
        viewModelScope.launch {
            repository.deleteActiveSetsForExercise(exerciseName)
        }
    }

    fun updateSet(set: WorkoutSet) {
        viewModelScope.launch {
            repository.updateSet(set)
        }
    }

    fun toggleSetCompletion(set: WorkoutSet) {
        viewModelScope.launch {
            repository.updateSet(set.copy(isCompleted = !set.isCompleted))
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            stopTimer()
            val logId = repository.saveWorkoutSession(_durationSeconds.value.toLong())
            if (logId != -1L) {
                _celebrationSessionId.value = logId
            }
            _isWorkoutActive.value = false
            _durationSeconds.value = 0
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            stopTimer()
            repository.clearActiveSets()
            _isWorkoutActive.value = false
            _durationSeconds.value = 0
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteLog(logId)
        }
    }

    fun closeCelebration() {
        _celebrationSessionId.value = null
    }
}

class WorkoutViewModelFactory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
