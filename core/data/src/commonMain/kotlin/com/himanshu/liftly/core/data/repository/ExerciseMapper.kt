package com.himanshu.liftly.core.data.repository

import com.himanshu.liftly.core.data.db.entity.ExerciseEntity
import com.himanshu.liftly.core.domain.model.Exercise

internal fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    seedKey = seedKey,
    name = name,
    aliases = aliases,
    primaryMuscle = primaryMuscle,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    trackingType = trackingType,
    mechanic = mechanic,
    difficulty = difficulty,
    imagePaths = imagePaths,
    videoUrl = videoUrl,
    instructions = instructions,
    formCues = formCues,
    commonMistakes = commonMistakes,
    userNote = userNote,
    isFavorite = isFavorite,
    isCustom = isCustom,
    isArchived = isArchived,
    restSeconds = restSeconds,
)
