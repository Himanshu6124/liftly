package com.himanshu.liftly.core.domain.model

/** Coarse groups for filter chips: one tap instead of picking among 20 muscles. */
enum class MuscleGroup(val muscles: Set<Muscle>) {
    CHEST(setOf(Muscle.CHEST)),
    BACK(setOf(Muscle.LATS, Muscle.UPPER_BACK, Muscle.LOWER_BACK, Muscle.TRAPS)),
    SHOULDERS(setOf(Muscle.FRONT_DELTS, Muscle.SIDE_DELTS, Muscle.REAR_DELTS)),
    ARMS(setOf(Muscle.BICEPS, Muscle.TRICEPS, Muscle.FOREARMS)),
    LEGS(setOf(Muscle.QUADS, Muscle.HAMSTRINGS, Muscle.GLUTES, Muscle.CALVES, Muscle.ADDUCTORS, Muscle.ABDUCTORS)),
    CORE(setOf(Muscle.ABS, Muscle.OBLIQUES)),
}
