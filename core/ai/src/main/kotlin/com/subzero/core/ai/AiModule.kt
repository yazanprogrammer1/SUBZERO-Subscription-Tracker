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
        /** The endpoint baked into this build, if the build had one; the user can override it. */
        @Provides
        @Singleton
        fun providesDefaultAiConfig(): AiConfig = AiConfig.fromBuildConfig()
    }
}
