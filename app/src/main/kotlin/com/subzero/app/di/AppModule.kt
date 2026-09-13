package com.subzero.app.di

import com.subzero.app.BuildConfig
import com.subzero.feature.settings.AppInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun providesAppInfo(): AppInfo = AppInfo(versionName = BuildConfig.VERSION_NAME)
}
