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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val textRecognizer = TextRecognizer()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var recognizedText by remember { mutableStateOf("") }
            val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
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
                        recognizedText = textRecognizer.recognizeText(bitmap)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                when {
                    cameraPermissionState.status.isGranted -> {
                        Column(
                            modifier = Modifier.fillMaxSize()
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
                                            recognizedText = textRecognizer.recognizeText(bitmap)
                                        }
                                    },
                                    onGalleryClick = {
                                        galleryLauncher.launch("image/*")
                                    }
                                )
                            }

                            // 识别结果显示区域 (Surface)
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
                                        text = if (recognizedText.isEmpty()) "识别结果将显示在这里" else recognizedText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
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

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: (Bitmap) -> Unit,
    onGalleryClick: () -> Unit
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
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    DisposableEffect(Unit) {
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
                Text("拍照识别")
            }
        }
    }
}