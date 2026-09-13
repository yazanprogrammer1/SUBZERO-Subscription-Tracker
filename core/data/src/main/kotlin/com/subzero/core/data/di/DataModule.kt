package com.subzero.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.subzero.core.common.coroutines.ApplicationScope
import com.subzero.core.common.coroutines.Dispatcher
import com.subzero.core.common.coroutines.SubzeroDispatcher
import com.subzero.core.data.database.SubzeroDatabase
import com.subzero.core.data.database.dao.SubscriptionDao
import com.subzero.core.data.export.DataManager
import com.subzero.core.data.export.LocalDataManager
import com.subzero.core.data.preferences.DataStoreUserPreferencesRepository
import com.subzero.core.data.repository.RoomSubscriptionRepository
import com.subzero.core.domain.repository.SubscriptionRepository
import com.subzero.core.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindsSubscriptionRepository(impl: RoomSubscriptionRepository): SubscriptionRepository

    @Binds
    abstract fun bindsUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    abstract fun bindsDataManager(impl: LocalDataManager): DataManager

    companion object {

        @Provides
        @Singleton
        fun providesDatabase(@ApplicationContext context: Context): SubzeroDatabase =
            Room.databaseBuilder(context, SubzeroDatabase::class.java, SubzeroDatabase.NAME)
                // Migrations are registered here as versions are added. Never fallbackToDestructiveMigration.
                .build()

        @Provides
        fun providesSubscriptionDao(database: SubzeroDatabase): SubscriptionDao = database.subscriptionDao()

        @Provides
        @Singleton
        fun providesPreferencesDataStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            @Dispatcher(SubzeroDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope + ioDispatcher) {
                context.preferencesDataStoreFile(PREFERENCES_NAME)
            }

        private const val PREFERENCES_NAME = "user_preferences"
    }
}
