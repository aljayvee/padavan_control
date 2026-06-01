package com.example.padavancontrol.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.padavancontrol.data.models.LanClient
import com.example.padavancontrol.ui.screens.AccordionCategory
import com.example.padavancontrol.theme.PadavanControlTheme
import com.example.padavancontrol.Wifi2gGeneral
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedFlowIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAccordionExpansion() {
        var clickedRoute: Any? = null

        composeTestRule.setContent {
            PadavanControlTheme {
                AccordionCategory(
                    title = "Wireless 2.4GHz",
                    subtitle = "SSIDs and security configurations",
                    isExpanded = true,
                    onToggle = {},
                    items = listOf(
                        "General Settings" to Wifi2gGeneral
                    ),
                    onItemClick = { clickedRoute = it }
                )
            }
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Wireless 2.4GHz").assertExists()

        // Verify general settings is displayed when expanded
        composeTestRule.onNodeWithText("General Settings").assertExists()

        // Perform click and check route callback
        composeTestRule.onNodeWithText("General Settings").performClick()
        assert(clickedRoute == Wifi2gGeneral)
    }
}
