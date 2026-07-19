package su.sv.app.testing

/**
 * Аннотация для тестов, которые должны запускаться перед релизом.
 * Запуск: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=su.sv.app.testing.ReleaseTest
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ReleaseTest

/**
 * Аннотация для дымовых тестов (критичные сценарии).
 * Запуск: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.annotation=su.sv.app.testing.SmokeTest
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SmokeTest

/**
 * Аннотация для тестов навигации.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class NavigationTest