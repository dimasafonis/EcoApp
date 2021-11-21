package dimasafonis.ecoapp

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.ImageButton
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

class CameraActivity : AppCompatActivity() {
    private lateinit var preview: TextureView
    private lateinit var cm: CameraManager // cameraManager
    private lateinit var tess: TessBaseAPI
    private lateinit var tesseractPath: File
    private var cameras = arrayListOf<Camera>()
    private var currentCamera: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(CAMERA) != PERMISSION_GRANTED)
                requestPermissions(arrayOf(CAMERA), 1)
        }

        cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cm.cameraIdList.forEach{ id ->
            cameras.add(Camera(id, cm, this))
        }
        preview = findViewById(R.id.preview)
        findViewById<ImageButton>(R.id.capture).setOnClickListener { onCaptureTouch() }
        findViewById<ImageButton>(R.id.changeCamera).setOnClickListener {
            if (cameras[currentCamera].isOpen()) cameras[currentCamera].closeCamera()
            if (++currentCamera > cameras.size - 1) {
                currentCamera = 0
            }
            cameras[currentCamera].openCamera()
        }
        tesseractDataInit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1)
            if (grantResults[0] != PERMISSION_GRANTED) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.camera_required)
                    .setTitle(R.string.camera_required_title)
                    .create().show()
                if (Build.VERSION.SDK_INT >= 23) requestPermissions(arrayOf(CAMERA), 1)
            }
    }

    private fun tesseractDataInit() {
        tesseractPath = File(this.cacheDir.absolutePath, "tessdata")
        if (!tesseractPath.exists()) tesseractPath.mkdir()

        val engFile = File(tesseractPath.absolutePath, "eng.traineddata")
        val eng = assets.open("tesseract/eng.traineddata").readBytes()

        if (!engFile.exists()) {
            engFile.createNewFile()
            engFile.writeBytes(eng)
        }
        else {
            if (!engFile.inputStream().readBytes().contentEquals(eng))
                engFile.writeBytes(eng)
        }
    }

    fun onCaptureTouch() {
        tess = TessBaseAPI()
        tess.init(cacheDir.absolutePath, "eng")
        tess.setVariable("language_model_penalty_non_dict_word", "1")
        tess.setImage(preview.bitmap)
        val text = tess.utF8Text
        Log.i("DimasafonisCodes", text)
        tess.end()
    }

    class Camera(var id: String, var cm: CameraManager, val activity: CameraActivity) {

        var cameraDevice: CameraDevice? = null
        lateinit var captureSession: CameraCaptureSession
        val callback = CameraCallback(this)
        lateinit var imageReader: ImageReader

        fun isOpen() : Boolean { return cameraDevice != null }

        @RequiresPermission(value = CAMERA)
        fun openCamera() {
            cm.openCamera(id, callback, null)
            startPreview()
        }

        fun closeCamera() {
            if (cameraDevice != null) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }

        fun startPreview() {
            activity.preview.surfaceTexture?.setDefaultBufferSize(1920, 1080)
            val surface = Surface(activity.preview.surfaceTexture)
            val builder = cameraDevice?.createCaptureRequest(TEMPLATE_PREVIEW)
            builder?.addTarget(surface)
            @Suppress("deprecation")
            if (Build.VERSION.SDK_INT >= 28) {
                cameraDevice?.createCaptureSession(SessionConfiguration(
                    SESSION_HIGH_SPEED,
                    arrayListOf(OutputConfiguration(surface)),
                    { it.run() },
                    CaptureCallback(this, builder as CaptureRequest.Builder)))
            } else {
                cameraDevice?.createCaptureSession(
                    arrayOf(surface).toMutableList(),
                    CaptureCallback(this, builder as CaptureRequest.Builder),
                    null
                )
            }
        }

        class CaptureCallback(val camera: Camera, val builder: CaptureRequest.Builder) : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                camera.captureSession = session
                camera.captureSession.setRepeatingRequest(builder.build(), null, null)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }

        class CameraCallback(val camera: Camera) : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                camera.cameraDevice = device
                camera.startPreview()
                Log.i(null, "Camera ${device.id} opened")
            }

            override fun onDisconnected(device: CameraDevice) {
                device.close()
                camera.cameraDevice = null
            }

            override fun onError(device: CameraDevice, error: Int) {
                Log.e("DimasafonisCodes", "Error with ${device.id}: $error")
            }

        }
    }
}