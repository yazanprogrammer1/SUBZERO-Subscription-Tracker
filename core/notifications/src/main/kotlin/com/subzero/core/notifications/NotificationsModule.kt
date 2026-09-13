package com.subzero.core.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.subzero.core.common.coroutines.ApplicationScope
import com.subzero.core.common.coroutines.Dispatcher
import com.subzero.core.common.coroutines.SubzeroDispatcher
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
internal abstract class NotificationsModule {

    @Binds
    abstract fun bindsNotifier(impl: SubzeroNotifier): Notifier

    @Binds
    abstract fun bindsLedger(impl: DataStoreNotificationLedger): NotificationLedger

    @Binds
    abstract fun bindsScheduler(impl: WorkManagerNotificationScheduler): NotificationScheduler

    companion object {
        @Provides
        @Singleton
        @LedgerDataStore
        fun providesLedgerDataStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            @Dispatcher(SubzeroDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope + ioDispatcher) {
                context.preferencesDataStoreFile("notification_ledger")
            }
    }
}
