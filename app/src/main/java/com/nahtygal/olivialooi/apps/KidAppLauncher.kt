package com.nahtygal.olivialooi.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

sealed interface KidAppLaunchResult {
    data object Launched : KidAppLaunchResult

    data class Unavailable(val app: KidApp) : KidAppLaunchResult
}

class KidAppLauncher(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    fun launch(app: KidApp): KidAppLaunchResult {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                ?: return KidAppLaunchResult.Unavailable(app)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launchIntent)
            KidAppLaunchResult.Launched
        } catch (_: ActivityNotFoundException) {
            KidAppLaunchResult.Unavailable(app)
        } catch (_: SecurityException) {
            KidAppLaunchResult.Unavailable(app)
        }
    }
}
