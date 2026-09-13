package com.subzero.core.data.export

import android.content.Context
import android.net.Uri
import com.subzero.core.common.coroutines.Dispatcher
import com.subzero.core.common.coroutines.SubzeroDispatcher
import com.subzero.core.data.database.SubzeroDatabase
import com.subzero.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Facts about what SUBZERO stores on the device, for the Data & storage screen. */
data class StorageInfo(
    val databaseBytes: Long,
    val subscriptionCount: Int,
)

/** Export writing and storage facts, as the settings screen needs them. */
interface DataManager {
    suspend fun writeExport(uri: Uri, format: ExportFormat)
    suspend fun storageInfo(): StorageInfo
}

/**
 * Writes exports to a user-chosen document and reports local storage use. The only place the
 * app touches a file outside its sandbox, and only to a Uri the user picked.
 */
class LocalDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exporter: DataExporter,
    private val repository: SubscriptionRepository,
    @Dispatcher(SubzeroDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DataManager {
    override suspend fun writeExport(uri: Uri, format: ExportFormat) {
        val content = exporter.export(format)
        withContext(ioDispatcher) {
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Could not open the chosen location")
            stream.bufferedWriter().use { it.write(content) }
        }
    }

    override suspend fun storageInfo(): StorageInfo = withContext(ioDispatcher) {
        val db = context.getDatabasePath(SubzeroDatabase.NAME)
        val wal = context.getDatabasePath("${SubzeroDatabase.NAME}-wal")
        StorageInfo(
            databaseBytes = db.length() + wal.length(),
            subscriptionCount = repository.getSubscriptions().size,
        )
    }
}
