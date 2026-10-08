# Project Plan

Tích hợp 2 project mới vào AllDemo Hub: RevAIAssemblyAIStreamingWebSocketAPI (tại E:\AndroidStudioCode\RevAIAssemblyAIStreamingWebSocketAPI) và HuggingFaceTransformersjsONNXWebRuntime (tại E:\AndroidStudioCode\HuggingFaceTransformersjsONNXWebRuntime). Đọc mã nguồn các project, đặt tên package khớp chính xác tên project (com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi, com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime), tạo 2 nút/thẻ điều hướng tương ứng trên màn hình chính AllDemo Hub.

## Project Brief

# Project Brief: Tích hợp Module AllDemo Hub

## Mô tả Dự án (Project Overview)
Dự án nhằm hợp nhất hai module tính năng mới vào ứng dụng **AllDemo Hub**:
1. **RevAIAssemblyAIStreamingWebSocketAPI**: Xử lý nhận diện giọng nói theo thời gian thực qua giao thức WebSocket với Rev AI và AssemblyAI APIs.
2. **HuggingFaceTransformersjsONNXWebRuntime**: Chạy các mô hình AI/ML trên thiết bị (On-device) bằng HuggingFace Transformers.js và ONNX Web Runtime.

Mã nguồn được tích hợp vào ứng dụng AllDemo Hub theo đúng cấu trúc package quy định (`com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi` và `com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime`), đi kèm với các thẻ điều hướng tương ứng trên màn hình chính.

---

## Các tính năng chính (Features)

1. **Màn hình điều hướng trung tâm AllDemo Hub (Hub Dashboard & Navigation)**
   - Hiển thị danh sách các module dưới dạng các thẻ (Cards)/nút điều hướng chuẩn Material Design 3.
   - Cho phép người dùng chuyển đổi linh hoạt giữa các tính năng thử nghiệm.

2. **Module Rev AI & AssemblyAI Streaming WebSocket**
   - Thu âm micro và truyền luồng dữ liệu âm thanh (audio streaming) liên tục qua WebSocket.
   - Xử lý và hiển thị kết quả chuyển đổi giọng nói thành văn bản (Speech-to-Text) theo thời gian thực từ Rev AI và AssemblyAI.

3. **Module Hugging Face Transformers.js ONNX Web Runtime**
   - Thực thi các mô hình AI/ML trực tiếp trên ứng dụng thông qua môi trường Web Runtime tích hợp ONNX Runtime & Transformers.js.
   - Tương tác dữ liệu hai chiều mượt mà giữa giao diện Jetpack Compose và môi trường chạy mô hình AI.

4. **Quản lý kết nối & Trạng thái thời gian thực (Real-time State Management)**
   - Quản lý vòng đời kết nối WebSocket (kết nối, ngắt kết nối, tự động xử lý lỗi mạng).
   - Phản hồi trạng thái xử lý và kết quả lên giao diện người dùng dựa trên Kotlin State & Flow.

---

## Công nghệ & Kiến trúc (High-Level Technical Stack)

- **Ngôn ngữ lập trình**: Kotlin
- **Giao diện người dùng (UI)**: Jetpack Compose, Material Design 3
- **Điều hướng (Navigation)**: **Jetpack Navigation 3** (State-driven navigation)
- **Giao diện thích ứng (Adaptive Layout)**: **Compose Material Adaptive** (`androidx.compose.material3.adaptive`)
- **Bất đồng bộ & Luồng dữ liệu**: Kotlin Coroutines & Flow
- **Mạng & Truyền dữ liệu thời gian thực**: OkHttp WebSocket Client
- **Môi trường chạy mô hình AI**: Android WebKit WebView / JavaScript Interface cho ONNX Web Runtime & Transformers.js
- **Mô hình kiến trúc**: Unidirectional Data Flow (UDF) với ViewModel và Compose State Management

## Implementation Steps
**Total Duration:** 1h 40m 27s

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

### Task_14_IntegrateRevAIAndHuggingFaceProjects: Integrate RevAIAssemblyAIStreamingWebSocketAPI (E:\AndroidStudioCode\RevAIAssemblyAIStreamingWebSocketAPI) and HuggingFaceTransformersjsONNXWebRuntime (E:\AndroidStudioCode\HuggingFaceTransformersjsONNXWebRuntime) into packages com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi and com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime. Register 1 button per project in FeatureRegistry, update AndroidManifest and build dependencies, and wire AppNavigation.
- **Status:** COMPLETED
- **Updates:** Successfully integrated RevAIAssemblyAIStreamingWebSocketAPI (com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi) and HuggingFaceTransformersjsONNXWebRuntime (com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime). Copied assets, registered 2 feature cards in FeatureRegistryRepositoryImpl, connected routes in AppNavigation, verified AndroidManifest permissions, and verified build assembleDebug and unit tests (10/10 passed).
- **Acceptance Criteria:**
  - Code ported into com.example.alldemo.features.revaiassemblyaistreamingwebsocketapi and com.example.alldemo.features.huggingfacetransformersjsonnxwebruntime
  - 1 button/card per project registered in FeatureRegistry and wired in AppNavigation
  - Dependencies, manifest permissions, and assets updated as required
- **Duration:** 11m 55s

### Task_15_RunAndVerifyRevAIAndHuggingFaceIntegrations: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements for RevAIAssemblyAIStreamingWebSocketAPI and HuggingFaceTransformersjsONNXWebRuntime integrations, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Verified Task 15. App built and installed on target device. RevAI & AssemblyAI Streaming WebSocket and Hugging Face Transformers.js ONNX Web Runtime feature cards rendered on dashboard and navigate cleanly. Zero crashes in logcat. All unit tests passed.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - Both new feature modules (RevAI & HuggingFace ONNX) launch and function without errors
- **Duration:** 3m 6s

