package com.himanshu.liftly.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Things the app can be asked to do from outside: shortcuts, Quick Actions, widgets, Live Activity. */
enum class AppAction(val url: String) {
    START_NEXT_WORKOUT("liftly://start-next-workout"),
    OPEN_WORKOUT("liftly://workout"),
}

/**
 * The single entry point for links into the app. Platform code (Android intents, iOS URL and Quick
 * Action handlers) calls [handle]; the app shell consumes [pending] once it is ready, so a link that
 * arrives before the first composition (cold start) isn't lost.
 */
object DeepLinks {
    const val SCHEME = "liftly"

    private val _pending = MutableStateFlow<AppAction?>(null)
    val pending: StateFlow<AppAction?> = _pending.asStateFlow()

    /** Returns false (and does nothing) for links Liftly doesn't know. */
    fun handle(url: String): Boolean {
        val action = parse(url) ?: return false
        _pending.value = action
        return true
    }

    fun consume(action: AppAction) {
        _pending.compareAndSet(action, null)
    }

    fun parse(url: String): AppAction? {
        val normalized = url.trim().trimEnd('/').lowercase()
        return AppAction.entries.firstOrNull { it.url == normalized }
    }
}
