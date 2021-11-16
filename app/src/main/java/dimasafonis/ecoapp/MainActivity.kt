package dimasafonis.ecoapp

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    private lateinit var camera: Intent
    private var data: Bitmap? = null
    private lateinit var preview: ImageView
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                data = result.data?.extras?.get("data") as Bitmap
                preview.setImageBitmap(data)
            }
        startCamera()
        }

    private lateinit var tess: TessBaseAPI
    private lateinit var tesseractPath: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        tesseractDataInit()
        tess.init(filesDir.absolutePath, "eng")
        preview = findViewById(R.id.cameraPreview)
        startCamera()

    }

    private fun startCamera() { resultLauncher.launch(camera) }

    private fun tesseractDataInit() {
        tesseractPath = File(this.filesDir.absolutePath, "tessdata")
        if (!tesseractPath.exists()) tesseractPath.mkdir()
        val engFile = File(tesseractPath.absolutePath, "eng.tessdata")
        if (!engFile.exists()) {
            engFile.createNewFile()
            engFile.writeBytes(assets.open("tesseract/eng.tessdata").readBytes())
            engFile.outputStream().flush()
        }
        else {
            if (!engFile.inputStream().readBytes().contentEquals(assets.open("tesseract/eng.tessdata").readBytes())) {
                engFile.writeBytes(assets.open("tesseract/eng.tessdata").readBytes())
                engFile.outputStream().flush()
            }
        }
    }

    @Suppress("unused_parameter")
    fun onCaptureTouch(view: View) {
        tess = TessBaseAPI()
        tess.setImage(data)
        val text = tess.utF8Text
        println(text)
        tess.end()
    }
}