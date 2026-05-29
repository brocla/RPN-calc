package com.brocla.rpn_calc.voice

import android.media.AudioRecord
import org.vosk.android.SpeechService

/**
 * Returns the audio session ID of the [AudioRecord] held by [SpeechService].
 *
 * Obtained via reflection because [SpeechService.getAudioSessionId] was added in
 * alphacep/vosk-api#2042 and is not yet in a published release.
 * TODO: replace with speechService.audioSessionId once vosk-android > 0.3.75 is released.
 */
fun SpeechService.audioSessionId(): Int {
    val field = SpeechService::class.java.getDeclaredField("recorder")
    field.isAccessible = true
    return (field.get(this) as AudioRecord).audioSessionId
}
