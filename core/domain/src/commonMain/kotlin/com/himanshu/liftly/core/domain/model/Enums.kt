package com.himanshu.liftly.core.domain.model

enum class Muscle {
    CHEST,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    ABS,
    OBLIQUES,
    LATS,
    TRAPS,
    UPPER_BACK,
    LOWER_BACK,
    GLUTES,
    QUADS,
    HAMSTRINGS,
    ADDUCTORS,
    ABDUCTORS,
    CALVES,
    NECK,
}

enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, EZ_BAR, SMITH_MACHINE, OTHER }

enum class TrackingType { WEIGHT_REPS, REPS_ONLY, TIME, DISTANCE }

enum class Mechanic { COMPOUND, ISOLATION }

enum class Difficulty { BEGINNER, INTERMEDIATE, EXPERT }

enum class ExerciseStatus { PENDING, LOGGED, SKIPPED }

enum class InputMode { UNIFORM, PER_SET }

enum class SetType { NORMAL, WARMUP, DROP, FAILURE }

enum class PrType { EST_1RM, MAX_WEIGHT, MAX_REPS, MAX_VOLUME }

enum class WeightUnit { KG, LB }

enum class ThemeMode { SYSTEM, LIGHT, DARK }
