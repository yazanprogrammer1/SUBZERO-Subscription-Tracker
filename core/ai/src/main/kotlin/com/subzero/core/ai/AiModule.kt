package com.subzero.core.ai

import com.subzero.core.domain.assistant.Assistant
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiModule {

    @Binds
    abstract fun bindsAssistant(impl: CompositeAssistant): Assistant

    @Binds
    abstract fun bindsTransport(impl: HttpChatTransport): ChatTransport

    companion object {
        @Provides
        @Singleton
        fun providesAiConfig(): AiConfig = AiConfig.fromBuildConfig()
    }
}
