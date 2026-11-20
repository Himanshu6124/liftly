package com.himanshu.liftly.core.domain.usecase

import com.himanshu.liftly.core.domain.model.DraftSet
import com.himanshu.liftly.core.domain.model.PrType
import com.himanshu.liftly.core.domain.model.SetType
import com.himanshu.liftly.core.domain.util.Strength

/** A record set by one session; [setIndex] points into the sets being saved. */
data class PrHit(val type: PrType, val value: Double, val setIndex: Int)

/**
 * Compares a session's sets with every earlier session of the same exercise.
 * - Only working sets count (warm-ups are excluded on both sides).
 * - A record must beat the previous best strictly; a tie is not a PR.
 * - The very first session sets no PRs (nothing to beat yet).
 * Editing a session re-runs detection against history that excludes that session, so lowering
 * the numbers removes the PR.
 */
class DetectPersonalRecordsUseCase {
    operator fun invoke(history: List<DraftSet>, session: List<DraftSet>): List<PrHit> {
        val past = history.working()
        if (past.isEmpty()) return emptyList()
        val now = session.withIndex().filter { it.value.isWorking() }
        if (now.isEmpty()) return emptyList()

        val hits = mutableListOf<PrHit>()
        fun check(type: PrType, best: Double?, candidates: List<Pair<Int, Double>>) {
            val top = candidates.maxByOrNull { it.second } ?: return
            if (best == null || top.second > best + EPSILON) hits += PrHit(type, top.second, top.first)
        }

        check(
            PrType.MAX_WEIGHT,
            past.mapNotNull { it.weightKg }.maxOrNull(),
            now.mapNotNull { (i, s) -> s.weightKg?.let { i to it } },
        )
        check(
            PrType.EST_1RM,
            past.mapNotNull(Strength::estimatedOneRepMax).maxOrNull(),
            now.mapNotNull { (i, s) -> Strength.estimatedOneRepMax(s)?.let { i to it } },
        )
        check(
            PrType.MAX_REPS,
            past.mapNotNull { it.reps?.toDouble() }.maxOrNull(),
            now.mapNotNull { (i, s) -> s.reps?.let { i to it.toDouble() } },
        )
        // Volume is per session, so compare against the best earlier session total.
        val sessionVolume = Strength.volume(session)
        if (sessionVolume > 0) {
            val bestVolume = history.groupBySession().maxOfOrNull { Strength.volume(it) } ?: 0.0
            if (sessionVolume > bestVolume + EPSILON) hits += PrHit(PrType.MAX_VOLUME, sessionVolume, now.first().index)
        }
        return hits
    }

    private fun List<DraftSet>.working() = filter { it.isWorking() }

    private fun DraftSet.isWorking() = setType != SetType.WARMUP && (reps != null || durationSec != null || distanceM != null)

    /** History arrives flattened; [DraftSet] has no session id, so callers pass sessions via [SessionHistory]. */
    private fun List<DraftSet>.groupBySession(): List<List<DraftSet>> = (this as? SessionHistory)?.sessions ?: listOf(this)

    private companion object {
        const val EPSILON = 1e-9
    }
}

/** Earlier sessions of one exercise, keeping per-session grouping for the volume record. */
class SessionHistory(val sessions: List<List<DraftSet>>) : List<DraftSet> by sessions.flatten()
