package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches Ukrainian title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Калькулятор", appName)
    }

    @Test
    fun `standard calculator evaluates expression including 00 and AC`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("std_2").performClick()
        composeRule.onNodeWithTag("std_00").performClick()
        composeRule.onNodeWithTag("std_add").performClick()
        composeRule.onNodeWithTag("std_5").performClick()
        composeRule.onNodeWithTag("std_0").performClick()
        composeRule.onNodeWithTag("std_equals").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("standard_result_text").assertTextContains("250")

        composeRule.onNodeWithTag("std_clear").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("standard_expression_text").assertTextContains("0")
    }

    @Test
    fun `quick icons and mode grid navigate smoothly across all modes`() {
        composeRule.waitForIdle()

        // 1-tap Fractions toggle
        composeRule.onNodeWithTag("quick_fractions_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("frac_1").performClick()
        composeRule.onNodeWithTag("frac_template").performClick()
        composeRule.onNodeWithTag("frac_2").performClick()
        composeRule.onNodeWithTag("frac_add").performClick()
        composeRule.onNodeWithTag("frac_1").performClick()
        composeRule.onNodeWithTag("frac_template").performClick()
        composeRule.onNodeWithTag("frac_4").performClick()
        composeRule.onNodeWithTag("frac_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fraction_result_view").assertIsDisplayed()

        // 1-tap Scientific toggle
        composeRule.onNodeWithTag("quick_scientific_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sci_sin").assertIsDisplayed()

        // Mode Grid -> Powers (2^3 × 2^4 = 128)
        composeRule.onNodeWithTag("open_modes_grid").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_powers").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pow_2").performClick()
        composeRule.onNodeWithTag("pow_template").performClick()
        composeRule.onNodeWithTag("pow_3").performClick()
        composeRule.onNodeWithTag("pow_mul").performClick()
        composeRule.onNodeWithTag("pow_2").performClick()
        composeRule.onNodeWithTag("pow_template").performClick()
        composeRule.onNodeWithTag("pow_4").performClick()
        composeRule.onNodeWithTag("pow_equals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("power_result_text").assertTextContains("128", substring = true)
        composeRule.onNodeWithTag("power_simplified_view").assertIsDisplayed()

        // Mode Grid -> Physics (search & solve v = s / t and t = s / v)
        composeRule.onNodeWithTag("open_modes_grid").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_physics").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("physics_search_input").performTextInput("швидкість")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("physics_formula_speed_distance_time").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("physics_input_s").performTextInput("100")
        composeRule.onNodeWithTag("physics_input_t").performTextInput("5")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("physics_result_text").assertTextContains("20", substring = true)
        composeRule.onNodeWithTag("physics_target_t").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("physics_input_v").performTextInput("25")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("physics_result_text").assertTextContains("4", substring = true)
        composeRule.onNodeWithTag("physics_solver_close").performClick()
        composeRule.waitForIdle()

        // Mode Grid -> Currency
        composeRule.onNodeWithTag("open_modes_grid").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_currency").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("currency_from_card").assertIsDisplayed()

        // Mode Grid -> History
        composeRule.onNodeWithTag("open_modes_grid").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_history").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Mode Grid -> Settings
        composeRule.onNodeWithTag("open_modes_grid").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("grid_settings").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_precision_slider")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
