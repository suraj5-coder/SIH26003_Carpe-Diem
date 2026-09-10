package org.mindmate.app.sos

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SosManager(private val context: Context) {
    fun localAlarm() {
        runCatching {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.play()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(1_500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun openDialer(phone: String): Boolean {
        if (!phone.any(Char::isDigit)) return false
        return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        }.isSuccess
    }

    fun openSms(phone: String, patientName: String): Boolean {
        if (!phone.any(Char::isDigit)) return false
        return runCatching {
        context.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}")).apply {
                putExtra("sms_body", "SOS from $patientName. Please contact me.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        }.isSuccess
    }
}
