package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun followingTheSystemUsesTheSystemSetting() {
        assertTrue(ThemeMode.SYSTEM.useDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.useDark(systemDark = false))
    }

    @Test
    fun lightAndDarkIgnoreTheSystemSetting() {
        assertFalse(ThemeMode.LIGHT.useDark(systemDark = true))
        assertTrue(ThemeMode.DARK.useDark(systemDark = false))
    }

    @Test
    fun anUnknownOrMissingStoredValueFollowsTheSystem() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("PURPLE"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
    }
}
