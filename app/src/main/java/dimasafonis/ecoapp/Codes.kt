package dimasafonis.ecoapp

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.InputStream

class Codes {
    var codes: List<Code> = arrayListOf()
    var categories: List<Category> = arrayListOf()

    class Category(val res: String, val name: String)
    class Code(val codes: Array<String>, val res: String, val cat: String,
               val recycle: Float)

    companion object {
        @JvmStatic
        fun load(input: InputStream): Codes {
            return Yaml(Constructor(Codes::class.java)).load(input) as Codes
        }
    }
}