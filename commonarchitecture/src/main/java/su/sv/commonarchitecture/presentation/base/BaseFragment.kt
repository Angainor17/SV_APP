package su.sv.commonarchitecture.presentation.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope

abstract class BaseFragment(@LayoutRes layout: Int) : Fragment(layout) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenStarted {
            callOperations()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onSetupLayout(savedInstanceState)
        onBindViewModel()
    }

    /**
     * Вызывать методы вью модели, которые получают данные из репозиториев
     */
    abstract fun callOperations()

    /**
     * В этом методе производить все настройки вью фрагмента
     * установка OnClickListener, RecyclerView.Adapter/LayoutManager, etc
     */
    abstract fun onSetupLayout(savedInstanceState: Bundle?)

    /**
     * В этом методе производить подписки на изменения значений в лайвдатах у ViewModel
     */
    abstract fun onBindViewModel()

    protected infix fun <T> LiveData<T>.observe(block: (T) -> Unit) {
        observe(this@BaseFragment.viewLifecycleOwner) { t -> block.invoke(t) }
    }
}
