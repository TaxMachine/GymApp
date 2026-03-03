package dev.taxmachine.gymapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.taxmachine.gymapp.db.GymDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = GymDatabase.getDatabase(context)
        runBlocking {
            db.clearAllTables()
        }
    }

    @Test
    fun testNavigationAndAddSupplement() {
        // Navigate to Supps tab
        composeTestRule.onNodeWithText("Supps").performClick()
        
        // Check if empty state is shown
        composeTestRule.onNodeWithText("No supplements added yet. Press + to add.").assertIsDisplayed()

        // Click Add FAB
        composeTestRule.onNodeWithContentDescription("Add Supplement").performClick()

        // Fill the form
        composeTestRule.onNodeWithText("Name").performTextInput("Vitamin D")
        composeTestRule.onNodeWithText("Dosage").performTextInput("2000")
        
        // Click Save
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify it appears in the list
        composeTestRule.onNodeWithText("Vitamin D").assertIsDisplayed()
        // Format: dosage + unit.label + " - " + timing.label
        composeTestRule.onNodeWithText("2000mg - Morning (Fasted)").assertIsDisplayed()
    }

    @Test
    fun testAddExercise() {
        // Navigate to Workouts tab
        composeTestRule.onNodeWithText("Workouts").performClick()

        // 1. Create a Split
        composeTestRule.onNodeWithContentDescription("Add Split").performClick()
        composeTestRule.onNodeWithText("Split Name (e.g. Push)").performTextInput("Push")
        composeTestRule.onNodeWithText("Create").performClick()

        // 2. Select the Split
        composeTestRule.onNodeWithText("Push").performClick()

        // 3. Add an Exercise
        composeTestRule.onNodeWithContentDescription("Add Exercise").performClick()
        
        // In the new dialog, the labels are "Name", "Weight", "Reps"
        composeTestRule.onNodeWithText("Name").performTextInput("Squat")
        composeTestRule.onNodeWithText("Weight").performTextInput("120")
        composeTestRule.onNodeWithText("Reps").performTextInput("5")

        // Click Save
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify in list
        composeTestRule.onNodeWithText("Squat").assertIsDisplayed()
        // Format: weight + weightUnit + " x " + reps + " reps"
        composeTestRule.onNodeWithText("120.0kg x 5 reps").assertIsDisplayed()
    }
}
