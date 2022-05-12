package dimasafonis.ecoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatDelegate.*

class SettingsActivity() : AppCompatActivity() {

    companion object {
        @JvmStatic
        val idMap: MutableMap<Int, String> = mutableMapOf()
        @JvmStatic
        val themeMap : MutableMap<String, Int> = mutableMapOf()
        init {
            idMap[R.id.systemTheme] = "system"
            idMap[R.id.darkTheme] = "dark"
            idMap[R.id.lightTheme] = "light"

            themeMap["system"] = R.id.systemTheme
            themeMap["dark"] = R.id.darkTheme
            themeMap["light"] = R.id.lightTheme
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        try {
            findViewById<RadioButton>(themeMap[CameraActivity.themeFile.readText()]!!).isChecked = true
        }
        catch (e: NullPointerException) {
            findViewById<RadioButton>(R.id.systemTheme).isChecked = true
            CameraActivity.themeFile.writeText("system")
        }
        val changed = { v: View ->
            val theme = idMap[v.id]
            setDefaultNightMode(when (theme) {
                "system" -> MODE_NIGHT_FOLLOW_SYSTEM
                "dark" -> MODE_NIGHT_YES
                "light" -> MODE_NIGHT_NO
                else -> throw IllegalArgumentException("It's not possible!")
            })
            CameraActivity.themeFile.writeText(theme)
        }
        findViewById<RadioButton>(R.id.systemTheme).setOnClickListener(changed)
        findViewById<RadioButton>(R.id.darkTheme).setOnClickListener(changed)
        findViewById<RadioButton>(R.id.lightTheme).setOnClickListener(changed)
    }
}