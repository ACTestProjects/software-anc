/*
 * this lib is for school project. do not use it is something else,
 * because you are neither licensed nor allowed to do it.
 *
 * author: someone
 *
 * */

#ifndef SOFTWARE_ANC_SOFTWAREANC_H
#define SOFTWARE_ANC_SOFTWAREANC_H

#include <jni.h>
#include <memory>
#include <oboe/Oboe.h>
#include <../../oboe/src/common/OboeDebug.h>

extern oboe::AudioStream* outputStream;
extern oboe::AudioStream* inputStream;
extern bool isInverting;

#endif //SOFTWARE_ANC_SOFTWAREANC_H
