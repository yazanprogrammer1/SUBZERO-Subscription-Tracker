package com.subzero.core.common.coroutines

import javax.inject.Qualifier

/** Qualifies an injected [kotlinx.coroutines.CoroutineDispatcher] so tests can swap it. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: SubzeroDispatcher)

enum class SubzeroDispatcher {
    /** Disk and database work. */
    IO,

    /** CPU-bound work such as insight evaluation over many subscriptions. */
    Default,
}
