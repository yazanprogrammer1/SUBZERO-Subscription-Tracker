package com.subzero.core.navigation

/**
 * The only navigation surface features see. Implemented by the app module on top of the
 * Navigation 3 back stack, so features stay independent of the navigation library.
 */
interface Navigator {
    /** Push [key] onto the current top-level stack. */
    fun navigate(key: SubzeroNavKey)

    /** Switch to a bottom-navigation tab, restoring that tab's own back stack. */
    fun switchTab(key: TopLevelKey)

    /** Pop the current destination. */
    fun back()
}

/**
 * Provided by the app shell. Features call `LocalNavigator.current.navigate(...)`.
 * The default throws so a missing provider is caught immediately in previews/tests.
 */
val LocalNavigator = androidx.compose.runtime.staticCompositionLocalOf<Navigator> {
    error("No Navigator provided. Wrap the content in the app shell or provide a fake in tests.")
}
