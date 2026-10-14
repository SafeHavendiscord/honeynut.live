package live.honeynut.ringtone

/** Thin Kotlin wrapper around the LAME MP3 encoder (native code in src/main/cpp). */
class Mp3Encoder(sampleRate: Int, private val channels: Int, kbps: Int = 192) : AutoCloseable {
    private var handle = nativeInit(sampleRate, channels, kbps)
    private var out = ByteArray(16 * 1024)

    init {
        require(handle != 0L) { "MP3 encoder can't handle $sampleRate Hz / $channels channels" }
    }

    /** [pcm] holds [frames] 16-bit samples per channel (interleaved if stereo). Returns MP3 bytes. */
    fun encode(pcm: ShortArray, frames: Int): ByteArray {
        val needed = (frames * 5) / 4 + 7200   // LAME's worst-case size rule
        if (out.size < needed) out = ByteArray(needed)
        val n = nativeEncode(handle, pcm, frames, channels, out)
        check(n >= 0) { "MP3 encoding failed ($n)" }
        return out.copyOf(n)
    }

    fun flush(): ByteArray {
        val n = nativeFlush(handle, out)
        return if (n > 0) out.copyOf(n) else ByteArray(0)
    }

    override fun close() {
        if (handle != 0L) nativeClose(handle)
        handle = 0L
    }

    private companion object {
        init { System.loadLibrary("mp3") }
        @JvmStatic external fun nativeInit(sampleRate: Int, channels: Int, kbps: Int): Long
        @JvmStatic external fun nativeEncode(h: Long, pcm: ShortArray, frames: Int, channels: Int, out: ByteArray): Int
        @JvmStatic external fun nativeFlush(h: Long, out: ByteArray): Int
        @JvmStatic external fun nativeClose(h: Long)
    }
}
