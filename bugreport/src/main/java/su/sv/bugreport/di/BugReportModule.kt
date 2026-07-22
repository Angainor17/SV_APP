package su.sv.bugreport.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import su.sv.bugreport.data.BugReportWorkManager
import su.sv.bugreport.data.ImgbbUploader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BugReportModule {

    @Provides
    @Singleton
    fun provideImgbbUploader(
        @ApplicationContext context: Context,
    ): ImgbbUploader {
        return ImgbbUploader(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideBugReportWorkManager(
        workManager: WorkManager,
    ): BugReportWorkManager {
        return BugReportWorkManager(workManager)
    }
}