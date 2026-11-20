package com.himanshu.liftly.core.domain.repository

/** Loads or upgrades the bundled exercise library. Safe to call on every launch. */
fun interface ExerciseSeeder {
    suspend fun seedIfNeeded()
}
