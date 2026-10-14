package live.honeynut.ringtone

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Converts small test videos (made by ffmpeg in the GitHub build, see
 * .github/workflows/android-app.yml) and checks the MP3s that come out.
 */
@RunWith(AndroidJUnit4::class)
class AudioConverterTest {

    private val testCtx = InstrumentationRegistry.getInstrumentation().context
    private val appCtx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun asset(name: String): Uri {
        val f = File(appCtx.cacheDir, name)
        testCtx.assets.open(name).use { i -> f.outputStream().use { i.copyTo(it) } }
        return Uri.fromFile(f)
    }

    private fun convert(name: String): ByteArray {
        val out = ByteArrayOutputStream()
        var last = -1
        AudioConverter.toMp3(appCtx, asset(name), out) { last = it }
        assertEquals("progress should finish at 100", 100, last)
        return out.toByteArray()
    }

    /** Rough MP3 length: 192 kbps = 24,000 bytes per second. */
    private fun assertLooksLikeMp3(bytes: ByteArray, seconds: Double) {
        assertTrue("starts with an MP3 frame", bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0)
        val expected = seconds * 24_000
        assertTrue("size ${bytes.size} should be about $expected", bytes.size > expected * 0.9 && bytes.size < expected * 1.1)
    }

    @Test fun stereoAacVideo() = assertLooksLikeMp3(convert("stereo_44k.mp4"), 5.0)

    @Test fun mono48kVideo() = assertLooksLikeMp3(convert("mono_48k.mp4"), 4.0)

    @Test fun surroundVideo() = assertLooksLikeMp3(convert("surround_51.mp4"), 3.0)

    @Test fun silentVideoGivesFriendlyError() {
        try {
            convert("no_audio.mp4")
            fail("expected NoAudioException")
        } catch (e: NoAudioException) {
            // good
        }
    }
}
