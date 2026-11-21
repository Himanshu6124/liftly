package com.himanshu.liftly.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class Equipment { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT, KETTLEBELL, BAND, EZ_BAR, SMITH_MACHINE, OTHER }

@Serializable
enum class TrackingType { WEIGHT_REPS, REPS_ONLY, TIME, DISTANCE }

@Serializable
enum class Mechanic { COMPOUND, ISOLATION }

@Serializable
enum class Difficulty { BEGINNER, INTERMEDIATE, EXPERT }

@Serializable
enum class ExerciseStatus { PENDING, LOGGED, SKIPPED }

@Serializable
enum class InputMode { UNIFORM, PER_SET }

@Serializable
enum class SetType { NORMAL, WARMUP, DROP, FAILURE }

@Serializable
enum class PrType { EST_1RM, MAX_WEIGHT, MAX_REPS, MAX_VOLUME }

@Serializable
enum class WeightUnit { KG, LB }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What a measurement's value means; values are stored in kg, cm or %. */
enum class MeasureKind { WEIGHT, LENGTH, PERCENT }

@Serializable
enum class MeasurementType(val kind: MeasureKind) {
    BODY_WEIGHT(MeasureKind.WEIGHT),
    BODY_FAT(MeasureKind.PERCENT),
    WAIST(MeasureKind.LENGTH),
    CHEST(MeasureKind.LENGTH),
    HIPS(MeasureKind.LENGTH),
    ARMS(MeasureKind.LENGTH),
    THIGHS(MeasureKind.LENGTH),
    NECK(MeasureKind.LENGTH),
}
