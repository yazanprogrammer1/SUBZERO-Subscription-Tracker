package com.subzero.core.designsystem.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MonogramIndexTest {

    @Test
    fun `index is deterministic and case and whitespace insensitive`() {
        assertThat(monogramIndex("Netflix")).isEqualTo(monogramIndex("Netflix"))
        assertThat(monogramIndex("Netflix")).isEqualTo(monogramIndex("  netflix "))
    }

    @Test
    fun `index stays within the palette`() {
        listOf("", "a", "Netflix", "ChatGPT", "Spotify", "Google One", "Adobe Creative Cloud", "1Password", "YouTube Premium")
            .forEach { assertThat(monogramIndex(it)).isIn(0 until 8) }
    }

    @Test
    fun `different common services do not all collapse onto one hue`() {
        val hues = listOf("Netflix", "ChatGPT", "Spotify", "Google One", "Adobe", "Claude", "Dropbox", "Canva")
            .map { monogramIndex(it) }
            .toSet()
        assertThat(hues.size).isAtLeast(4)
    }
}
