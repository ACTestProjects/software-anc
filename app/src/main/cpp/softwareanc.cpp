/*
 * this lib is for school project. do not use it is something else,
 * because you are neither licensed nor allowed to do it.
 *
 * author: someone
 *
 * */

#include "softwareanc.h"

oboe::AudioStream* outputStream;
oboe::AudioStream* inputStream;
bool isInverting = false;

// cally backy thingy
class OutputCallback : public oboe::AudioStreamCallback {
    oboe::DataCallbackResult
    onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) override {
        int16_t* ptr = (int16_t*)audioData;
        inputStream->read(audioData, numFrames, oboe::kNanosPerMillisecond);
        if (isInverting) {
            for (int i = 0; i < numFrames; i++) {
                ptr[i] = -ptr[i];
            }
        }
        return oboe::DataCallbackResult::Continue;
    }
};

// checky thingy
extern "C"
JNIEXPORT jint JNICALL
Java_com_ac_softwareanc_MainActivity_checkNativeLibrary(JNIEnv *env, jobject thiz) {
    return 8328; // this is a magic number
}

// updatey thingy
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_ac_softwareanc_MainActivity_updateRecorderNew(JNIEnv *env, jobject thiz, jint sample_rate,
                                                       jint buffer_size, jboolean record,
                                                       jboolean invert) {
    oboe::AudioStreamBuilder builder = oboe::AudioStreamBuilder();
    isInverting = invert;

    if (outputStream != nullptr) {
        outputStream->close();
        delete outputStream;
    }
    if (inputStream != nullptr) {
        inputStream->close();
        delete inputStream;
    }

    if (record) {
        builder.setDirection(oboe::Direction::Output);
        builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        builder.setCallback(new OutputCallback());
        if (builder.openStream(&outputStream) != oboe::Result::OK) {
            LOGE("error while creating output stream!!");
            return false;
        }
        outputStream->setBufferSizeInFrames(outputStream->getFramesPerBurst() * 2);

        // now an input stream
        delete &builder;
        builder = oboe::AudioStreamBuilder();

        builder.setDirection(oboe::Direction::Input);
        builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        if (builder.openStream(&inputStream) != oboe::Result::OK) {
            LOGE("error while creating input stream!!");
            return false;
        }

        if (inputStream->requestStart() != oboe::Result::OK){
            LOGE("error while starting input stream!!");
            return false;
        }
        if (outputStream->requestStart() != oboe::Result::OK){
            LOGE("error while starting output stream!!");
            return false;
        }
        return true;
    }
    else {
        return true;
    }
}

// stopy thingy
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_ac_softwareanc_MainActivity_stopRecorderNew(JNIEnv *env, jobject thiz) {
    if (outputStream != nullptr) outputStream->close();
    if (inputStream != nullptr) inputStream->close();
    return true;
}