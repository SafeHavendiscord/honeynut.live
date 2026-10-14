package live.honeynut.ringtone

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import java.io.BufferedOutputStream

class MainActivity : Activity() {

    private lateinit var pickButton: Button
    private lateinit var playButton: Button
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    private var savedMp3: Uri? = null
    private var player: MediaPlayer? = null
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pickButton = findViewById(R.id.pick)
        playButton = findViewById(R.id.play)
        status = findViewById(R.id.status)
        progress = findViewById(R.id.progress)

        // Keep content clear of the status bar / gesture bar on newer Android.
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            }
            insets
        }

        pickButton.setOnClickListener { pickVideo() }
        playButton.setOnClickListener { togglePlay() }

        handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** Opened from the Share menu with a video. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        @Suppress("DEPRECATION")
        val uri = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent.getParcelableExtra(Intent.EXTRA_STREAM)
        uri?.let { convert(it) }
    }

    private fun pickVideo() {
        val photoPicker = Build.VERSION.SDK_INT >= 33 ||
            (Build.VERSION.SDK_INT >= 30 && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2)
        val intent = if (photoPicker) {
            Intent(MediaStore.ACTION_PICK_IMAGES).setType("video/*")
        } else {
            Intent(Intent.ACTION_GET_CONTENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, PICK_VIDEO)
    }

    @Deprecated("Plain Activity API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO && resultCode == RESULT_OK) data?.data?.let { convert(it) }
    }

    private fun convert(video: Uri) {
        if (busy) return
        busy = true
        stopPlaying()
        savedMp3 = null
        pickButton.isEnabled = false
        playButton.visibility = View.GONE
        progress.visibility = View.VISIBLE
        progress.progress = 0
        status.text = getString(R.string.converting)

        val name = baseName(video)
        Thread {
            var saved: Uri? = null
            val error = try {
                saved = saveMp3(video, name)
                null
            } catch (e: NoAudioException) {
                e.message
            } catch (e: Exception) {
                getString(R.string.failed, e.message ?: e.javaClass.simpleName)
            }
            runOnUiThread {
                busy = false
                pickButton.isEnabled = true
                progress.visibility = View.GONE
                if (saved != null) {
                    savedMp3 = saved
                    status.text = getString(R.string.saved, "$name.mp3", SAVE_FOLDER)
                    playButton.text = getString(R.string.play)
                    playButton.visibility = View.VISIBLE
                } else {
                    status.text = error
                }
            }
        }.start()
    }

    /** Writes the MP3 into Music/HoneyNUT Ringtones and returns its address. */
    private fun saveMp3(video: Uri, name: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.mp3")
            put(MediaStore.Audio.Media.TITLE, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, SAVE_FOLDER)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = contentResolver.insert(collection, values) ?: error("couldn't create the file")
        try {
            contentResolver.openOutputStream(uri)!!.let { BufferedOutputStream(it) }.use { out ->
                AudioConverter.toMp3(this, video, out) { pct -> runOnUiThread { progress.progress = pct } }
            }
            contentResolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)   // don't leave a broken file behind
            throw e
        }
    }

    /** "My Video.mp4" -> "My Video" */
    private fun baseName(uri: Uri): String {
        val display = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        return display?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "HoneyNUT clip"
    }

    private fun togglePlay() {
        if (player != null) { stopPlaying(); return }
        val uri = savedMp3 ?: return
        player = MediaPlayer.create(this, uri)?.apply {
            setOnCompletionListener { stopPlaying() }
            start()
        }
        if (player != null) playButton.text = getString(R.string.stop)
    }

    private fun stopPlaying() {
        player?.release()
        player = null
        playButton.text = getString(R.string.play)
    }

    override fun onDestroy() {
        stopPlaying()
        super.onDestroy()
    }

    companion object {
        private const val PICK_VIDEO = 1
        val SAVE_FOLDER = Environment.DIRECTORY_MUSIC + "/HoneyNUT Ringtones"
    }
}
