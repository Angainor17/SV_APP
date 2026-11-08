package su.sv.shpargalki.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.sv.shpargalki.domain.model.DialecticsNode
import su.sv.shpargalki.testing.ShpargalkiTestTags

/**
 * UI-тесты шторки понятия [NodeDetailSheet].
 */
@RunWith(AndroidJUnit4::class)
class NodeDetailSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sheet_displayed_whenNodeProvided() {
        composeRule.setContent {
            MaterialTheme {
                NodeDetailSheet(node = sampleNode(), onDismiss = {})
            }
        }

        composeRule.onNodeWithTag(ShpargalkiTestTags.NODE_SHEET, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("Бытие").assertIsDisplayed()
    }

    @Test
    fun sheet_hidden_whenNodeNull() {
        composeRule.setContent {
            MaterialTheme {
                NodeDetailSheet(node = null, onDismiss = {})
            }
        }

        composeRule.onNodeWithTag(ShpargalkiTestTags.NODE_SHEET, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    private fun sampleNode(): DialecticsNode = DialecticsNode(
        id = "bytie",
        order = 1,
        term = "Бытие",
        german = "Sein",
        parents = emptyList(),
        section = "bytie",
        subsection = "Качество",
        popov = "Пояснение Попова",
        hegel = "Пояснение Гегеля",
        svremya = "Шпаргалка",
        derivation = "Выводится из предыдущего",
        pair = "Ничто",
    )
}
