package su.sv.quiz.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import su.sv.commonui.theme.LocalAppDimensions
import su.sv.quiz.R
import su.sv.quiz.domain.model.Quiz

/**
 * Карточка квиза в списках (корневой экран, экран темы).
 *
 * @param quiz квиз
 * @param isNew показывать ли бейдж «Новое»
 * @param onClick обработчик клика
 */
@Composable
fun QuizCard(
    quiz: Quiz,
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = LocalAppDimensions.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(dimensions.cardPaddingInner)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isNew) {
                    NewBadge()
                }
            }
            Spacer(modifier = Modifier.height(dimensions.itemSpacingSmall))
            Text(
                text = pluralStringResource(
                    R.plurals.quiz_question_count,
                    quiz.questions.size,
                    quiz.questions.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Бейдж «Новое» для недавно созданных квизов.
 */
@Composable
private fun NewBadge() {
    val dimensions = LocalAppDimensions.current

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = stringResource(R.string.quiz_new_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(
                horizontal = dimensions.itemSpacingSmall,
                vertical = dimensions.itemSpacingSmall,
            ),
        )
    }
}
