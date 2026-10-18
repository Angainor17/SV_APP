package su.sv.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Генерирует Baseline Profile для :app.
 *
 * Сценарий — типовой путь пользователя (детерминированный, без сети):
 *   1. холодный старт → лента «Новости»;
 *   2. вкладка «Книги» → каталог (мок отдаёт список с задержкой ~1 c);
 *   3. первая книга «ОВЛ том №12» → карточка книги (обложка, описание, кнопка действия);
 *   4. назад в каталог;
 *   5. вкладки «Wiki» и «Наши проекты»;
 *   6. возврат на «Новости».
 *
 * Навигация идёт по видимым текстам/контент-описаниям (UiAutomator), т.к.
 * Compose testTag в UiAutomator не виден без `testTagsAsResourceId`.
 *
 * Открытие читалки (ReaderScreen) осознанно не включено: для него нужна уже
 * скачанная книга (сеть + DownloadManager), что делает профиль нестабильным.
 * Когда появится предзагруженная книга — добавить сюда шаг чтения.
 *
 * Запуск (нужно подключённое устройство с API 33+ или root):
 *   ./gradlew :app:generateReleaseBaselineProfile
 *
 * Результат: app/src/release/generated/baselineProfiles/baseline-prof.txt
 */
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun typicalJourney() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        profileBlock = {
            // 1. Холодный старт → лента «Новости».
            pressHome()
            startActivityAndWait()
            tap(By.desc("Новости"), LAUNCH_TIMEOUT_MS)

            // 2. Вкладка «Книги» → каталог.
            tap(By.desc("Книги"))

            // 3. Первая книга → карточка книги (по кнопке «Назад» понимаем, что она открылась).
            tap(By.textContains(FIRST_BOOK_TITLE), BOOKS_TIMEOUT_MS)
            device.wait(Until.hasObject(By.desc("Назад")), DETAIL_TIMEOUT_MS)
            device.waitForIdle()

            // 4. Назад в каталог.
            tap(By.desc("Назад"))
            device.waitForIdle()

            // 5. Остальные вкладки: Wiki и «Наши проекты».
            tap(By.desc("Wiki"))
            device.waitForIdle()
            tap(By.desc("Наши проекты"))
            device.waitForIdle()

            // 6. Возврат на «Новости».
            tap(By.desc("Новости"))
            device.waitForIdle()
        },
    )

    private companion object {
        const val PACKAGE_NAME = "su.sv.app"
        const val FIRST_BOOK_TITLE = "ОВЛ том"
        const val LAUNCH_TIMEOUT_MS = 15_000L
        const val BOOKS_TIMEOUT_MS = 15_000L
        const val DETAIL_TIMEOUT_MS = 10_000L
    }
}

/**
 * Ждёт появления элемента (до [timeoutMs]) и кликает по нему.
 * Падает с понятным сообщением, если элемент так и не появился.
 */
private fun MacrobenchmarkScope.tap(selector: BySelector, timeoutMs: Long = 5_000L) {
    val found = device.wait(Until.hasObject(selector), timeoutMs)
    check(found) { "Элемент не найден за ${timeoutMs}мс: $selector" }
    device.findObject(selector).click()
}
