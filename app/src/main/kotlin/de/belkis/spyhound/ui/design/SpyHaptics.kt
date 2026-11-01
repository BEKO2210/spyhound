package de.belkis.spyhound.ui.design

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View

class SpyHaptics(private val context: Context) {
    private fun enabled() = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    fun tick(view: View) {
        if (enabled()) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun success(view: View) {
        if (enabled()) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun alert(view: View) {
        if (!enabled()) return
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 29) vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        else view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
