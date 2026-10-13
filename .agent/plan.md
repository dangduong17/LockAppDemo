# Project Plan

Tích hợp project mới FaceRecognition-Android (tại E:\AndroidStudioCode\Face\FaceRecognition-Android) vào AllDemo Hub. Đọc mã nguồn project, đặt tên package com.example.alldemo.features.facerecognitionandroid, tạo 1 nút/thẻ điều hướng riêng biệt trên màn hình chính AllDemo Hub.

## Project Brief

# Tài liệu Tóm tắt Dự án (Project Brief) - Tích hợp FaceRecognition-Android vào AllDemo Hub

## Mô tả tổng quan
Dự án thực hiện tích hợp tính năng từ mô-đun `FaceRecognition-Android` (tại đường dẫn `E:\AndroidStudioCode\Face\FaceRecognition-Android`) vào ứng dụng **AllDemo Hub**. Toàn bộ mã nguồn tính năng sẽ được tái cấu trúc dưới package `com.example.alldemo.features.facerecognitionandroid` và có nút/thẻ điều hướng riêng trên màn hình chính của AllDemo Hub.

---

## Features (Tính năng chính)

1. **Thẻ điều hướng riêng biệt trên AllDemo Hub**:
   - Cung cấp nút/thẻ điều hướng chuyên biệt tại màn hình chính của AllDemo Hub để người dùng chuyển đổi mượt mà sang tính năng Nhận diện khuôn mặt.
2. **Xem trước Camera & Phát hiện khuôn mặt thời gian thực (Real-time Face Detection)**:
   - Tích hợp luồng preview camera trực tiếp, tự động phát hiện vị trí và vẽ khung viền xung quanh khuôn mặt đang xuất hiện.
3. **Đăng ký khuôn mặt mới (Face Registration)**:
   - Hỗ trợ chụp và trích xuất thông tin khuôn mặt hiện tại, cho phép người dùng nhập tên/thông tin để lưu vào danh sách nhận diện.
4. **So khớp và Nhận diện khuôn mặt (Face Matching & Recognition)**:
   - Trích xuất định danh vector khuôn mặt, so sánh với các dữ liệu đã đăng ký và hiển thị nhãn tên người dùng ngay trên màn hình camera.

---

## High-Level Tech Stack (Công nghệ sử dụng)

- **Ngôn ngữ lập trình**: Kotlin
- **Giao diện người dùng (UI)**: Jetpack Compose (Material 3)
- **Điều hướng & Bố cục thích ứng (Navigation & Adaptive)**:
  - **Jetpack Navigation 3** (Điều hướng theo trạng thái - State-driven navigation)
  - **Compose Material Adaptive** (`androidx.compose.material3.adaptive`)
- **Quản lý Luồng & Bất đồng bộ**: Kotlin Coroutines & Flow
- **Kiến trúc hệ thống**: MVVM / Clean Architecture (ViewModel, StateFlow)
- **Camera & AI/ML**: CameraX, ML Kit Face Detection / TensorFlow Lite (Trích xuất & so khớp vector đặc trưng)
- **Package cấu trúc**: `com.example.alldemo.features.facerecognitionandroid`

## Implementation Steps
**Total Duration:** 2h 24m 25s

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

### Task_16_IntegrateSpeechBrainAndGeminiLiveVoiceProjects: Integrate SpeechBrainTest (E:\AndroidStudioCode\SpeechBrainTest) and GeminiMultimodalLiveVoiceAPI (E:\AndroidStudioCode\GeminiMultimodalLiveVoiceAPI) into packages com.example.alldemo.features.speechbraintest and com.example.alldemo.features.geminimultimodallivevoiceapi. Register 1 button per project in FeatureRegistry, update AndroidManifest and build dependencies, and wire AppNavigation.
- **Status:** COMPLETED
- **Updates:** Ported SpeechBrainTest into com.example.alldemo.features.speechbraintest and GeminiMultimodalLiveVoiceAPI into com.example.alldemo.features.geminimultimodallivevoiceapi. Registered 2 separate demo buttons in FeatureRegistryRepositoryImpl, connected routes in AppNavigation, verified AndroidManifest permissions, and verified assembleDebug build and testDebugUnitTest unit tests.
- **Acceptance Criteria:**
  - SpeechBrainTest and GeminiMultimodalLiveVoiceAPI ported into packages com.example.alldemo.features.speechbraintest and com.example.alldemo.features.geminimultimodallivevoiceapi
  - 1 button/card per project registered in FeatureRegistry and wired in AppNavigation
  - Dependencies, manifest permissions, components, and API key configurations updated as required
- **Duration:** 19m 44s

### Task_17_RunAndVerifySpeechBrainAndGeminiIntegrations: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements for SpeechBrainTest and GeminiMultimodalLiveVoiceAPI integrations, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Verified Task 17. AssemblyDebug build and testDebugUnitTest unit tests passed 100%. Pushed commit 976b4cb to GitHub repository https://github.com/dangduong17/LockAppDemo.git main branch.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - Both new feature modules (SpeechBrain and Gemini Live Voice API) launch and function without errors
- **Duration:** 1m 6s

### Task_18_IntegrateFaceRecognitionAndroid: Port native facesdk library, assets, and source code from FaceRecognition-Android (E:\AndroidStudioCode\Face\FaceRecognition-Android) into package com.example.alldemo.features.facerecognitionandroid. Add Camera permission, CameraX, ML Kit, and TensorFlow Lite dependencies in build.gradle.kts and AndroidManifest.xml, register 1 feature card in FeatureRegistryRepositoryImpl, and wire AppNavigation.
- **Status:** COMPLETED
- **Updates:** Successfully integrated FaceRecognition-Android. Copied facesdk.aar and fotoapparat.aar to app/libs, model bin/param assets to src/main/assets, ported FaceSDK wrappers, DBManager, FaceView, and FaceRecognitionAndroidDemoScreen into com.example.alldemo.features.facerecognitionandroid. Registered feature card in FeatureRegistryRepositoryImpl, added Camera permission in AndroidManifest.xml, connected route in AppNavigation, and verified assembleDebug build and testDebugUnitTest.
- **Acceptance Criteria:**
  - FaceRecognition-Android code, native libraries, and assets ported into com.example.alldemo.features.facerecognitionandroid
  - 1 feature card registered in FeatureRegistry and wired in AppNavigation
  - Camera permissions, dependencies, and native libs configured properly
- **Duration:** 23m 8s

### Task_19_RunAndVerifyFaceRecognitionIntegration: Build and run the app, instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements for FaceRecognition-Android integration, and report critical UI issues.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - FaceRecognition feature screen launches cleanly and operates without errors
- **StartTime:** 2026-10-13 10:25:52 GMT+07:00

