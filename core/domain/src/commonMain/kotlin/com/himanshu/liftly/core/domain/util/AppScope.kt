package com.himanshu.liftly.core.domain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide scope for writes that must finish even if the screen that started them goes away
 * (e.g. saving a note while the user navigates back).
 */
class AppScope(scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) : CoroutineScope by scope
