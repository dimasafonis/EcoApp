package dimasafonis.ecoapp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.internal.ContextUtils.getActivity

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("dimasafonis.ecoapp", appContext.packageName)
    }
    @Test
    fun testCodes() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val activity = getActivity(appContext) as CameraActivity
        
    }
}