package com.himanshu.liftly.core.designsystem.component

import androidx.compose.runtime.Composable
import com.himanshu.liftly.core.designsystem.resources.Res
import com.himanshu.liftly.core.designsystem.resources.equipment_band
import com.himanshu.liftly.core.designsystem.resources.equipment_barbell
import com.himanshu.liftly.core.designsystem.resources.equipment_bodyweight
import com.himanshu.liftly.core.designsystem.resources.equipment_cable
import com.himanshu.liftly.core.designsystem.resources.equipment_dumbbell
import com.himanshu.liftly.core.designsystem.resources.equipment_ez_bar
import com.himanshu.liftly.core.designsystem.resources.equipment_kettlebell
import com.himanshu.liftly.core.designsystem.resources.equipment_machine
import com.himanshu.liftly.core.designsystem.resources.equipment_other
import com.himanshu.liftly.core.designsystem.resources.equipment_smith_machine
import com.himanshu.liftly.core.designsystem.resources.group_arms
import com.himanshu.liftly.core.designsystem.resources.group_back
import com.himanshu.liftly.core.designsystem.resources.group_chest
import com.himanshu.liftly.core.designsystem.resources.group_core
import com.himanshu.liftly.core.designsystem.resources.group_legs
import com.himanshu.liftly.core.designsystem.resources.group_shoulders
import com.himanshu.liftly.core.designsystem.resources.muscle_abductors
import com.himanshu.liftly.core.designsystem.resources.muscle_abs
import com.himanshu.liftly.core.designsystem.resources.muscle_adductors
import com.himanshu.liftly.core.designsystem.resources.muscle_biceps
import com.himanshu.liftly.core.designsystem.resources.muscle_calves
import com.himanshu.liftly.core.designsystem.resources.muscle_chest
import com.himanshu.liftly.core.designsystem.resources.muscle_forearms
import com.himanshu.liftly.core.designsystem.resources.muscle_front_delts
import com.himanshu.liftly.core.designsystem.resources.muscle_glutes
import com.himanshu.liftly.core.designsystem.resources.muscle_hamstrings
import com.himanshu.liftly.core.designsystem.resources.muscle_lats
import com.himanshu.liftly.core.designsystem.resources.muscle_lower_back
import com.himanshu.liftly.core.designsystem.resources.muscle_neck
import com.himanshu.liftly.core.designsystem.resources.muscle_obliques
import com.himanshu.liftly.core.designsystem.resources.muscle_quads
import com.himanshu.liftly.core.designsystem.resources.muscle_rear_delts
import com.himanshu.liftly.core.designsystem.resources.muscle_side_delts
import com.himanshu.liftly.core.designsystem.resources.muscle_traps
import com.himanshu.liftly.core.designsystem.resources.muscle_triceps
import com.himanshu.liftly.core.designsystem.resources.muscle_upper_back
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import com.himanshu.liftly.core.domain.model.MuscleGroup
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun Muscle.label(): String = stringResource(labelRes())

@Composable
fun Equipment.label(): String = stringResource(labelRes())

@Composable
fun MuscleGroup.label(): String = stringResource(labelRes())

fun Muscle.labelRes(): StringResource = when (this) {
    Muscle.CHEST -> Res.string.muscle_chest
    Muscle.FRONT_DELTS -> Res.string.muscle_front_delts
    Muscle.SIDE_DELTS -> Res.string.muscle_side_delts
    Muscle.REAR_DELTS -> Res.string.muscle_rear_delts
    Muscle.BICEPS -> Res.string.muscle_biceps
    Muscle.TRICEPS -> Res.string.muscle_triceps
    Muscle.FOREARMS -> Res.string.muscle_forearms
    Muscle.ABS -> Res.string.muscle_abs
    Muscle.OBLIQUES -> Res.string.muscle_obliques
    Muscle.LATS -> Res.string.muscle_lats
    Muscle.TRAPS -> Res.string.muscle_traps
    Muscle.UPPER_BACK -> Res.string.muscle_upper_back
    Muscle.LOWER_BACK -> Res.string.muscle_lower_back
    Muscle.GLUTES -> Res.string.muscle_glutes
    Muscle.QUADS -> Res.string.muscle_quads
    Muscle.HAMSTRINGS -> Res.string.muscle_hamstrings
    Muscle.ADDUCTORS -> Res.string.muscle_adductors
    Muscle.ABDUCTORS -> Res.string.muscle_abductors
    Muscle.CALVES -> Res.string.muscle_calves
    Muscle.NECK -> Res.string.muscle_neck
}

fun Equipment.labelRes(): StringResource = when (this) {
    Equipment.BARBELL -> Res.string.equipment_barbell
    Equipment.DUMBBELL -> Res.string.equipment_dumbbell
    Equipment.MACHINE -> Res.string.equipment_machine
    Equipment.CABLE -> Res.string.equipment_cable
    Equipment.BODYWEIGHT -> Res.string.equipment_bodyweight
    Equipment.KETTLEBELL -> Res.string.equipment_kettlebell
    Equipment.BAND -> Res.string.equipment_band
    Equipment.EZ_BAR -> Res.string.equipment_ez_bar
    Equipment.SMITH_MACHINE -> Res.string.equipment_smith_machine
    Equipment.OTHER -> Res.string.equipment_other
}

fun MuscleGroup.labelRes(): StringResource = when (this) {
    MuscleGroup.CHEST -> Res.string.group_chest
    MuscleGroup.BACK -> Res.string.group_back
    MuscleGroup.SHOULDERS -> Res.string.group_shoulders
    MuscleGroup.ARMS -> Res.string.group_arms
    MuscleGroup.LEGS -> Res.string.group_legs
    MuscleGroup.CORE -> Res.string.group_core
}
