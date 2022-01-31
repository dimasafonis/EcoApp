package dimasafonis.ecoapp

import android.Manifest.permission.CAMERA
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.EXTRA_OUTPUT
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class CameraActivity : AppCompatActivity() {

    private var lastResultX = 0
    private var lastResultY = 0
    var firstTime = true

    private lateinit var tmp: File
    private val tag = "DimasafonisCodes"
    lateinit var preview: ImageView
    lateinit var capture: Button
    lateinit var results: GridLayout
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) preProcess()
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            Toast.makeText(this, "", Toast.LENGTH_LONG)
            finish()
        }
    }

    private var wasEditor = false
    private var imageFile: File? = null
    private var imageUri: Uri? = null
    private lateinit var codes: Codes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        tmp = File(externalCacheDir!!, "tmp")
        if (!tmp.exists()) tmp.mkdir()

        if (File(filesDir, "data").exists()) firstTime = false
        else File(filesDir, "data").createNewFile()

        if (Build.VERSION.SDK_INT >= 23)
            if (checkSelfPermission(CAMERA) == PERMISSION_DENIED) cameraPermission.launch(CAMERA)

        preview = findViewById(R.id.preview)
        capture = findViewById(R.id.capture)

        capture.setOnClickListener { capture() }

        codes = Codes.load(assets.open("recycleCodes.json"))
    }

    private fun capture() {
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File.createTempFile("imageText", ".jpg", tmp)
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
        camera.putExtra(EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(camera)
    }

    private val imageEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        imageUri = it.data?.data ?: imageUri
        if (imageUri == it.data?.data) {
            imageFile = imageUri!!.toFile()
        }
        mainProcess()
    }

    private fun preProcess() {
        AlertDialog.Builder(this, R.style.Dialog)
            .setTitle("Обработка")
            .setMessage("Вы хотите обработать это изображение? (Это может повысить качество распознавания)")
            .setPositiveButton("Да") { dialog, _ ->
                Intent(ACTION_EDIT).run {
                    setDataAndType(imageUri, "image/*")
                    putExtra(EXTRA_OUTPUT, imageUri)
                    addFlags(FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                    try {
//                        startActivity(this)
                        imageEditor.launch(this)
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
            .create().show()
    }

    override fun onResume() {
        super.onResume()
        if (wasEditor) {
            mainProcess()
            wasEditor = false
        }
    }

    private fun mainProcess() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        capture.layoutParams = capture.layoutParams.also { (it as ConstraintLayout.LayoutParams).verticalBias = 0.9f }

        preview.setImageBitmap(BitmapFactory.decodeFile(imageFile?.absolutePath))
        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(this, imageUri!!)
        }
        catch (e: Exception) {
            Log.e(tag, "Exception in method mainProcces", e)
        }
        if (image == null) return

        recognizer.process(image)
            .addOnSuccessListener exitLambda@{ text ->
                var typesMap: Map<String, *>? =
                    mapOf(
                        *(
                                codes.codes.run {
                                    val pairs = arrayListOf<Pair<String, Codes.Code>>()
                                    forEach {
                                        it.codes.forEach { code ->
                                            pairs.add(code to it)
                                        }
                                    }
                                    pairs.toArray()
                                    val pairsArray: Array<Pair<String, Codes.Code>> = Array(pairs.size) { "" to Codes.Code(listOf(), "", "", 0F) }
                                    pairs.forEachIndexed { index, pair ->
                                        pairsArray[index] = pair
                                    }
                                    pairsArray
                                }
                                ),

                        *(
                                codes.categories.run {
                                    val pairs = arrayListOf<Pair<String, Codes.Category>>()
                                    forEach {
                                        it.codes.forEach { code ->
                                            pairs.add(code to it)
                                        }
                                    }
                                    val pairsArray: Array<Pair<String, Codes.Category>> = Array(pairs.size) { "" to Codes.Category("", listOf()) }
                                    pairs.forEachIndexed { index, pair ->
                                        pairsArray[index] = pair
                                    }
                                    pairsArray
                                }
                                )
                    )

                Log.i(tag, "Recognized text ${text.text}")
                val types = arrayListOf<String>()
                var materialCount: Short = 0
                var materialAfterRecycle: Short = 0
                if (typesMap != null) {
                    @Suppress("NAME_SHADOWING")
                    val typesMap: Map<String, *> = typesMap
                    text.textBlocks.forEach {
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
                            if (c in chars) containsChars = true
                            if (c == '/') containsSlash = true
                        }
                        if (containsDigits && containsSlash && !containsChars) {
                            val values = it.text.split("/")
                            materialCount = values[1].toShort()
                            materialAfterRecycle = values[0].toShort()
                        }
                        if ((containsChars || containsDigits) && !containsSlash)
                            if (typesMap.containsKey(it.text))
                                types += it.text
                    }
                }

                @Suppress("UNUSED_VALUE")
                typesMap = null
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
                Log.e(tag, "Cannot recognize text", it)
            }
    }

    private fun getString(res: String): String {
        return getString(R.string::class.java.getField(res).getInt(null))
    }
}