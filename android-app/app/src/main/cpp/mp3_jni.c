/* Small bridge so Kotlin can talk to the LAME MP3 encoder. */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <lame.h>

JNIEXPORT jlong JNICALL
Java_live_honeynut_ringtone_Mp3Encoder_nativeInit(JNIEnv *env, jclass cls,
        jint sampleRate, jint channels, jint kbps) {
    lame_t l = lame_init();
    if (!l) return 0;
    lame_set_in_samplerate(l, sampleRate);
    lame_set_num_channels(l, channels);
    lame_set_mode(l, channels == 1 ? MONO : JOINT_STEREO);
    lame_set_brate(l, kbps);
    lame_set_quality(l, 2);
    lame_set_bWriteVbrTag(l, 0);   /* we stream the output, so no seek-back header */
    if (lame_init_params(l) < 0) { lame_close(l); return 0; }
    return (jlong)(intptr_t)l;
}

/* pcm: 16-bit samples, interleaved when stereo. frames = samples per channel. */
JNIEXPORT jint JNICALL
Java_live_honeynut_ringtone_Mp3Encoder_nativeEncode(JNIEnv *env, jclass cls,
        jlong handle, jshortArray pcm, jint frames, jint channels, jbyteArray out) {
    lame_t l = (lame_t)(intptr_t)handle;
    jshort *in = (*env)->GetShortArrayElements(env, pcm, NULL);
    jbyte *buf = (*env)->GetByteArrayElements(env, out, NULL);
    jsize outLen = (*env)->GetArrayLength(env, out);
    int n = channels == 1
        ? lame_encode_buffer(l, in, in, frames, (unsigned char *)buf, outLen)
        : lame_encode_buffer_interleaved(l, in, frames, (unsigned char *)buf, outLen);
    (*env)->ReleaseShortArrayElements(env, pcm, in, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, out, buf, 0);
    return n;
}

JNIEXPORT jint JNICALL
Java_live_honeynut_ringtone_Mp3Encoder_nativeFlush(JNIEnv *env, jclass cls,
        jlong handle, jbyteArray out) {
    lame_t l = (lame_t)(intptr_t)handle;
    jbyte *buf = (*env)->GetByteArrayElements(env, out, NULL);
    jsize outLen = (*env)->GetArrayLength(env, out);
    int n = lame_encode_flush(l, (unsigned char *)buf, outLen);
    (*env)->ReleaseByteArrayElements(env, out, buf, 0);
    return n;
}

JNIEXPORT void JNICALL
Java_live_honeynut_ringtone_Mp3Encoder_nativeClose(JNIEnv *env, jclass cls, jlong handle) {
    lame_close((lame_t)(intptr_t)handle);
}
