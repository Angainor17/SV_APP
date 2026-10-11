package su.sv.shpargalki.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import su.sv.shpargalki.data.source.ApiDataSource
import su.sv.shpargalki.data.source.LocalJsonDataSource
import su.sv.shpargalki.data.source.ShpargalkiDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ShpargalkiModule {

    /**
     * Переключатель источника данных.
     * true  — локальный JSON из assets (текущий режим);
     * false — API (будет реализовано позже).
     */
    private const val USE_LOCAL_DATA_SOURCE = true

    @Provides
    @Singleton
    fun provideDataSource(
        local: LocalJsonDataSource,
        api: ApiDataSource,
    ): ShpargalkiDataSource = if (USE_LOCAL_DATA_SOURCE) local else api
}
