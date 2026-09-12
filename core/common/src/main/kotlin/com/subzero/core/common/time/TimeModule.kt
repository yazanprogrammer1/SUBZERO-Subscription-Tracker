package com.subzero.core.common.time

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * The single source of "now" for the whole app.
 *
 * Nothing in SUBZERO calls `LocalDate.now()` directly; everything receives a [Clock].
 * That is what makes "tomorrow", month boundaries and leap years testable.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {

    @Provides
    fun providesClock(): Clock = Clock.systemDefaultZone()
}
