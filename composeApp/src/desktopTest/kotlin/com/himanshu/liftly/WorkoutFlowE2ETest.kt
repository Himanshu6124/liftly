package com.himanshu.liftly

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.himanshu.liftly.core.data.db.AppDatabase
import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.InputMode
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.repository.ExerciseSeeder
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.repository.WorkoutRepository
import com.himanshu.liftly.di.liftlyModules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The MVP's core loop through the real UI, ViewModels and Room (in memory):
 * start an empty workout → add 2 exercises → log one in the sheet → quick-log the other
 * → finish → the workout is in History.
 */
@OptIn(ExperimentalTestApi::class)
class WorkoutFlowE2ETest {

    private val workouts: WorkoutRepository get() = KoinPlatform.getKoin().get()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                liftlyModules + module {
                    single<AppDatabase> {
                        Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
                    }
                    single<SettingsRepository> { InMemorySettings() }
                },
            )
        }
        runBlocking {
            KoinPlatform.getKoin().get<ExerciseSeeder>().seedIfNeeded()
            // A finished past session of Bench Press, so it can be quick-logged ("same as last time").
            val id = workouts.createWorkout("Push", routineId = null, exerciseIds = listOf(BENCH_ID))
            val we = workouts.observeActiveWorkout().first()!!.exercises.single().workoutExercise
            workouts.logSets(we.id, List(3) { DraftSet(weightKg = 60.0, reps = 8) }, InputMode.UNIFORM)
            workouts.finish(id)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun startAddLogQuickLogFinishShowsInHistory() = runComposeUiTest {
        setContent { App() }

        clickText("Start empty workout")

        // Add Barbell Curl (never done) and Bench Press (done before).
        clickText("Add exercise")
        waitFor(hasText("Add exercises"))
        pickExercise("Barbell Curl")
        pickExercise("Bench Press")
        clickText("Add (2)")

        // Log Barbell Curl through the Batch Log Sheet.
        waitFor(card("Barbell Curl"))
        onAllNodes(card("Barbell Curl")).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        waitFor(hasText("First time: enter what you did"))
        waitFor(hasText("Barbell Curl") and !hasClickAction()) // sheet header: pre-fill loaded
        onNode(hasSetTextAction() and hasText("Weight", substring = true)).performTextInput("20")
        onNode(hasSetTextAction() and hasText("Reps per set", substring = true)).performTextInput("10 10 8")
        clickText("Save")
        waitFor(loggedCard("Barbell Curl"))

        // Quick-log Bench Press (the screen-reader equivalent of swiping right).
        onAllNodes(card("Bench Press")).onFirst().performCustomAccessibilityActionWithLabel("Log same as last time")
        waitFor(loggedCard("Bench Press"))

        clickText("Finish")
        waitFor(hasText("Workout complete"))
        clickText("Done")

        clickText("History")
        waitFor(hasText("2 exercises", substring = true))

        val history = runBlocking { workouts.observeHistory().first() }
        assertEquals(2, history.size)
        with(history.first()) {
            assertEquals(2, loggedCount)
            assertEquals(6, workingSets) // 3 curl sets + 3 bench sets copied from last time
            assertEquals(20.0 * 28 + 60.0 * 8 * 3, volumeKg)
        }
    }

    private fun ComposeUiTest.waitFor(matcher: SemanticsMatcher) =
        try {
            waitUntil(timeoutMillis = TIMEOUT_MS) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            println(onAllNodes(isRoot()).printToString(maxDepth = 60)) // semantics tree in the test report
            throw e
        }

    private fun ComposeUiTest.clickText(text: String) {
        val matcher = hasText(text) and hasClickAction() and !hasSetTextAction()
        waitFor(matcher)
        // Semantics click: works for buttons a sheet has pushed below the test window.
        onAllNodes(matcher).onFirst().performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun ComposeUiTest.pickExercise(name: String) {
        onNode(hasSetTextAction() and hasText("Search exercises", substring = true)).performTextInput(name)
        clickText(name)
        onNode(hasSetTextAction() and hasText(name)).performTextClearance()
    }

    private fun card(name: String) = hasText(name) and hasClickAction() and hasContentDescription("Not logged yet")

    private fun loggedCard(name: String) = hasText(name) and hasClickAction() and hasContentDescription("Logged")

    private class InMemorySettings : SettingsRepository {
        private val state = MutableStateFlow(UserSettings())
        private var seed = 0
        override val settings: Flow<UserSettings> = state
        override suspend fun update(transform: (UserSettings) -> UserSettings) {
            state.value = transform(state.value)
        }
        override suspend fun seedVersion() = seed
        override suspend fun setSeedVersion(version: Int) {
            seed = version
        }
    }

    private companion object {
        const val BENCH_ID = "seed:bench_press"
        const val TIMEOUT_MS = 10_000L
    }
}
