# Project Plan

Tích hợp 3 project mới vào AllDemo Hub: WakewordVoiceprint (tại E:\AndroidStudioCode\WakewordVoiceprint), FUTOVoiceInputEngine (tại E:\AndroidStudioCode\FUTOVoiceInputEngine) và DeepgramNova2APIWebSocket (tại E:\AndroidStudioCode\DeepgramNova2APIWebSocket). Đọc mã nguồn các project, đặt tên package khớp chính xác tên project (com.example.alldemo.features.wakewordvoiceprint, com.example.alldemo.features.futovoiceinputengine, com.example.alldemo.features.deepgramnova2apiwebsocket), tạo 3 nút/thẻ điều hướng tương ứng trên màn hình chính AllDemo Hub.

## Project Brief

# Project Brief: AllDemo Hub Integration

## Tính năng chính (Features)
1. **Màn hình trung tâm AllDemo Hub**: Màn hình chính chứa 3 thẻ/nút điều hướng trực quan, cho phép chuyển đổi mượt mà giữa các mô-đun tính năng tích hợp.
2. **Mô-đun Wakeword & Voiceprint** (`com.example.alldemo.features.wakewordvoiceprint`): Tích hợp khả năng phát hiện từ kích hoạt (wakeword) và xác thực dấu vân giọng nói (voiceprint).
3. **Mô-đun FUTO Voice Input Engine** (`com.example.alldemo.features.futovoiceinputengine`): Tích hợp bộ máy nhận dạng và nhập liệu giọng nói offline từ FUTO Voice Input Engine.
4. **Mô-đun Deepgram Nova-2 API WebSocket** (`com.example.alldemo.features.deepgramnova2apiwebsocket`): Tích hợp truyền phát âm thanh thời gian thực (Real-time Speech-to-Text) qua kết nối WebSocket với Deepgram Nova-2 API.

## Kiến trúc & Công nghệ (High-Level Tech Stack)
- **Ngôn ngữ lập trình**: Kotlin
- **Giao diện người dùng**: Jetpack Compose & Material Design 3
- **Điều hướng (Navigation)**: **Jetpack Navigation 3** (State-driven navigation)
- **Thích ứng màn hình (Adaptive Layout)**: **Compose Material Adaptive**
- **Xử lý bất đồng bộ & Luồng dữ liệu**: Kotlin Coroutines & StateFlow
- **Giao tiếp mạng**: OkHttp (cho kết nối WebSocket thời gian thực tới Deepgram Nova-2)
- **Cấu trúc Package chính**:
  - `com.example.alldemo.features.wakewordvoiceprint`
  - `com.example.alldemo.features.futovoiceinputengine`
  - `com.example.alldemo.features.deepgramnova2apiwebsocket`

## Implementation Steps
**Total Duration:** 1h 25m 26s

### Task_1_SetupRegistryAndViewModel: Define the Demo Registry data model, repository, ViewModel with StateFlow, search/filter business logic, and sample demo entries.
- **Status:** COMPLETED
- **Updates:** Successfully created DemoCategory, DemoItem data model, FeatureRegistryRepository with sample demo modules, DemoLauncherViewModel with StateFlow reactive search/category filtering, and verified with 6/6 unit tests passing and debug APK build.
- **Acceptance Criteria:**
  - DemoItem data model and FeatureRegistry implementation created
  - ViewModel created handling search queries and category filters with StateFlow
  - Sample demo entries registered in the registry
- **Duration:** 4m 25s

### Task_2_BuildDashboardAndNavigationUI: Implement UI using Jetpack Compose (Material 3) and Navigation 3, featuring a Search bar, category filter chips, demo grid/list dashboard, and state-driven demo screen navigation.
- **Status:** COMPLETED
- **Updates:** Implemented DashboardScreen with SearchBar, CategoryFilterChips, DemoCard grid/list toggle, empty state, sample Compose demo screens (UI, Storage, Network, Location, Settings), standalone SampleDemoActivity in AndroidManifest.xml, state-driven AppNavigation, and verified build with assembleDebug and testDebugUnitTest.
- **Acceptance Criteria:**
  - Dashboard UI displays feature cards in a grid/list layout
  - Search bar and category filter chips dynamically filter displayed demos
  - Navigation 3 state-driven transition between launcher dashboard and individual demo screens works smoothly
- **Duration:** 5m 47s

### Task_3_RunAndVerify: Build and run the app, verify stability (no crashes), confirm alignment with user requirements for search, filtering, and demo navigation, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Task 3 successfully verified. App built and installed on target device. MainActivity launched cleanly with zero crashes or exceptions. All UI elements (SearchBar, Category Filter Chips, Demo Cards grid, Standalone Activity launcher) verified in runtime hierarchy. Unit test suite fully passed (6/6 tests).
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - All features (Registry, Search/Filter, Navigation) function without errors
- **Duration:** 1m 59s

