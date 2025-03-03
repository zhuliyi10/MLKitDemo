package com.leon.mlkitdemo

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

// Add these imports at the top
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.ScrollableTabRow

class MainActivity : ComponentActivity() {
    private val textRecognizer = TextRecognizer()
    private val faceDetector = FaceDetector()
    private val qrCodeScanner = QRCodeScanner()
    // Remove speechRecognizer property

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove speechRecognizer initialization
        setContent {
            MaterialTheme {
                var recognizedText by remember { mutableStateOf("") }
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf("文字识别", "人脸检测", "二维码扫描")  // Remove 语音识别
                val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
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
                                MediaStore.Images.Media.getBitmap(context.contentResolver, selectedUri)
                            }
                            recognizedText = when (selectedTabIndex) {
                                0 -> textRecognizer.recognizeText(bitmap)
                                1 -> faceDetector.detectFaces(bitmap)
                                2 -> qrCodeScanner.scanQRCode(bitmap)
                                else -> ""
                            }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        cameraPermissionState.status.isGranted -> {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Surface(
                                    shadowElevation = 8.dp,
                                    tonalElevation = 8.dp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(1f)  // 确保显示在最上层
                                ) {
                                    ScrollableTabRow(
                                        selectedTabIndex = selectedTabIndex,
                                        modifier = Modifier.fillMaxWidth(),
                                        edgePadding = 0.dp  // 移除边缘padding
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

                                // 相机预览和结果显示区域
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        CameraPreview(  // Remove speech recognition UI condition
                                            modifier = Modifier.fillMaxSize(),
                                            onImageCaptured = { bitmap ->
                                                scope.launch {
                                                    recognizedText = when (selectedTabIndex) {
                                                        0 -> textRecognizer.recognizeText(bitmap)
                                                        1 -> faceDetector.detectFaces(bitmap)
                                                        2 -> qrCodeScanner.scanQRCode(bitmap)
                                                        else -> ""
                                                    }
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
                                            useFrontCamera = selectedTabIndex == 1  // 人脸检测时使用前置摄像头
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shadowElevation = 4.dp
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = if (recognizedText.isEmpty()) {
                                                    when (selectedTabIndex) {
                                                        0 -> "识别结果将显示在这里"
                                                        1 -> "人脸检测结果将显示在这里"
                                                        2 -> "二维码扫描结果将显示在这里"
                                                        else -> ""
                                                    }
                                                } else recognizedText,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .verticalScroll(rememberScrollState())
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(
                                                            onLongPress = {
                                                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                val clip = ClipData.newPlainText("扫描结果", recognizedText)
                                                                clipboardManager.setPrimaryClip(clip)
                                                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    }
                                            )
                                        }
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