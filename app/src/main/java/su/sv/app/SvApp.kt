package su.sv.app

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.github.axet.bookreader.app.BookApplication
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath
import ru.ok.tracer.HasTracerConfiguration
import ru.ok.tracer.TracerConfiguration
import ru.ok.tracer.crash.report.CrashFreeConfiguration
import ru.ok.tracer.crash.report.CrashReportConfiguration
import ru.ok.tracer.disk.usage.DiskUsageConfiguration
import ru.ok.tracer.heap.dumps.HeapDumpConfiguration
import timber.log.Timber

@HiltAndroidApp
class SvApp : BookApplication(), SingletonImageLoader.Factory, HasTracerConfiguration {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this);
        Timber.plant(Timber.DebugTree())
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.1)
                    .build()
            }
            .build()
    }

    override val tracerConfiguration: List<TracerConfiguration>
        get() = listOf(
            CrashReportConfiguration.build {
                // опции сборщика крэшей
            },
            CrashFreeConfiguration.build {
                // опции подсчета crash free
                setEnabled(true)
            },
            HeapDumpConfiguration.build {
                // опции сборщика хипдампов при ООМ
            },
            DiskUsageConfiguration.build {
                /* Опции анализатора дискового пространства
                setProbability - вероятность(1/n) того, что раз в день у этого юзера в фоне будет произведена проверка
                    использования дискового места. По умолчанию 0, что равноценно включению плагина
                setInterestingSize — лимит занимаемого места, при превышении которого сдк будет сигнализировать
                    о проблеме и отправлять отчет в Tracer. Измеряется в байтах. По умолчанию 10Gb
                setExcludePath — пути с заранее известными большими файлами которые должны быть исключены из проверки.
                    Принимает только пути сформированные через GlobalDirs
                 */
            },
        )
}