### Task_4_IntegrateLockscreenFeatureAndPermissions: Port Lockscreen feature code from LockscreenTestApp into com.example.alldemo.features.lockscreen, including overlay service, screen receiver, latency logger, permission management, Compose UI dashboard screen, and AndroidManifest updates. Register Lockscreen Demo in FeatureRegistry.
- **Status:** COMPLETED
- **Updates:** Ported LockscreenTestApp codebase into com.example.alldemo.features.lockscreen (LockscreenOverlayService, ScreenReceiver, LockscreenOverlayView, LockscreenDemoScreen, LatencyLogger, PermissionUtils). Updated AndroidManifest.xml with SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, POST_NOTIFICATIONS permissions and service declaration. Registered 'Lockscreen Test & Overlay Service' in FeatureRegistryRepositoryImpl under SYSTEM_SERVICES. Connected route in AppNavigation. Verified with assembleDebug and testDebugUnitTest.
- **Acceptance Criteria:**
  - Lockscreen feature components (Service, Receiver, Latency Logger, UI) ported under com.example.alldemo.features.lockscreen
  - Permissions (SYSTEM_ALERT_WINDOW, POST_NOTIFICATIONS, USE_FULL_SCREEN_INTENT) declared in AndroidManifest.xml and handled in UI
  - Lockscreen Demo registered in FeatureRegistry and navigable from dashboard
- **Duration:** 5m 26s

### Task_5_RunAndVerifyLockscreenIntegration: Build and run the app, verify Lockscreen demo integration, test permission requests, overlay foreground service, latency logging, verify stability with no crashes, and ensure all unit tests pass.
- **Status:** COMPLETED
- **Updates:** Verified Lockscreen feature integration. APK successfully built and deployed. Lockscreen card appears on Dashboard and navigates seamlessly to LockscreenDemoScreen. Permission cards, Overlay Service toggles, overlay trigger, and Latency Summary rendering verified. Zero crashes in logcat. All unit tests passed.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - Lockscreen feature screen opens, permissions check/request work, and overlay service/latency logging operate properly
- **Duration:** 3m 15s

### Task_6_IntegrateLockTestAndVoiceSpeechFeatures: Integrate source code from LockTestApp (E:\AndroidStudioCode\LockTestApp) into com.example.alldemo.features.locktest and DemoVoiceSpeechRecognizer (E:\AndroidStudioCode\DemoVoiceSpeechRecognizer) into com.example.alldemo.features.voicespeech. Clean up redundant placeholder demo cards from FeatureRegistryRepositoryImpl, keeping only real feature entries. Update AndroidManifest.xml with required permissions/services and integrate into AppNavigation.
- **Status:** COMPLETED
- **Updates:** Ported LockTestApp into com.example.alldemo.features.locktest and DemoVoiceSpeechRecognizer into com.example.alldemo.features.voicespeech. Removed redundant sample cards from FeatureRegistryRepositoryImpl, leaving only real project entries (Lockscreen Overlay Service, Lock Test & Keyguard, Voice Speech Recognizer). Updated AndroidManifest.xml and AppNavigation. Verified build assembleDebug and unit tests.
- **Acceptance Criteria:**
  - LockTestApp and DemoVoiceSpeechRecognizer ported into com.example.alldemo.features.locktest and com.example.alldemo.features.voicespeech packages
  - Redundant placeholder demo entries removed from FeatureRegistryRepositoryImpl and real entries registered
  - Permissions, manifests, and AppNavigation updated accordingly
- **Duration:** 7m 12s

### Task_7_RunAndVerifyAllDemoIntegration: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements for LockTest and VoiceSpeech features, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Task 7 completed successfully. App built, deployed, and tested on device. Verified that redundant placeholder cards were removed and replaced with the 3 real project demo cards (Lockscreen Overlay Service, Lock Test & Keyguard Management, Voice Speech Recognizer). All 3 feature screens load cleanly, handle back-stack navigation, and operate without crashes. Unit tests pass.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - All integrated feature demos launch cleanly and function without errors
- **Duration:** 2m 39s

### Task_8_RefactorFeatureSubPackages: Refactor feature sub-packages under com.example.alldemo.features to match project names: rename lockscreen to lockscreentestapp, locktest to locktestapp, and voicespeech to demovoicespeechrecognizer. Update package declarations, imports, AndroidManifest.xml, AppNavigation, FeatureRegistryRepositoryImpl, and ViewModel references.
- **Status:** COMPLETED
- **Updates:** Successfully refactored feature packages under com.example.alldemo.features: lockscreen -> lockscreentestapp, locktest -> locktestapp, voicespeech -> demovoicespeechrecognizer. Updated package declarations, imports across the project, and AndroidManifest.xml. Verified build assembleDebug successfully.
- **Acceptance Criteria:**
  - Sub-packages under com.example.alldemo.features renamed to lockscreentestapp, locktestapp, and demovoicespeechrecognizer
  - All package headers, imports, AndroidManifest.xml, AppNavigation, and ViewModel references updated
  - Project builds without package or unresolved import errors
