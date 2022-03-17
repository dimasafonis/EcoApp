package dimasafonis.ecoapp

import com.google.gson.GsonBuilder
import java.io.InputStream

class Codes {
    var codes: ArrayList<Code> = arrayListOf()
    var categories: ArrayList<Category> = arrayListOf()

    data class Category(val name: String, val codes: List<String>, val readableName: String)
    data class Code(val codes: List<String>, val name: String, val cat: String,
               val recycle: Float)

    companion object {
        @JvmStatic
        fun load(input: InputStream): Codes {
            val gson = GsonBuilder().create()
            return gson.fromJson(input.reader(), Codes::class.java)
        }
    }
}
