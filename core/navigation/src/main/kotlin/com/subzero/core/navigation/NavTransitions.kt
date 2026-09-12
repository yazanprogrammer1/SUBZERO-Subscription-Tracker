package com.subzero.core.navigation

/**
 * Per-destination transition metadata, attached when a feature registers its entries.
 *
 * The maps are Navigation 3 entry metadata built by the app module (which owns the motion
 * tokens and the NavDisplay), so features declare *which* transition a screen uses without
 * depending on the animation implementation.
 */
data class NavTransitions(
    /** Top-level tabs: fade through. */
    val tab: Map<String, Any>,
    /** Pushed screens: slide in from the end, slide back out. */
    val push: Map<String, Any>,
    /** Modal forms: rise from the bottom, sink back. */
    val modal: Map<String, Any>,
) {
    companion object {
        /** No custom metadata; NavDisplay defaults apply. Used by previews and tests. */
        val None = NavTransitions(emptyMap(), emptyMap(), emptyMap())
    }
}
