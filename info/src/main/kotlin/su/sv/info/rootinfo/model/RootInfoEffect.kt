package su.sv.info.rootinfo.model

/**
 * Side-эффекты экрана RootInfo
 */
sealed class RootInfoEffect {

    /** Открыть экран Bug Report */
    object OpenBugReport : RootInfoEffect()
}