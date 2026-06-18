package su.sv.main.badge

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel для управления состоянием бейджей в навигации
 */
@HiltViewModel
class BadgeViewModel @Inject constructor(
    private val badgeManager: BadgeManager,
) : ViewModel() {

    private val _showWikiBadge = MutableStateFlow(badgeManager.shouldShowWikiBadge())
    val showWikiBadge: StateFlow<Boolean> = _showWikiBadge.asStateFlow()

    /**
     * Отметить Wiki как посещённый (скрыть бейдж)
     */
    fun markWikiAsVisited() {
        badgeManager.markWikiAsVisited()
        _showWikiBadge.value = false
    }
}
