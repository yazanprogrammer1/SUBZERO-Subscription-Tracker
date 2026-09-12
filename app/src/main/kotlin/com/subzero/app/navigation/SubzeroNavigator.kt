package com.subzero.app.navigation

import com.subzero.core.navigation.Navigator
import com.subzero.core.navigation.SubzeroNavKey
import com.subzero.core.navigation.TopLevelKey

/** [Navigator] implementation backed by the app [TopLevelBackStack]. */
class SubzeroNavigator(private val backStack: TopLevelBackStack) : Navigator {
    override fun navigate(key: SubzeroNavKey) = backStack.push(key)
    override fun switchTab(key: TopLevelKey) = backStack.switchTab(key)
    override fun back() = backStack.pop()
}