- **Duration:** 27m 39s

### Task_9_RunAndVerifyPackageRefactoring: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm all features in renamed packages function properly and unit tests pass.
- **Status:** COMPLETED
- **Updates:** Task 9 completed successfully. Verified app build, installation, launch stability, and navigation across renamed packages (lockscreentestapp, locktestapp, demovoicespeechrecognizer). Logcat shows zero crashes or errors. Unit tests passed.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - All integrated features in lockscreentestapp, locktestapp, and demovoicespeechrecognizer launch cleanly and function without errors
- **Duration:** 1m 38s

### Task_10_IntegrateThreeSpeechProjects: Integrate CloudWhisperDemo, AndroidNativeSpeechRecognizer, and VoskDemooo projects into packages com.example.alldemo.features.cloudwhisperdemo, com.example.alldemo.features.androidnativespeechrecognizer, and com.example.alldemo.features.voskdemooo. Update dependencies, AndroidManifest.xml, FeatureRegistryRepositoryImpl, and AppNavigation.
- **Status:** COMPLETED
- **Updates:** Successfully integrated CloudWhisperDemo (com.example.alldemo.features.cloudwhisperdemo), AndroidNativeSpeechRecognizer (com.example.alldemo.features.androidnativespeechrecognizer), and VoskDemooo (com.example.alldemo.features.voskdemooo). Added Vosk & JNA dependencies to app/build.gradle.kts, copied Vosk assets to src/main/assets, registered 3 demo cards in FeatureRegistryRepositoryImpl, connected routes in AppNavigation. Verified assembleDebug and testDebugUnitTest.
- **Acceptance Criteria:**
  - CloudWhisperDemo, AndroidNativeSpeechRecognizer, and VoskDemooo ported into target packages com.example.alldemo.features.cloudwhisperdemo, com.example.alldemo.features.androidnativespeechrecognizer, com.example.alldemo.features.voskdemooo
  - Dependencies, manifest permissions, components, and assets updated as required
  - 1 button/card per project registered in FeatureRegistry and wired in AppNavigation
- **Duration:** 7m 19s

### Task_11_RunAndVerifySpeechProjectsIntegration: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements for CloudWhisperDemo, AndroidNativeSpeechRecognizer, and VoskDemooo integrations, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Verified Task 11. APK successfully built and deployed to target device. All 3 new speech project cards (CloudWhisperDemo, AndroidNativeSpeechRecognizer, VoskDemooo) appear on the dashboard. Navigation routes connected with TopBar back actions. Logcat verified zero crashes. Unit tests passed.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - All 3 speech recognition demo modules launch and function without errors
- **Duration:** 2m 15s

### Task_12_IntegrateWakewordFUTOAndDeepgramProjects: Integrate WakewordVoiceprint (E:\AndroidStudioCode\WakewordVoiceprint), FUTOVoiceInputEngine (E:\AndroidStudioCode\FUTOVoiceInputEngine), and DeepgramNova2APIWebSocket (E:\AndroidStudioCode\DeepgramNova2APIWebSocket) into packages com.example.alldemo.features.wakewordvoiceprint, com.example.alldemo.features.futovoiceinputengine, and com.example.alldemo.features.deepgramnova2apiwebsocket. Register 1 button per project in FeatureRegistry, wire AppNavigation, update AndroidManifest.xml, and update app dependencies if needed.
- **Status:** COMPLETED
- **Updates:** Successfully ported WakewordVoiceprint (com.example.alldemo.features.wakewordvoiceprint), FUTOVoiceInputEngine (com.example.alldemo.features.futovoiceinputengine), and DeepgramNova2APIWebSocket (com.example.alldemo.features.deepgramnova2apiwebsocket). Registered 3 feature cards in FeatureRegistryRepositoryImpl, connected routes in AppNavigation, verified AndroidManifest permissions, and verified build assembleDebug and unit tests.
- **Acceptance Criteria:**
  - WakewordVoiceprint, FUTOVoiceInputEngine, and DeepgramNova2APIWebSocket ported into target packages com.example.alldemo.features.wakewordvoiceprint, com.example.alldemo.features.futovoiceinputengine, com.example.alldemo.features.deepgramnova2apiwebsocket
  - 1 button/card per project registered in FeatureRegistry and wired in AppNavigation
  - Dependencies, manifest permissions, components, and assets updated as required
- **Duration:** 13m 39s

### Task_13_RunAndVerifyNewSpeechAndVoiceIntegrations: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements for WakewordVoiceprint, FUTOVoiceInputEngine, and DeepgramNova2APIWebSocket integrations, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Verified Task 13. App built and installed on target device. WakewordVoiceprint, FUTOVoiceInputEngine, and DeepgramNova2APIWebSocket feature cards rendered on the dashboard and navigate cleanly with back button support. Zero crashes in logcat. All unit tests passed.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - All 3 new voice/speech feature modules launch and function without errors
- **Duration:** 2m 13s

