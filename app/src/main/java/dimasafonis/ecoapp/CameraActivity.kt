package dimasafonis.ecoapp

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import java.io.File

import android.os.*
import android.provider.MediaStore
import android.provider.MediaStore.EXTRA_OUTPUT
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class CameraActivity : AppCompatActivity() {

    private lateinit var tmp: File
    private val tag = "DimasafonisCodes"
    lateinit var preview: ImageView
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
    private lateinit var codes: Codes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        tmp = File(externalCacheDir!!, "tmp")
        if (!tmp.exists()) tmp.mkdir()

        if (Build.VERSION.SDK_INT >= 23)
            if (checkSelfPermission(CAMERA) == PERMISSION_DENIED) cameraPermission.launch(CAMERA)

        findViewById<MaterialButton>(R.id.capture).setOnClickListener { onCaptureTouch() }
        preview = findViewById(R.id.preview)
        codes = Codes.load(assets.open("recycleCodes.yml"))
    }

    private fun onCaptureTouch() {
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File.createTempFile("imageText", ".jpg", tmp)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
        camera.putExtra(EXTRA_OUTPUT, uri)
        cameraLauncher.launch(camera)
    }

    private fun capture() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        preview.setImageBitmap(BitmapFactory.decodeFile(imageFile?.absolutePath))
        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(this, FileProvider.getUriForFile(this,
                "$packageName.provider", imageFile!!))
        }
        catch (e: Exception) {
            Log.e(tag, "Exception in method capture", e)
        }
        if (image == null) return
        recognizer.process(image)
            .addOnSuccessListener { text ->
                Log.i(tag, "Recognized text ${text.text}")
                var types = arrayOf("")
                var materialCount: UShort = 0.toUShort()
                var materialAfterRecycle: UShort = 0.toUShort()
                text.textBlocks.forEach {
                    val digits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
                    val chars =
                        arrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'g', 'k', 'l', 'm',
                            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z')
                    var containsDigits = false
                    var containsChars = false
                    var containsSlash = false
                    it.text.forEach { c ->
                        if (c in digits) containsDigits = true
                        if (c in chars) containsChars = true
                        if (c == '/') containsSlash = true
                    }
                    if (containsDigits && containsSlash && !containsChars) {
                        materialCount = it.text.split("/")[1].toUShort()
                        materialAfterRecycle = it.text.split("/")[0].toUShort()
                    }
                    if ((containsChars || containsDigits) && !containsSlash)
                        types += it.text
                }
                val getType = {
                    codes.codes.first {
                        it.codes.toList().containsAll(types.toList())
                    }
                }
                val getCategory = { code: Codes.Code ->
                    codes.categories.first { it.name == code.cat }
                }

                var message = ""
                val code = getType()
                message += "${getString(R.string.material_count)}: "
                message += if (materialCount == 0.toUShort()) getString("unknown") else materialCount.toString()
                message += "\n"
                message += "${getString(R.string.material_after_recycle)}: "
                message += if (materialAfterRecycle == 0.toUShort()) getString("unknown") else materialAfterRecycle.toString()
                message += "\n"
                message += "${getString(R.string.material_type)}: "
                message += getString(getCategory(code).res) + "\n"
                message += "${getString(R.string.material_subtype)}: "
                message += getString(code.res) + "\n"
                message += when (code.recycle) {
                    0f -> getString(R.string.not_recycle)
                    0.3f -> {
                        getString(R.string.dont_use) + "Эта упаковка " +
                                getString(R.string.bad) +
                                " и " +
                                getString(R.string.hard_to_recycle)
                    }
                    0.4f -> "Эта упаковка " + getString(R.string.bad)
                    0.5f -> getString(R.string.hard_to_find)
                    1f -> getString(R.string.use)
                    else -> throw RuntimeException("Recycle value not in 0..1")
                }

                AlertDialog.Builder(this)
                    .setTitle(R.string.recognized_text)
                    .setMessage(message)
                    .create().show()
            }
            .addOnFailureListener {
                Log.e(tag, "Cannot recognize text", it)
            }
        imageFile?.delete()
    }

    fun getString(res: String): String {
        return getString(R.string::class.java.getField(res).getInt(null))
    }
}