package su.sv.commonarchitecture.mock

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor для перехвата сетевых запросов и возврата мок-данных.
 *
 * Когда MockConfig.IS_MOCK_ENABLED = true:
 * - Перехватывает запросы к API
 * - Возвращает мок-ответы из MockDataProvider
 * - Сетевые запросы не выполняются
 *
 * Когда MockConfig.IS_MOCK_ENABLED = false:
 * - Пропускает запросы к реальному серверу
 *
 * @since 2026-06-24
 */
@Singleton
class MockInterceptor @Inject constructor(
    private val mockDataProvider: MockDataProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Если мок-режим выключен — пропускаем запрос дальше
        if (!MockConfig.IS_MOCK_ENABLED) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val url = request.url.toString()

        Timber.d("MockInterceptor: Intercepting request to $url")

        return when {
            // Wiki API (svremya.su)
            url.contains("svremya.su") -> {
                Timber.i("MockInterceptor: Serving Wiki mock for $url")
                mockDataProvider.getWikiMock(request)
            }

            // Books API (svremya.org)
            url.contains("svremya.org") -> {
                Timber.i("MockInterceptor: Serving Books mock for $url")
                mockDataProvider.getBooksMock(request)
            }

            // VK API (api.vk.com)
            url.contains("api.vk.com") -> {
                Timber.i("MockInterceptor: Serving VK mock for $url")
                mockDataProvider.getVkMock(request)
            }

            // Неизвестный URL — пропускаем запрос
            else -> {
                Timber.w("MockInterceptor: Unknown URL, passing through: $url")
                chain.proceed(request)
            }
        }
    }
}
