package live.honeynut.ringtone

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.OutputStream
import java.nio.ByteOrder

class NoAudioException : Exception("This video has no sound to convert.")

/**
 * Pulls the soundtrack out of a video, decodes it with the phone's own decoder,
 * and re-encodes it as MP3.
 */
object AudioConverter {

    /** Converts [video] into MP3 bytes written to [out]. [onProgress] gets 0..100. */
    fun toMp3(context: Context, video: Uri, out: OutputStream, onProgress: (Int) -> Unit = {}) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: Mp3Encoder? = null
        try {
            extractor.setDataSource(context, video, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw NoAudioException()
            extractor.selectTrack(track)
            val inFormat = extractor.getTrackFormat(track)
            val durationUs = if (inFormat.containsKey(MediaFormat.KEY_DURATION))
                inFormat.getLong(MediaFormat.KEY_DURATION) else 0L

            val codec = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME)!!)
            decoder = codec
            codec.configure(inFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var lastPct = -1
            while (true) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, 10_000)
                if (outIndex < 0) continue   // try again / format changed (we read it below)

                if (info.size > 0) {
                    val fmt = codec.outputFormat
                    val srcChannels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val outChannels = minOf(srcChannels, 2)
                    val enc = encoder ?: Mp3Encoder(fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE), outChannels)
                        .also { encoder = it }
                    val isFloat = fmt.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        fmt.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT

                    val buf = codec.getOutputBuffer(outIndex)!!.order(ByteOrder.nativeOrder())
                    buf.position(info.offset).limit(info.offset + info.size)
                    val pcm = toStereoShorts(buf, isFloat, srcChannels, outChannels)
                    out.write(enc.encode(pcm, pcm.size / outChannels))

                    if (durationUs > 0) {
                        val pct = (info.presentationTimeUs * 100 / durationUs).toInt().coerceIn(0, 99)
                        if (pct != lastPct) { lastPct = pct; onProgress(pct) }
                    }
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
            encoder?.let { out.write(it.flush()) } ?: throw NoAudioException()
            onProgress(100)
        } finally {
            encoder?.close()
            try { decoder?.stop() } catch (_: Exception) {}
            decoder?.release()
            extractor.release()
        }
    }

    /** Turns decoded PCM (16-bit or float, any channel count) into 16-bit mono/stereo. */
    private fun toStereoShorts(buf: java.nio.ByteBuffer, isFloat: Boolean, srcCh: Int, outCh: Int): ShortArray {
        val bytesPerSample = if (isFloat) 4 else 2
        val frames = buf.remaining() / (bytesPerSample * srcCh)
        val pcm = ShortArray(frames * outCh)
        if (!isFloat && srcCh == outCh) {
            buf.asShortBuffer().get(pcm)
            return pcm
        }
        var i = 0
        for (f in 0 until frames) {
            for (c in 0 until srcCh) {
                val s: Short = if (isFloat) {
                    (buf.getFloat().coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                } else buf.getShort()
                if (c < outCh) pcm[i++] = s   // surround sound: keep front left + right
            }
        }
        return pcm
    }
}
