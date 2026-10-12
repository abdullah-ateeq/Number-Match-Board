package com.example.numbermatchboard.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.example.numbermatchboard.ui.FeedbackKind

/**
 * Plays short synthesized tones and haptics. Uses the platform ToneGenerator so no audio
 * assets are required; any failure to obtain audio is ignored and never blocks the game.
 */
class GameFeedback(private val view: View) {

    private var toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
    }.getOrNull()

    fun play(kind: FeedbackKind, sound: Boolean, haptics: Boolean) {
        if (sound) playTone(kind)
        if (haptics) performHaptic(kind)
    }

    private fun playTone(kind: FeedbackKind) {
        val (tone, duration) = when (kind) {
            FeedbackKind.SELECT -> ToneGenerator.TONE_PROP_BEEP to 35
            FeedbackKind.IDENTICAL_MATCH -> ToneGenerator.TONE_PROP_ACK to 140
            FeedbackKind.SUM_TO_TEN_MATCH -> ToneGenerator.TONE_PROP_BEEP2 to 140
            FeedbackKind.MISMATCH -> ToneGenerator.TONE_PROP_NACK to 160
            FeedbackKind.CPU_ACTION -> ToneGenerator.TONE_CDMA_PIP to 50
            FeedbackKind.VICTORY -> ToneGenerator.TONE_CDMA_CONFIRM to 400
            FeedbackKind.DEFEAT -> ToneGenerator.TONE_SUP_ERROR to 300
            FeedbackKind.DRAW -> ToneGenerator.TONE_PROP_PROMPT to 200
        }
        runCatching { toneGenerator?.startTone(tone, duration) }
    }

    private fun performHaptic(kind: FeedbackKind) {
        val light = HapticFeedbackConstants.CLOCK_TICK
        val medium = HapticFeedbackConstants.CONTEXT_CLICK
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        val warning = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
        val constant = when (kind) {
            FeedbackKind.SELECT, FeedbackKind.MISMATCH -> light
            FeedbackKind.IDENTICAL_MATCH, FeedbackKind.SUM_TO_TEN_MATCH -> medium
            FeedbackKind.VICTORY -> success
            FeedbackKind.DEFEAT -> warning
            FeedbackKind.DRAW -> medium
            FeedbackKind.CPU_ACTION -> return
        }
        view.performHapticFeedback(constant)
    }

    fun release() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }

    private companion object {
        const val TONE_VOLUME = 45
    }
}

@Composable
fun rememberGameFeedback(): GameFeedback {
    val view = LocalView.current
    val feedback = remember(view) { GameFeedback(view) }
    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }
    return feedback
}
