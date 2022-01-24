package dimasafonis.ecoapp

import android.Manifest.permission.CAMERA
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.EXTRA_OUTPUT
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

open class CameraActivity : AppCompatActivity() {
    lateinit var cameraExecutor: ExecutorService
    var imageCapture: ImageCapture? = null

    var firstTime = true

    private lateinit var tmp: File
    companion object {
        @JvmStatic
        private val TAG = "DimasafonisCodes"
    }

    lateinit var capture: ImageButton

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            Toast.makeText(this, "", Toast.LENGTH_LONG)
            finish()
        }
    }

    private var wasEditor = false
    var imageFile: File? = null
    var imageUri: Uri? = null
    private lateinit var codes: Codes

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.localNightMode = MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        tmp = File(externalCacheDir!!, "tmp")
        if (!tmp.exists()) tmp.mkdir()

        if (File(filesDir, "data").exists()) firstTime = false
        else File(filesDir, "data").createNewFile()

        if (Build.VERSION.SDK_INT >= 23)
            if (checkSelfPermission(CAMERA) == PERMISSION_DENIED) cameraPermission.launch(CAMERA)

        startCamera()

        capture = findViewById(R.id.capture)

        capture.setOnClickListener { capture() }

        cameraExecutor = Executors.newSingleThreadExecutor()
        codes = Codes.load(assets.open("recycleCodes.json"))
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(findViewById<PreviewView>(R.id.preview).surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val selector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture)
            } catch (e: Exception) { Log.e(TAG, "Failed to bind camera to lifecycle", e) }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun capture() {
        val imageCapture = imageCapture ?: return

        imageFile = File.createTempFile("imageText", ".jpg", tmp)

        val options = ImageCapture.OutputFileOptions.Builder(imageFile!!).build()

        val imageCallback = object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                TODO("Not yet implemented")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Error when capturing image", exception)
            }
        }

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this), imageCallback)

        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
    }

    private val imageEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        mainProcess()
    }

    private fun preProcess() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.CustomDialog))
            .setTitle("Обработка")
            .setMessage("Вы хотите обработать это изображение? (Это может повысить качество распознавания)")
            .setPositiveButton("Да") { dialog, _ ->
                Intent(ACTION_EDIT).also {
                    it.setDataAndType(imageUri, "image/*")
                    it.putExtra(EXTRA_OUTPUT, imageUri)
                    it.flags = FLAG_GRANT_WRITE_URI_PERMISSION or FLAG_GRANT_READ_URI_PERMISSION
                    try {
//                        startActivity(this)
                        imageEditor.launch(it)
                    } catch (e: ActivityNotFoundException) {
                        AlertDialog.Builder(this@CameraActivity)
                            .setTitle("Не получается найти подходящее приложение")
                            .setMessage("Попробуйте установить приложение для редактирования фотографий")
                    }
                }
                dialog.cancel()
                wasEditor = true
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.cancel()
                mainProcess()
            }
            .show()
    }

