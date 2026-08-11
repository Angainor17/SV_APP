# Memory leak detection via LeakCanary

LeakCanary v2.14 added to the `debug` build type only.

## How it works

- Debug APK has `android:name=".DebugSvApp"` in `app/src/debug/AndroidManifest.xml`
- DebugSvApp duplicates Coil image loader configuration from SvApp
- `com.squareup.leakcanary:leakcanary-android:2.14` is on `debugImplementation` path
- LeakCanary automatically starts after `Application.onCreate()` — no manual init needed
- Watches Activity lifecycle: after `Activity.onDestroy()`, it waits up to 5 seconds
- On potential leak: shows notification, opens heap dump, starts MAL analysis

## Debug APK details

- Package name suffix: `.debug` (`su.sv.app.debug`)
- Can be installed alongside release APK (`su.sv.app`)
- Build: `./gradlew assembleDebug`

## Running on device

1. Install debug APK on device/emulator
2. Open app, navigate to Reader screen, open a book
3. Close reader (navigate back)
4. Wait ~5 seconds — LeakCanary checks for leaks
5. If leak detected: notification with heap dump opens MAL (Memory Analyzer) for inspection

## LeakCanary limitations

LeakCanary only detects Activity leaks that are **not** referenced after destroy.
It needs a heap dump (takes RAM) and ~2 seconds to detect.

For UI tests (androidTest), LeakCanary detection is unreliable because:

- `finish()` is called in the test
- The test process still holds references
- No heap dump can be taken while test runner is alive

## For UI tests — use LeakTestRule approach

LeakCanary **does not work** with instrumented tests that call `activity.finish()`
because the test runner holds a reference to the Activity through `Instrumentation`.

### Recommended alternatives

1. **Manual verification**: After UI test finishes, open MAT/Studio Profiler to check

2. **Robolectric**: Run tests in JVM, no test runner interference
   ```kotlin
   @RunWith(RobolectricTestRunner::class)
   @Config(sdk = [34])
   class ReaderScreenLeakTest {
       @Test
       fun `open and close reader should not leak`() {
           val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
           // navigate to reader...
           activity.finish()
           Robolectric.processSync()
           // activity should be garbage collected
       }
   }
   ```

3. **Manual LeakCanary check** in emulator:
    - Run debug APK on emulator
    - Open reader → close reader → wait 3s
    - Check LeakCanary notification for leak reports

## CI integration

LeakCanary is `debugImplementation` only and NOT included in `debugAndroidTest`.
For CI: use the profiler or Robolectric approach.
