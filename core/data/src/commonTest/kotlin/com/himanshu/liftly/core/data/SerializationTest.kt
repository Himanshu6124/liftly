package com.himanshu.liftly.core.data

import com.himanshu.liftly.core.data.db.Converters
import com.himanshu.liftly.core.data.seed.SeedFile
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.Muscle
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runs on every target: Kotlin/Native needs serializers that the JVM finds reflectively. */
class SerializationTest {
    @Test
    fun seedJsonParses() {
        val file = Json.decodeFromString(
            SeedFile.serializer(),
            """{"version":1,"exercises":[{"seedKey":"bench","name":"Bench","primaryMuscle":"CHEST",
               "secondaryMuscles":["TRICEPS"],"equipment":"BARBELL","trackingType":"WEIGHT_REPS","mechanic":"COMPOUND"}]}""",
        )
        val bench = file.exercises.single()
        assertEquals(Muscle.CHEST, bench.primaryMuscle)
        assertEquals(listOf(Muscle.TRICEPS), bench.secondaryMuscles)
        assertEquals(Equipment.BARBELL, bench.equipment)
    }

    @Test
    fun roomConvertersRoundTrip() {
        val c = Converters()
        val muscles = listOf(Muscle.LATS, Muscle.BICEPS)
        assertEquals(muscles, c.jsonToMuscles(c.musclesToJson(muscles)))
        assertEquals(listOf("a", "b"), c.jsonToStrings(c.stringsToJson(listOf("a", "b"))))
    }
}
