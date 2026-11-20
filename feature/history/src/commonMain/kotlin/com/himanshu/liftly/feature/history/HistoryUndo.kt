package com.himanshu.liftly.feature.history

import com.himanshu.liftly.core.domain.model.DeletedWorkout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the last deleted workout so the History list can offer Undo even when the delete
 * happened on the detail screen (which closes right away).
 */
class HistoryUndo {
    private val _pending = MutableStateFlow<DeletedWorkout?>(null)
    val pending: StateFlow<DeletedWorkout?> = _pending

    fun offer(deleted: DeletedWorkout) {
        _pending.value = deleted
    }

    fun take(): DeletedWorkout? = _pending.value.also { _pending.value = null }
}
