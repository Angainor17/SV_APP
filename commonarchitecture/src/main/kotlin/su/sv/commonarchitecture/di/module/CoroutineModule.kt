package su.sv.commonarchitecture.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
abstract class CoroutineModule {
    @Binds
    abstract fun provideDispatcherProvider(
        dispatcherProvider: DispatcherProviderImpl
    ): DispatcherProvider
}

class DispatcherProviderImpl @Inject constructor() : DispatcherProvider {
    override val io = Dispatchers.IO
    override val main = Dispatchers.Main
    override val default = Dispatchers.Default
}

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}
