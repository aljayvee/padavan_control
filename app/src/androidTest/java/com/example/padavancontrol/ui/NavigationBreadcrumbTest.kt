package com.example.padavancontrol.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.navigation3.runtime.NavKey
import com.example.padavancontrol.*
import com.example.padavancontrol.ui.components.BreadcrumbBar
import com.example.padavancontrol.theme.PadavanControlTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationBreadcrumbTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBreadcrumbBarRenderingAndNavigation() {
        val backStack = listOf<NavKey>(
            Dashboard,
            Settings,
            AdvancedHub,
            WanDmz
        )

        var targetedKey: NavKey? = null

        composeTestRule.setContent {
            PadavanControlTheme {
                BreadcrumbBar(
                    backStack = backStack,
                    onNavigateToKey = { targetedKey = it }
                )
            }
        }

        // Verify all navigation keys resolved labels are displayed
        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
        composeTestRule.onNodeWithText("Advanced").assertExists()
        composeTestRule.onNodeWithText("DMZ").assertExists()

        // Click intermediate item (Settings)
        composeTestRule.onNodeWithText("Settings").performClick()
        assertEquals(Settings, targetedKey)
    }
}
