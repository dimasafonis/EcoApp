package dimasafonis.ecoapp

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.Intent.*
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.provider.MediaStore.EXTRA_OUTPUT
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class CameraActivity : AppCompatActivity() {

    private lateinit var tmp: File
    companion object {
        @JvmStatic
        private val TAG = "DimasafonisCodes"
        @JvmStatic
        private val DIGITS = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        @JvmStatic
        private val CHARS = arrayOf(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'g', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        )
    }
    lateinit var preview: ImageView
    lateinit var capture: Button
    lateinit var pick: Button
    lateinit var bottomSheetBeh: BottomSheetBehavior<ConstraintLayout>
    lateinit var materialCountText: TextView
    lateinit var recycleCountText: TextView
    lateinit var materialType: TextView
    lateinit var materialSubtype: TextView
    lateinit var recommendations: TextView
    private lateinit var emptyText: TextView

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) mainProcess()
    }
    private val pickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        imageUri = it.data?.data
        mainProcess()
    }
    private val cameraPermissionLambda: (Boolean) -> Unit = {
        if (!it) {
            AlertDialog.Builder(this)
                .setTitle("Камера")
                .setMessage("Разрешите использовать камеру, без неё эта функция не будет работать")
                .setPositiveButton("Ок") { di, _ ->
                    di.cancel()
                    cameraPermission.launch(CAMERA)
                }
                .setNegativeButton("Нет") { di, _ ->
                    di.cancel()
                }
        }
        if (it) {
            capture()
        }
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission(), cameraPermissionLambda)

    private var imageFile: File? = null
    private var imageUri: Uri? = null
    private lateinit var codes: Codes
    private lateinit var typesMap: Map<String, *>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (savedInstanceState != null)
            onRestoreInstanceState(savedInstanceState)
        tmp = File(externalCacheDir!!, "tmp")
        if (!tmp.exists()) tmp.mkdir()

        preview = findViewById(R.id.preview)
        capture = findViewById(R.id.capture)
        pick = findViewById(R.id.pick)

        capture.setOnClickListener { capture() }
        pick.setOnClickListener { pickFromGallery() }

        codes = Codes.load(assets.open("recycleCodes.json"))

        typesMap = mapOf(
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
                        val pairsArray: Array<Pair<String, Codes.Category>> = Array(pairs.size) { "" to Codes.Category("", listOf(), "") }
                        pairs.forEachIndexed { index, pair ->
                            pairsArray[index] = pair
                        }
                        pairsArray
                    }
                    )
        )
        bottomSheetBeh = BottomSheetBehavior.from(findViewById(R.id.bottomSheet))

        bottomSheetBeh.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        findViewById<AppCompatImageView>(R.id.arrow).setOnClickListener {
            bottomSheetBeh.state = if (bottomSheetBeh.state == STATE_COLLAPSED) STATE_EXPANDED else STATE_COLLAPSED
        }
        materialCountText = findViewById<TextView>(R.id.material_count).also { it.visibility = GONE }
        recycleCountText = findViewById<TextView>(R.id.recycle_count).also { it.visibility = GONE }
        materialType = findViewById<TextView>(R.id.material_type).also { it.visibility = GONE }
        materialSubtype = findViewById<TextView>(R.id.material_subtype).also { it.visibility = GONE }
        recommendations = findViewById<TextView>(R.id.recommendation).also { it.visibility = GONE }
        emptyText = findViewById(R.id.empty_text)
    }
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putParcelable("imageUri", imageUri)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        imageUri = savedInstanceState.getParcelable("imageUri")
        preview.setImageURI(imageUri)
    }

    override fun onResume() {
        super.onResume()
        tmp.listFiles()?.forEach {
            if (it.name != imageFile?.name)
                it.delete()
        }
    }

    private fun pickFromGallery() {
        val picker = Intent(ACTION_PICK)
        picker.type = "image/*"
        picker.flags = FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION
        pickLauncher.launch(picker)
    }
    
    private fun capture() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(CAMERA) == PERMISSION_DENIED) {
                cameraPermission.launch(CAMERA)
                return
            }
        }
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageFile = File.createTempFile("imageText", ".jpg", tmp)
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
        camera.putExtra(EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(camera)
    }

    private fun mainProcess() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        capture.layoutParams = capture.layoutParams.also { (it as ConstraintLayout.LayoutParams).verticalBias = 0.2f }
        pick.layoutParams = pick.layoutParams.also { (it as ConstraintLayout.LayoutParams).verticalBias = 0.1f }

        preview.setImageURI(imageUri)
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
                Log.i(TAG, "Recognized text ${text.text}")
                val types = arrayListOf<String>()
                var materialCount: Short = 0
                var materialAfterRecycle: Short = 0
                text.textBlocks.forEach {
                    it.lines.forEach { line ->
                        var containsDigits = false
                        var containsChars = false
                        var containsSlash = false
                        line.text.forEach { c ->
                            if (c in DIGITS) containsDigits = true
                            if (c.lowercaseChar() in CHARS) containsChars = true
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
                        AlertDialog.Builder(this, R.style.Dialog)
                            .setTitle("Ошибка")
                            .setMessage("При распозновании произошла ошибка, попробуйте ещё раз")
                            .create().show()
                        return@exitLambda
                    }
                }

                materialCountText.text = getString(
                    R.string.material_count,
                    if (materialCount == 0.toShort()) "Неизвестно" else materialCount.toString()
                )
                recycleCountText.text = getString(
                    R.string.material_after_recycle,
                    if (materialAfterRecycle == 0.toShort()) "Неизвестно" else materialAfterRecycle.toString()
                )

                materialType.text = getString(
                    R.string.material_type,
                    if (category?.name == null)
                        if (code?.cat == null) "Неизвестно" else codes.categories.first { code.cat == it.name }.readableName
                    else category.readableName
                )
                materialSubtype.text = getString(
                    R.string.material_subtype,
                    code?.name ?: "Неизвестно"
                )
                recommendations.text = when (code?.recycle) {
                    0f -> "Не перерабатывается"
                    0.3f -> "Эту упаковку лучше не использовать. она содержит вредные материалы, а также мало где принимают этот материал"
                    0.4f -> "Эта упаковка содержит вредные материалы"
                    0.5f -> "Материал можно сдать, но далеко не везде"
                    1f -> "Эта упаковка безопасна"
                    null -> "Неизвестно"
                    else -> throw RuntimeException("Recycle value not in 0..1")
                }
                emptyText.visibility = GONE
                materialCountText.visibility = VISIBLE
                recycleCountText.visibility = VISIBLE
                materialType.visibility = VISIBLE
                materialSubtype.visibility = VISIBLE
                recommendations.visibility = VISIBLE
                bottomSheetBeh.state = STATE_EXPANDED
            }
            .addOnFailureListener {
                Log.e(TAG, "Cannot recognize text", it)
            }
    }

}
