package com.leon.mlkitdemo

// Add these imports at the top

// 添加这些新的导入
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivity : ComponentActivity() {
    private val textRecognizer = TextRecognizer()
    private val faceDetector = FaceDetector()
    private val qrCodeScanner = QRCodeScanner()
    // Remove speechRecognizer property

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 添加这些窗口设置
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Remove speechRecognizer initialization
        setContent {
            MaterialTheme {
                // 添加系统UI控制器设置
                val systemUiController = rememberSystemUiController()
                DisposableEffect(systemUiController) {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,  // 使用 Compose 的 Color.Transparent
                        darkIcons = true
                    )
                    onDispose {}
                }

                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf("文字识别", "人脸检测", "二维码扫描")  // Remove 语音识别
                val cameraPermissionState =
                    rememberPermissionState(android.Manifest.permission.CAMERA)
                // Remove micPermissionState
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { selectedUri ->
                        scope.launch {
                            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(
                                        context.contentResolver,
                                        selectedUri
                                    )
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(
                                    context.contentResolver,
                                    selectedUri
                                )
                            }
                            val result = when (selectedTabIndex) {
                                0 -> textRecognizer.recognizeText(bitmap)
                                1 -> faceDetector.detectFaces(bitmap)
                                2 -> qrCodeScanner.scanQRCode(bitmap)
                                else -> ""
                            }
                            navigateToResult(bitmap, result)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        cameraPermissionState.status.isGranted -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                // 相机预览
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    CameraPreview(
                                        modifier = Modifier.fillMaxSize(),
                                        onImageCaptured = { bitmap ->
                                            scope.launch {
                                                val result = when (selectedTabIndex) {
                                                    0 -> textRecognizer.recognizeText(bitmap)
                                                    1 -> faceDetector.detectFaces(bitmap)
                                                    2 -> qrCodeScanner.scanQRCode(bitmap)
                                                    else -> ""
                                                }
                                                navigateToResult(bitmap, result)
                                            }
                                        },
                                        onGalleryClick = {
                                            galleryLauncher.launch("image/*")
                                        },
                                        captureButtonText = when (selectedTabIndex) {
                                            0 -> "拍照识别文字"
                                            1 -> "拍照检测人脸"
                                            2 -> "拍照扫描二维码"
                                            else -> ""
                                        },
                                        useFrontCamera = selectedTabIndex == 1
                                    )
                                }

                                // Tab栏
                                ScrollableTabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    modifier = Modifier.fillMaxWidth(),
                                    edgePadding = 0.dp,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.95f
                                    )
                                ) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = {
                                                Text(
                                                    text = title,
                                                    color = if (selectedTabIndex == index)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    else
                                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                            alpha = 0.6f
                                                        )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                                Text("Request Camera Permission")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap): Uri {
        val tempFile = File(cacheDir, "temp_image.jpg")
        tempFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            tempFile
        )
    }

    private fun navigateToResult(bitmap: Bitmap, result: String) {
        val imageUri = saveBitmapToTempFile(bitmap)
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("imageUri", imageUri.toString())
            putExtra("result", result)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Bitmap) -> Unit,
    onGalleryClick: () -> Unit,
    captureButtonText: String,
    useFrontCamera: Boolean = false  // 添加参数控制使用前置还是后置摄像头
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraSelector = if (useFrontCamera) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    DisposableEffect(useFrontCamera) {  // 改变这里，让它响应 useFrontCamera 的变化
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)

        onDispose {
            cameraProvider.unbindAll()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 底部按钮栏
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onGalleryClick() }
            ) {
                Text("从相册选择")
            }

            Button(
                onClick = {
                    scope.launch {
                        val bitmap = previewView.bitmap
                        bitmap?.let { bmp ->
                            onImageCaptured(bmp)
                        }
                    }
                }
            ) {
                Text(captureButtonText)
            }
        }
    }
}