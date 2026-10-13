package com.example.alldemo.features.facerecognitionandroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kbyai.facesdk.FaceBox
import com.kbyai.facesdk.FaceDetectionParam
import com.kbyai.facesdk.FaceSDK
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import kotlin.random.Random

data class IdentifiedResult(
    val identifiedFace: Bitmap?,
    val enrolledFace: Bitmap?,
    val name: String,
    val similarity: Float,
    val liveness: Float,
    val yaw: Float,
    val roll: Float,
    val pitch: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRecognitionAndroidDemoScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sdkInitStatus by remember { mutableStateOf("Initializing FaceSDK...") }
    var sdkSuccess by remember { mutableStateOf(false) }

    var dbManager by remember { mutableStateOf<DBManager?>(null) }
    var personListState by remember { mutableStateOf<List<Person>>(emptyList()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var identifiedResult by remember { mutableStateOf<IdentifiedResult?>(null) }

    // Settings state
    var livenessThreshold by remember { mutableFloatStateOf(SettingsHelper.getLivenessThreshold(context)) }
    var identifyThreshold by remember { mutableFloatStateOf(SettingsHelper.getIdentifyThreshold(context)) }
    var cameraLensFacing by remember { mutableIntStateOf(SettingsHelper.getCameraLens(context)) }
    var livenessLevel by remember { mutableIntStateOf(SettingsHelper.getLivenessLevel(context)) }

    // Init FaceSDK & DB
    LaunchedEffect(Unit) {
        val licenseKey = "dYSREvlnlNxuMwFlDCngsmkG5rFIck95ymNvkPDeTUXt3Cj7y0sFIoYIuv3rXaeCb6Imf7lbr7r09S" +
                "sAPPPhL6oD1uCsdRqddQcMjzHThgjLBXjphSMnclb8SM8mzs/brmMZ+Ofu2p7nqKIy7zJASB3iRo2L5gy7e" +
                "hNvKUy4bSyt7n7xCz8PrGWmBnphupYbQJLGU24RdVN1suybukqjZX5ctUUu2sDSd2CawEDH7ftLyoLuFrG1v" +
                "YqExNq/FOhgBjzHgSmZ1tiZa+35rrU6kyzUG6O9Nl8A+Wr/lsV2QDjqn7iGgPmzimGL2pr7OLXJRUkOOWldW" +
                "detUeqohaI9eA=="

        var ret = FaceSDK.setActivation(licenseKey)
        if (ret == FaceSDK.SDK_SUCCESS) {
            ret = FaceSDK.init(context.assets)
        }

        if (ret == FaceSDK.SDK_SUCCESS) {
            sdkInitStatus = "FaceSDK Activated Successfully!"
            sdkSuccess = true
        } else {
            sdkSuccess = false
            sdkInitStatus = when (ret) {
                FaceSDK.SDK_LICENSE_KEY_ERROR -> "Invalid License Key!"
                FaceSDK.SDK_LICENSE_APPID_ERROR -> "Invalid App ID!"
                FaceSDK.SDK_LICENSE_EXPIRED -> "License Expired!"
                FaceSDK.SDK_NO_ACTIVATED -> "SDK Not Activated!"
                FaceSDK.SDK_INIT_ERROR -> "SDK Init Error!"
                else -> "FaceSDK Error Code: $ret"
            }
        }

        val db = DBManager(context)
        db.loadPerson()
        dbManager = db
        personListState = ArrayList(DBManager.personList)
    }

    // Photo picker for face enrollment
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = Utils.getCorrectlyOrientedImage(context, it)
                val faceDetectionParam = FaceDetectionParam().apply {
                    check_liveness = true
                    check_liveness_level = SettingsHelper.getLivenessLevel(context)
                }
                val faceBoxes = FaceSDK.faceDetection(bitmap, faceDetectionParam)

                if (faceBoxes.isNullOrEmpty()) {
                    Toast.makeText(context, "No face detected in image!", Toast.LENGTH_SHORT).show()
                } else if (faceBoxes.size > 1) {
                    Toast.makeText(context, "Multiple faces detected! Please choose a single face photo.", Toast.LENGTH_SHORT).show()
                } else {
                    val faceImage = Utils.cropFace(bitmap, faceBoxes[0])
                    val templates = FaceSDK.templateExtraction(bitmap, faceBoxes[0])
                    val newPersonName = "Person_${Random.nextInt(1000, 9999)}"

                    dbManager?.insertPerson(newPersonName, faceImage, templates)
                    personListState = ArrayList(DBManager.personList)
                    Toast.makeText(context, "Person enrolled successfully: $newPersonName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error enrolling face: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FaceRecognition-Android", fontWeight = FontWeight.Bold)
                        Text("On-device Live Face Matching & Liveness", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SDK Status Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (sdkSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (sdkSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (sdkSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sdkInitStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sdkSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Live Recognition") },
                    icon = { Icon(Icons.Rounded.Camera, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Enrolled (${personListState.size})") },
                    icon = { Icon(Icons.Rounded.PersonSearch, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Settings") },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> LiveRecognitionTab(
                        context = context,
                        cameraLensFacing = cameraLensFacing,
                        onFaceIdentified = { identified, enrolled, name, sim, live, yaw, roll, pitch ->
                            identifiedResult = IdentifiedResult(identified, enrolled, name, sim, live, yaw, roll, pitch)
                        }
                    )
                    1 -> EnrolledPersonsTab(
                        personList = personListState,
                        onEnrollClick = { photoPickerLauncher.launch("image/*") },
                        onDeletePerson = { name ->
                            dbManager?.deletePerson(name)
                            personListState = ArrayList(DBManager.personList)
                        },
                        onClearAll = {
                            dbManager?.clearDB()
                            personListState = ArrayList(DBManager.personList)
                        }
                    )
                    2 -> SettingsTab(
                        livenessThreshold = livenessThreshold,
                        onLivenessThresholdChange = {
                            livenessThreshold = it
                            SettingsHelper.setLivenessThreshold(context, it)
                        },
                        identifyThreshold = identifyThreshold,
                        onIdentifyThresholdChange = {
                            identifyThreshold = it
                            SettingsHelper.setIdentifyThreshold(context, it)
                        },
                        cameraLensFacing = cameraLensFacing,
                        onCameraLensChange = {
                            val lensStr = if (it == CameraSelector.LENS_FACING_BACK) "back" else "front"
                            cameraLensFacing = it
                            SettingsHelper.setCameraLens(context, lensStr)
                        },
                        livenessLevel = livenessLevel,
                        onLivenessLevelChange = {
                            livenessLevel = it
                            SettingsHelper.setLivenessLevel(context, it)
                        }
                    )
                }
            }
        }

        // Identification Result Dialog
        identifiedResult?.let { result ->
            AlertDialog(
                onDismissRequest = { identifiedResult = null },
                icon = { Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Face Match Identified!", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Enrolled Face", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                result.enrolledFace?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Enrolled Face",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Identified Live", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                result.identifiedFace?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Identified Face",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(2.dp, Color.Green, RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Name: ${result.name}", fontWeight = FontWeight.Bold)
                                Text("Similarity: ${(result.similarity * 100).toInt()}% (${String.format("%.4f", result.similarity)})")
                                Text("Liveness Score: ${String.format("%.2f", result.liveness)}")
                                Text("Pose: Yaw=${result.yaw.toInt()}°, Roll=${result.roll.toInt()}°, Pitch=${result.pitch.toInt()}°")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { identifiedResult = null }) {
                        Text("Continue Scanning")
                    }
                }
            )
        }
    }
}

@Composable
fun LiveRecognitionTab(
    context: Context,
    cameraLensFacing: Int,
    onFaceIdentified: (Bitmap, Bitmap?, String, Float, Float, Float, Float, Float) -> Unit
) {
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Camera, contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera permission is required for live recognition.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    } else {
        var recognizedState by remember { mutableStateOf(false) }
        val mainHandler = remember { Handler(Looper.getMainLooper()) }

        DisposableEffect(recognizedState) {
            onDispose { }
        }

        AndroidView(
            factory = { ctx ->
                val container = FrameLayout(ctx)
                val cameraView = CameraView(ctx)
                val faceView = FaceView(ctx)

                container.addView(cameraView)
                container.addView(faceView)

                val lens = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) back() else front()
                val fotoapparat = Fotoapparat.with(ctx)
                    .into(cameraView)
                    .lensPosition(lens)
                    .frameProcessor(object : FrameProcessor {
                        override fun process(frame: Frame) {
                            if (recognizedState) return

                            var cameraMode = 7
                            if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                                cameraMode = 6
                            }

                            val bitmap = FaceSDK.yuv2Bitmap(frame.image, frame.size.width, frame.size.height, cameraMode) ?: return

                            val faceDetectionParam = FaceDetectionParam().apply {
                                check_liveness = true
                                check_liveness_level = SettingsHelper.getLivenessLevel(ctx)
                            }
                            val faceBoxes = FaceSDK.faceDetection(bitmap, faceDetectionParam) ?: emptyList()

                            mainHandler.post {
                                faceView.setFrameSize(Size(bitmap.width, bitmap.height))
                                faceView.setFaceBoxes(faceBoxes)
                            }

                            if (faceBoxes.isNotEmpty()) {
                                val faceBox = faceBoxes[0]
                                if (faceBox.liveness > SettingsHelper.getLivenessThreshold(ctx)) {
                                    val templates = FaceSDK.templateExtraction(bitmap, faceBox) ?: return

                                    var maxSimilarity = 0f
                                    var maxPerson: Person? = null
                                    for (person in DBManager.personList) {
                                        if (person.templates != null) {
                                            val similarity = FaceSDK.similarityCalculation(templates, person.templates)
                                            if (similarity > maxSimilarity) {
                                                maxSimilarity = similarity
                                                maxPerson = person
                                            }
                                        }
                                    }

                                    if (maxSimilarity > SettingsHelper.getIdentifyThreshold(ctx) && maxPerson != null) {
                                        recognizedState = true
                                        val identifiedFace = Utils.cropFace(bitmap, faceBox)
                                        mainHandler.post {
                                            onFaceIdentified(
                                                identifiedFace,
                                                maxPerson?.face,
                                                maxPerson?.name ?: "Unknown",
                                                maxSimilarity,
                                                faceBox.liveness,
                                                faceBox.yaw,
                                                faceBox.roll,
                                                faceBox.pitch
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    })
                    .previewResolution { Resolution(1280, 720) }
                    .build()

                container.tag = fotoapparat
                fotoapparat.start()

                container
            },
            update = { container ->
                if (recognizedState) {
                    recognizedState = false
                }
            },
            onRelease = { container ->
                (container.tag as? Fotoapparat)?.stop()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun EnrolledPersonsTab(
    personList: List<Person>,
    onEnrollClick: () -> Unit,
    onDeletePerson: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onEnrollClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enroll Face")
            }

            if (personList.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Database")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (personList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.AccountBox,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No enrolled persons yet.", color = MaterialTheme.colorScheme.outline)
                    Text("Tap 'Enroll Face' to register a face from photo gallery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(personList) { person ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            person.face?.let { faceBm ->
                                Image(
                                    bitmap = faceBm.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } ?: Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.name ?: "Unknown",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Template size: ${person.templates?.size ?: 0} bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onDeletePerson(person.name ?: "") }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    livenessThreshold: Float,
    onLivenessThresholdChange: (Float) -> Unit,
    identifyThreshold: Float,
    onIdentifyThresholdChange: (Float) -> Unit,
    cameraLensFacing: Int,
    onCameraLensChange: (Int) -> Unit,
    livenessLevel: Int,
    onLivenessLevelChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Liveness Threshold: ${String.format("%.2f", livenessThreshold)}", fontWeight = FontWeight.SemiBold)
                Text("Higher threshold reduces spoofing false accepts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = livenessThreshold,
                    onValueChange = onLivenessThresholdChange,
                    valueRange = 0.0f..1.0f
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Identification Threshold: ${String.format("%.2f", identifyThreshold)}", fontWeight = FontWeight.SemiBold)
                Text("Minimum cosine similarity required for face match.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = identifyThreshold,
                    onValueChange = onIdentifyThresholdChange,
                    valueRange = 0.0f..1.0f
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Camera Lens", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = cameraLensFacing == CameraSelector.LENS_FACING_FRONT,
                        onClick = { onCameraLensChange(CameraSelector.LENS_FACING_FRONT) },
                        label = { Text("Front Camera") },
                        leadingIcon = { Icon(Icons.Rounded.Cameraswitch, contentDescription = null) }
                    )
                    FilterChip(
                        selected = cameraLensFacing == CameraSelector.LENS_FACING_BACK,
                        onClick = { onCameraLensChange(CameraSelector.LENS_FACING_BACK) },
                        label = { Text("Back Camera") },
                        leadingIcon = { Icon(Icons.Rounded.Camera, contentDescription = null) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Liveness Level", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = livenessLevel == 0,
                        onClick = { onLivenessLevelChange(0) },
                        label = { Text("Level 0 (Standard)") }
                    )
                    FilterChip(
                        selected = livenessLevel == 1,
                        onClick = { onLivenessLevelChange(1) },
                        label = { Text("Level 1 (Strict)") }
                    )
                }
            }
        }
    }
}
