package dimasafonis.ecoapp

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.hardware.camera2.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import java.io.File

import android.os.*
import android.provider.MediaStore
import android.provider.MediaStore.EXTRA_OUTPUT
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.googlecode.tesseract.android.TessBaseAPI


class CameraActivity : AppCompatActivity() {
//    private lateinit var dataFile: File
//    private var isFirstTime = true
//    private val data = object {
//        var data: MutableMap<String, String> = mutableMapOf()
//        fun save() {
//            var text = ""
//            data.forEach {
//                val keyValue = arrayOf(it.key, it.value)
//                text += keyValue.joinToString("=")
//                text += ";"
//            }
//            text.removeSuffix(";")
//            dataFile.writeBytes(text.toByteArray())
//        }
//        fun load() {
//            data = mutableMapOf()
//            if (dataFile.inputStream().available() == 0) return
//            val pairs = dataFile.readText().split(";")
//            pairs.forEach {
//                val keyValue = it.split("=")
//                data[keyValue[0]] = keyValue[1]
//            }
//        }
//        operator fun get(key: String): String {
//            return data[key] ?: ""
//        }
//        operator fun set(key: String, value: String) { data[key] = value }
//    }

    private lateinit var tess: TessBaseAPI
    private lateinit var tesseractPath: File
    private lateinit var tmp: File
    private val tag = "DimasafonisCodes"
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) capture()
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            Toast.makeText(this, "", Toast.LENGTH_LONG)
            finish()
        }
    }

    private var imageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        tmp = File(externalCacheDir!!, "tmp")
        if (!tmp.exists()) tmp.mkdir()
        tesseractPath = File(filesDir!!, "tesseract")
        if(!tesseractPath.exists()) tesseractPath.mkdir()

        if (Build.VERSION.SDK_INT >= 23)
            if (checkSelfPermission(CAMERA) == PERMISSION_DENIED) cameraPermission.launch(CAMERA)

        findViewById<MaterialButton>(R.id.capture).setOnClickListener { onCaptureTouch() }
    }

    override fun onResume() {
        super.onResume()
        tesseractDataInit()
    }

    private fun tesseractDataInit() {
        val tessdata = File(tesseractPath, "tessdata")
        if (!tessdata.exists()) tessdata.mkdir()
        if (!tessdata.isDirectory) {
            tessdata.delete()
            tessdata.mkdir()
        }

        val engFile = File(tessdata, "eng.traineddata")
        val eng = assets.open("tesseract/eng.traineddata").readBytes()

        if (!engFile.exists())
            engFile.createNewFile()
        if (!engFile.inputStream().readBytes().contentEquals(eng))
            engFile.writeBytes(eng)

    }

    private fun onCaptureTouch() {
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File.createTempFile("tesseract", ".jpg", tmp)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
        camera.putExtra(EXTRA_OUTPUT, uri)
        cameraLauncher.launch(camera)
    }

    private fun capture() {
        tess = TessBaseAPI()
        tess.init(tesseractPath.absolutePath, "eng")
        tess.setVariable("language_model_penalty_non_dict_word", "0")
        tess.setImage(imageFile)
        val text = tess.utF8Text
        Log.i("DimasafonisCodes", text)
        tess.stop()
        imageFile?.delete()
    }
}