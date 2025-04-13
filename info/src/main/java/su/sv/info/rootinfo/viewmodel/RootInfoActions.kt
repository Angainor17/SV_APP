package su.sv.info.rootinfo.viewmodel


sealed class RootInfoActions {

    /** Обновление списка инфо при помощи SwipeRefresh */
    object OnSwipeRefresh : RootInfoActions()

    /** Нажатие на кнопку "Повторить", которая отобразилась из-за ошибки */
    object OnRetryClick : RootInfoActions()
}
