# Bug Report Module

Модуль отправки баг-репортов.

## Обзор

Модуль `bugreport` предоставляет функционал для отправки пользовательских отчётов об ошибках и пожеланиях к приложению. Поддерживает отправку через Tracer API и Email.

## Архитектура

```
bugreport/
├── data/
│   ├── ImgbbUploader.kt         # Загрузка изображений на Imgbb
│   └── BugReportWorker.kt       # WorkManager для фоновой отправки
├── domain/
│   ├── model/
│   │   └── BugReport.kt         # Модель баг-репорта
│   ├── SendBugReportUseCase.kt  # Отправка через Tracer + Imgbb
│   └── SendEmailReportUseCase.kt # Создание Email Intent
├── presentation/
│   ├── nav/
│   │   └── BugReportScreen.kt   # Modo Screen
│   └── bugreport/
│       ├── viewmodel/
│       │   ├── BugReportViewModel.kt
│       │   └── model/
│       │       ├── BugReportState.kt
│       │       ├── BugReportAction.kt
│       │       └── BugReportEffect.kt
│       └── ui/
│           ├── BugReportContent.kt
│           ├── BugReportForm.kt
│           ├── SuccessScreen.kt
│           ├── SendingContent.kt
│           └── ErrorContent.kt
└── di/
    └── BugReportModule.kt       # DI модуль Hilt
```

## Основные компоненты

### BugReport (Domain Model)

```kotlin
data class BugReport(
    val description: String,
    val screenshots: List<Uri>,
    val appVersion: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    val androidVersion: String,
    val timestamp: Long = System.currentTimeMillis(),
)
```

### SendBugReportUseCase

Отправка баг-репорта через Tracer + Imgbb:

```kotlin
class SendBugReportUseCase @Inject constructor(
    private val imgbbUploader: ImgbbUploader,
    private val bugReportWorkManager: BugReportWorkManager,
) {
    suspend fun execute(report: BugReport): Result<Unit>
}
```

**Алгоритм:**
1. Логирует метаданные через `TracerCrashReport.log()`
2. Загружает скриншоты на Imgbb → получает ссылки
3. Логирует ссылки в Tracer: `"Screenshot 0: https://i.ibb.co/xxx.jpg"`
4. Отправляет отчёт через `TracerCrashReport.report()`
5. Ставит задачу в WorkManager для гарантии доставки

### ImgbbUploader

Загрузка изображений на бесплатный хостинг Imgbb:

```kotlin
class ImgbbUploader @Inject constructor(context: Context) {
    suspend fun uploadImage(uri: Uri, timestamp: Long, index: Int): String?
}
```

**API:** `https://api.imgbb.com/1/upload`

### BugReportWorker

Фоновая отправка через WorkManager:

```kotlin
@HiltWorker
class BugReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val imgbbUploader: ImgbbUploader,
) : CoroutineWorker(context, workerParams)
```

**Особенности:**
- До 3 попыток с экспоненциальной задержкой
- Работает даже если приложение закрыто
- Требует наличие сети

### SendEmailReportUseCase

Создание Intent для отправки на email:

```kotlin
class SendEmailReportUseCase @Inject constructor(context: Context) {
    fun execute(report: BugReport, email: String = DEFAULT_EMAIL): Intent
}
```

**Email:** `angainor17@gmail.com`

## UI

### BugReportScreen (Modo)

```kotlin
@Parcelize
class BugReportScreen : Screen {
    @Composable
    override fun Content(modifier: Modifier)
}
```

### BugReportViewModel (MVI)

```kotlin
@HiltViewModel
class BugReportViewModel : BaseViewModel() {
    val state: StateFlow<BugReportState>
    val effect: Flow<BugReportEffect>
    fun onAction(action: BugReportAction)
}
```

### BugReportState

```kotlin
sealed class BugReportState {
    data class Form(
        val description: String,
        val descriptionError: String?,
        val screenshots: List<Uri>,
        val isSendButtonEnabled: Boolean,
        val isSending: Boolean,
        val sendEmailForFeedback: Boolean,  // Чекбокс
    ) : BugReportState()

    object Sending : BugReportState()
    object Success : BugReportState()
    data class SuccessWithEmail(val emailIntent: Intent) : BugReportState()
    data class Error(val message: String) : BugReportState()
}
```

### BugReportAction

