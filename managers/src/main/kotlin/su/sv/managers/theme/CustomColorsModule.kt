package su.sv.managers.theme

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления зависимостей кастомных цветов темы.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CustomColorsModule {

    @Binds
    @Singleton
    abstract fun bindCustomColorsRepository(
        impl: CustomColorsRepositoryImpl
    ): CustomColorsRepository
}