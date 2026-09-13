package com.subzero.core.domain.assistant

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The on-device assistant is the default; a remote implementation can replace this binding later. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AssistantModule {

    @Binds
    abstract fun bindsAssistant(impl: LocalAssistant): Assistant
}
