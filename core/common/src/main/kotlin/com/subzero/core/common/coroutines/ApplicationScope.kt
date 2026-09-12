package com.subzero.core.common.coroutines

import javax.inject.Qualifier

/**
 * A [kotlinx.coroutines.CoroutineScope] that lives as long as the process.
 * Use it for work that must outlive the caller (e.g. rolling billing dates forward),
 * never for UI-bound work.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
