package com.parasde.example

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.activity_camera.*
import org.opencv.android.*
import org.opencv.core.Mat

class CameraActivity: AppCompatActivity() {
    private val tag = "[CameraActivity]"

    private lateinit var launcher: ActivityResultLauncher<Array<String>>

    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("camera")
    }

    // 원본 카메라 사진 Mat
    private var input: Mat? = null
    // 라벨링 후 카메라 사진 Mat
    private var output: Mat? = null
    // 라벨링 위치 값 저장
    private var cropRect = IntArray(4) {0}
    private external fun detection(input: Long, output: Long, crop: IntArray)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        launcher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    allGranted = false
                }
            }
            if (allGranted) {
                cameraStart()
            } else {
                Toast.makeText(this, R.string.permission_msg, Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        launcher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    private fun cameraStart () {
        val params = camera_layout.resources.displayMetrics
        camera_guide.layoutParams.width = params.widthPixels * 7/10
        camera_guide.layoutParams.height = params.heightPixels * 8/10

        camera.setCameraPermissionGranted()
        camera.visibility = SurfaceView.VISIBLE
        camera.setCameraIndex(1)
        camera.setCvCameraViewListener(cameraViewListener)
        capture.setOnClickListener {
            capture.visibility = View.GONE
            camera.disableView()
        }
        Toast.makeText(this, R.string.capture_toast, Toast.LENGTH_LONG).show()
    }

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    // 카메라 뷰 시작
                    camera.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        camera.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    private val cameraRestart = Runnable {
        camera.enableView()
        capture.visibility = View.VISIBLE
    }

    private val cameraViewListener = object: CameraBridgeViewBase.CvCameraViewListener2 {
        override fun onCameraViewStarted(width: Int, height: Int) {}

        // 카메라 뷰 stopped 시 이미지 crop 작업
        override fun onCameraViewStopped() {
            // 최종적으로 자른 이미지 상태 확인
            if (cropRect[0] == 0 || cropRect[1] == 0 || cropRect[2] == 0 || cropRect[3] == 0) {
                Toast.makeText(this@CameraActivity, "Empty", Toast.LENGTH_SHORT).show();
                Handler(mainLooper).postDelayed(cameraRestart, 1000)
                return
            }

            Handler(mainLooper).post {
                val bitmap = Bitmap.createBitmap(output!!.cols(), output!!.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(output, bitmap)
                camera.visibility = View.GONE
                val crop = Bitmap.createBitmap(bitmap, cropRect[2] + 10, cropRect[3] + 10, cropRect[0] - 10, cropRect[1] - 10)
                preview.visibility = View.VISIBLE
                preview.setImageBitmap(crop)

                btnll.visibility = View.VISIBLE
                // 저장
                captureSave.setOnClickListener {
                    // crop bitmap saved
                }
                // 재촬영
                reCapture.setOnClickListener {
                    camera.visibility = View.VISIBLE
                    preview.visibility = View.GONE
                    btnll.visibility = View.GONE
                    Handler(mainLooper).postDelayed(cameraRestart, 1000)
                }
            }
        }

        override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
            if (inputFrame !== null) {
                input = inputFrame.rgba()
                if (output == null) output = Mat(input!!.rows(), input!!.cols(), input!!.type())

                detection(input!!.nativeObjAddr, output!!.nativeObjAddr, cropRect)
            }
            return output!!
        }

    }
}