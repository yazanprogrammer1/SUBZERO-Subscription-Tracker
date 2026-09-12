package com.subzero.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import com.subzero.core.navigation.SubzeroNavKey
import com.subzero.core.navigation.TopLevelKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One back stack per bottom-navigation tab, flattened into a single list for NavDisplay.
 *
 * Tabs are kept in most-recently-used order, so system back from a tab root returns to the
 * previously visited tab, and back from the first tab exits the app. The whole structure is
 * serialized through [Saver] so it survives configuration changes and process death.
 */
@Stable
class TopLevelBackStack private constructor(
    initialStacks: List<List<SubzeroNavKey>>,
) {
    constructor(startKey: TopLevelKey) : this(listOf(listOf(startKey)))

    private val stacks: LinkedHashMap<TopLevelKey, SnapshotStateList<SubzeroNavKey>> =
        LinkedHashMap<TopLevelKey, SnapshotStateList<SubzeroNavKey>>().apply {
            initialStacks.forEach { stack ->
                val root = stack.first() as TopLevelKey
                put(root, mutableStateListOf(*stack.toTypedArray()))
            }
        }

    /** The tab whose stack is currently on top. */
    var topLevelKey: TopLevelKey by mutableStateOf(stacks.keys.last())
        private set

    /** The flattened stack rendered by NavDisplay. */
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf<NavKey>().also { flat ->
        flat.addAll(stacks.values.flatten())
    }

    /** The destination currently on screen. */
    val currentKey: SubzeroNavKey
        get() = stacks.getValue(topLevelKey).last()

    fun switchTab(key: TopLevelKey) {
        if (key == topLevelKey) return
        val existing = stacks.remove(key)
        stacks[key] = existing ?: mutableStateListOf(key)
        topLevelKey = key
        publish()
    }

    fun push(key: SubzeroNavKey) {
        stacks.getValue(topLevelKey).add(key)
        publish()
    }

    /** Pops the current destination; when a tab root is popped the tab itself is removed. */
    fun pop() {
        val current = stacks.getValue(topLevelKey)
        val removed = current.removeLastOrNull()
        if (current.isEmpty()) {
            stacks.remove(removed as TopLevelKey)
        }
        if (stacks.isEmpty()) return
        topLevelKey = stacks.keys.last()
        publish()
    }

    private fun publish() {
        backStack.clear()
        backStack.addAll(stacks.values.flatten())
    }

    private fun snapshot(): List<List<SubzeroNavKey>> = stacks.values.map { it.toList() }

    companion object {
        private val serializer = ListSerializer(ListSerializer(SubzeroNavKey.serializer()))

        val Saver: Saver<TopLevelBackStack, String> = Saver(
            save = { Json.encodeToString(serializer, it.snapshot()) },
            restore = { TopLevelBackStack(Json.decodeFromString(serializer, it)) },
        )
    }
}

@Composable
fun rememberTopLevelBackStack(startKey: TopLevelKey): TopLevelBackStack =
    rememberSaveable(saver = TopLevelBackStack.Saver) { TopLevelBackStack(startKey) }
