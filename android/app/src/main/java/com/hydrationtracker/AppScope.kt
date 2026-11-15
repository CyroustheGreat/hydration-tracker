package com.hydrationtracker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppScope {
    val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