//    override fun onResume() {
//        super.onResume()
//        if (wasEditor) {
//            mainProcess()
//            wasEditor = false
//        }
//    }

    private fun mainProcess() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        capture.layoutParams = capture.layoutParams.also { (it as ConstraintLayout.LayoutParams).verticalBias = 0.9f }

        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(this, imageUri!!)
        }
        catch (e: Exception) {
            Log.e(TAG, "Exception in method mainProcess", e)
        }
        if (image == null) return

        recognizer.process(image)
            .addOnSuccessListener exitLambda@{ text ->
                val typesHashes = setOf(
                    *codes.codes.run {
                        val outHashes = arrayListOf<Int>()
                        forEach {
                            it.codes.forEach { code -> outHashes.add(code.hashCode()) }
                        }
                        outHashes.toIntArray().toTypedArray()
                    },
                    *codes.categories.run {
                        val outHashes = arrayListOf<Int>()
                        forEach {
                            it.codes.forEach { code -> outHashes.add(code.hashCode()) }
                        }
                        outHashes.toIntArray().toTypedArray()
                    }
                )
                Log.i(TAG, "Recognised Text:")
                for (i in text.textBlocks.indices) {
                    Log.i(TAG, "  Text Block $i")
                    for (j in text.textBlocks[i].lines.indices) {
                        Log.i(TAG, "    Line $j")
                        Log.i(TAG, "      ${text.textBlocks[i].lines[j].text}")
                    }
                }
                val types = arrayListOf<String>()
                var materialCount: Short = 0
                var materialAfterRecycle: Short = 0
                text.textBlocks.forEach { block ->
                    block.lines.forEach {
                        val digits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
                        val chars =
                            arrayOf(
                                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'g', 'k', 'l', 'm',
                                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
                            )
                        var containsDigits = false
                        var containsChars = false
                        var containsSlash = false
                        it.text.forEach { c ->
                            if (c in digits) containsDigits = true
                            if (c.lowercaseChar() in chars) containsChars = true
                            if (c == '/') containsSlash = true
                        }
                        if (containsDigits && containsSlash && !containsChars) {
                            val values = it.text.split("/")
                            materialCount = values[1].toShort()
                            materialAfterRecycle = values[0].toShort()
                        }
                        if ((containsChars || containsDigits) && !containsSlash)
                            if (typesHashes.contains(it.text.hashCode()))
                                types += it.text
                    }
                }
                System.gc()
                var code: Codes.Code? = null
                var category: Codes.Category? = null
                var firstTryWorked = false
                try{
                    code = codes.codes.first {
                        var ret = false
                        it.codes.forEach exit@{
                            types.forEach { type ->
                                if (it == type) {
                                    ret = true
                                    return@exit
                                }
                            }
                        }
                        ret
                    }
                }
                catch (e: Exception) { firstTryWorked = true }
                try {
                    category = codes.categories.first {
                        var ret = false
                        it.codes.forEach exit@{
                            types.forEach { type ->
                                if (it == type) {
                                    ret = true
                                    return@exit
                                }
                            }
                        }
                        ret
                    }
                }
                catch (e: Exception) {
                    Log.e(TAG, "Recognition error:", e)
                    if (firstTryWorked){
                        AlertDialog.Builder(this)
                            .setTitle("Ошибка")
                            .setMessage("При распозновании произошла ошибка, попробуйте ещё раз")
                            .create().show()
                        return@exitLambda
                    }
                }

                var message = ""
                message += "${getString(R.string.material_count)}: "
                message += if (materialCount == 0.toShort()) getString("unknown") else materialCount.toString()
                message += "\n"
                message += "${getString(R.string.material_after_recycle)}: "
                message += if (materialAfterRecycle == 0.toShort()) getString("unknown") else materialAfterRecycle.toString()
                message += "\n"
                message += "${getString(R.string.material_type)}: "
                message += getString(
                    if (category?.name == null) {
                        if (code?.cat == null) "unknown" else code.cat }
                    else category.name
                ) + "\n"
                message += "${getString(R.string.material_subtype)}: "
                message += getString(code?.res ?: "unknown") + "\n"
                message += when (code?.recycle) {
                    0f -> getString(R.string.not_recycle)
                    0.3f -> {
                        getString(R.string.dont_use) + "Эта упаковка " +
                                getString(R.string.bad) +
                                ", а также " +
                                getString(R.string.hard_to_recycle)
                    }
                    0.4f -> "Эта упаковка " + getString(R.string.bad)
                    0.5f -> getString(R.string.hard_to_find)
                    1f -> getString(R.string.use)
                    null -> getString("unknown")
                    else -> throw RuntimeException("Recycle value not in 0..1")
                }

                AlertDialog.Builder(this)
                    .setTitle(R.string.recognized_text)
                    .setMessage(message)
                    .create().show()
            }
            .addOnFailureListener {
                Log.e(TAG, "Cannot recognize text", it)
            }
    }

    private fun getString(res: String): String {
        return getString(R.string::class.java.getField(res).getInt(null))
    }
}