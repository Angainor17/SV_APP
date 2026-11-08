package su.sv.shpargalki.presentation.tree

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import su.sv.shpargalki.domain.model.DialecticsNode

/**
 * UI-тесты вертикального древа понятий [DialecticsTree].
 *
 * Компонент чистый (stateless): принимает группы понятий и колбэк клика,
 * поэтому тестируется изолированно, без Hilt/навигации/сети.
 */
@RunWith(AndroidJUnit4::class)
class DialecticsTreeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tree_displaysNodeTermAndGerman() {
        val node = node(term = "Бытие", german = "Sein")
        composeRule.setContent {
            MaterialTheme {
                DialecticsTree(
                    groups = listOf(SubsectionGroup("Качество", listOf(node))),
                    onNodeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Бытие").assertIsDisplayed()
        composeRule.onNodeWithText("Sein").assertIsDisplayed()
    }

    @Test
    fun tree_clickInvokesCallbackWithNode() {
        val node = node(term = "Бытие")
        var clicked: DialecticsNode? = null
        composeRule.setContent {
            MaterialTheme {
                DialecticsTree(
                    groups = listOf(SubsectionGroup("Качество", listOf(node))),
                    onNodeClick = { clicked = it },
                )
            }
        }

        composeRule.onNodeWithText("Бытие").performClick()

        assertEquals(node, clicked)
    }

    @Test
    fun tree_rendersMultipleSubsections() {
        val being = node(id = "being", order = 1, term = "Бытие", subsection = "Качество")
        val nothing = node(id = "nothing", order = 2, term = "Ничто", subsection = "Качество")
        val becoming = node(id = "becoming", order = 3, term = "Становление", subsection = "Количество")
        composeRule.setContent {
            MaterialTheme {
                DialecticsTree(
                    groups = listOf(
                        SubsectionGroup("Качество", listOf(being, nothing)),
                        SubsectionGroup("Количество", listOf(becoming)),
                    ),
                    onNodeClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Бытие").assertIsDisplayed()
        composeRule.onNodeWithText("Ничто").assertIsDisplayed()
        composeRule.onNodeWithText("Становление").assertIsDisplayed()
    }

    private fun node(
        id: String = "bytie",
        order: Int = 1,
        term: String,
        german: String = "",
        subsection: String = "Качество",
    ): DialecticsNode = DialecticsNode(
        id = id,
        order = order,
        term = term,
        german = german,
        parents = emptyList(),
        section = "bytie",
        subsection = subsection,
        popov = "Пояснение Попова",
        hegel = "Пояснение Гегеля",
        svremya = "Шпаргалка",
        derivation = "Выводится из предыдущего",
        pair = "",
    )
}
