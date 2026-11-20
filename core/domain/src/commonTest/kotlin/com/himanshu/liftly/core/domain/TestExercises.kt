package com.himanshu.liftly.core.domain

import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Exercise
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.TrackingType

fun exercise(
    name: String,
    aliases: List<String> = emptyList(),
    primary: Muscle = Muscle.CHEST,
    equipment: Equipment = Equipment.BARBELL,
    id: String = name,
) = Exercise(
    id = id, seedKey = null, name = name, aliases = aliases, primaryMuscle = primary, secondaryMuscles = emptyList(),
    equipment = equipment, trackingType = TrackingType.WEIGHT_REPS, mechanic = null, difficulty = null,
    imagePaths = emptyList(), videoUrl = null, instructions = emptyList(), formCues = emptyList(),
    commonMistakes = emptyList(), userNote = null, isFavorite = false, isCustom = false, isArchived = false,
)
