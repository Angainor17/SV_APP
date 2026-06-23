package su.sv.managers.theme

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.sv.commonui.theme.ThemeRepository
import javax.inject.Singleton

/**
 * DI модуль для темы
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        impl: ThemeRepositoryImpl
    ): ThemeRepository
}
