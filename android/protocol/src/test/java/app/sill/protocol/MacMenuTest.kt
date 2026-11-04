package app.sill.protocol

import org.junit.Assert.assertEquals
import org.junit.Test

class MacMenuTest {
    @Test
    fun roundTripMacMenu() {
        val item = MacMenuItem(id = "1.2", title = "Open", enabled = true, submenu = false)
        val menu = MacMenu(version = 3, app = "Code", bundleID = "com.microsoft.VSCode", menus = listOf(item), note = "hi")
        assertEquals(menu, MacMenu.fromJson(menu.toJson()))
    }

    @Test
    fun roundTripFetchAndPress() {
        val f = FetchMenu(version = 3, id = "1", title = "File", token = 7)
        assertEquals(f, FetchMenu.fromJson(f.toJson()))
        val p = PressMenuItem(version = 3, id = "1", title = "Open", token = 9)
        assertEquals(p, PressMenuItem.fromJson(p.toJson()))
    }
}