```kotlin
sealed class BugReportAction {
    data class OnDescriptionChange(val text: String) : BugReportAction()
    data class OnSendEmailForFeedbackChange(val checked: Boolean) : BugReportAction()
    data class OnScreenshotsSelected(val uris: List<Uri>) : BugReportAction()
    data class OnRemoveScreenshot(val uri: Uri) : BugReportAction()
    object OnSendReport : BugReportAction()
    object OnSuccessDismiss : BugReportAction()
    object OnRetry : BugReportAction()
}
```

## Логика отправки

### Flow

```
Пользователь нажимает "Отправить"
    ↓
1. SendBugReportUseCase.execute():
   ├─ TracerCrashReport.log() — метаданные
   ├─ ImgbbUploader.uploadImage() — загрузка скриншотов
   ├─ TracerCrashReport.log() — ссылки на скриншоты
   ├─ TracerCrashReport.report() — отправка отчёта
   └─ BugReportWorkManager.enqueueBugReport() — фоновая отправка
    ↓
2. Если sendEmailForFeedback = true:
   → SuccessWithEmail → открыть Email Intent
   Иначе:
   → Success → показать SuccessScreen
```

### Что логируется в Tracer

```
=== Bug Report ===
Timestamp: 1721600000000
Description: Текст пользователя...
App Version: 0.3.1
Device: Samsung Galaxy S21
Android: 14
Screenshots: 2
Screenshot 0: https://i.ibb.co/xxx.jpg
Screenshot 1: https://i.ibb.co/yyy.jpg
```

## Зависимости

```kotlin
// build.gradle.kts
dependencies {
    // Модули-utils
    implementation(project(":commonarchitecture"))
    implementation(project(":commonui"))

    // Compose
    implementation(libs.bundles.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.modo.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Logging
    implementation(libs.timber)

    // Tracer
    implementation(platform(libs.tracer.platform))
    implementation(libs.tracer.crash.report)

    // WorkManager
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // OkHttp (Imgbb API)
    implementation(libs.okhttp)
}
```

## Интеграция

### Точка входа

Иконка 🐛 в Toolbar экрана RootInfo (info модуль):

```kotlin
// RootInfo.kt
IconButton(onClick = { viewModel.onAction(RootInfoActions.OnBugReportClick) }) {
    Icon(Icons.Default.BugReport, contentDescription = "Сообщить о проблеме")
}
```

### Навигация

```kotlin
// RootInfoViewModel.kt
is RootInfoActions.OnBugReportClick -> {
    _effect.trySend(RootInfoEffect.OpenBugReport)
}

// RootInfo.kt
OneTimeEffect(viewModel.effect) { effect ->
    when (effect) {
        is RootInfoEffect.OpenBugReport -> {
            stackNavigation.forward(BugReportScreen())
        }
    }
}
```

## API Keys

**Imgbb API Key:** хранится в `ImgbbUploader.kt`

```kotlin
companion object {
    const val API_KEY = "b4d9b1eb07f78d1d5cad70253cd29b03"
}
```

Получить ключ: https://imgbb.com/

## Структура файлов

```
bugreport/src/main/java/su/sv/bugreport/
├── data/
│   ├── ImgbbUploader.kt
│   └── BugReportWorker.kt
├── domain/
│   ├── model/
│   │   └── BugReport.kt
│   ├── SendBugReportUseCase.kt
│   └── SendEmailReportUseCase.kt
├── presentation/
│   ├── nav/
│   │   └── BugReportScreen.kt
│   └── bugreport/
│       ├── viewmodel/
│       │   ├── BugReportViewModel.kt
│       │   └── model/
│       │       ├── BugReportState.kt
│       │       ├── BugReportAction.kt
│       │       └── BugReportEffect.kt
│       └── ui/
│           ├── BugReportContent.kt
│           ├── BugReportForm.kt
│           ├── SuccessScreen.kt
│           ├── SendingContent.kt
│           └── ErrorContent.kt
└── di/
    └── BugReportModule.kt
```

## Особенности реализации

### Поле описания

Авто-расширение в зависимости от текста:
- `minLines = 3`
- `maxLines = Int.MAX_VALUE`

### Скриншоты

- Photo Picker: `ActivityResultContracts.PickMultipleVisualMedia(5)`
- Превью: 80x80dp с кнопкой удаления
- Максимум: 5 изображений

### Чекбокс для обратной связи

Если выбран, после успешной отправки открывается Email Intent с:
- Заполненным текстом
- Прикреплёнными скриншотами

### Валидация

- Минимум 10 символов для описания
- Кнопка "Отправить" активна только при валидном тексте