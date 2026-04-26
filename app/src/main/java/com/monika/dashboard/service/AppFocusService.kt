package me.elvish.statusreporter.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import me.elvish.statusreporter.repository.AppStatusRepository

class AppFocusService : AccessibilityService() {

    private val appLabelCache = mutableMapOf<String, String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (shouldIgnorePackage(packageName)) {
            return
        }

        val appLabel = getAppLabel(packageName)

        AppStatusRepository.currentForegroundApp = appLabel
        Log.d(TAG, "Foreground App updated: $appLabel ($packageName)")
    }

    override fun onInterrupt() {
        // Required, but no action needed.
    }

    private fun shouldIgnorePackage(packageName: String): Boolean {
        return packageName == SYSTEM_UI_PACKAGE || packageName == applicationContext.packageName
    }

    private fun getAppLabel(packageName: String): String {
        appLabelCache[packageName]?.let { return it }

        val label = loadAppLabel(packageName)
        if (label != packageName) {
            appLabelCache[packageName] = label
        }
        return label
    }

    private fun loadAppLabel(packageName: String): String {
        resolveApplicationLabel(packageName)?.let { return it }
        resolveLauncherLabel(packageName)?.let { return it }
        return packageName
    }

    private fun resolveApplicationLabel(packageName: String): String? {
        return runCatching {
            packageManager.getApplicationInfo(packageName, 0)
                .loadLabel(packageManager)
                .toString()
                .takeIf { it.isNotBlank() }
        }.onFailure { error ->
            Log.w(TAG, "Unable to resolve application label for $packageName", error)
        }.getOrNull()
    }

    private fun resolveLauncherLabel(packageName: String): String? {
        return runCatching {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }

            packageManager.queryIntentActivities(launcherIntent, 0)
                .firstOrNull()
                ?.loadLabel(packageManager)
                ?.toString()
                ?.takeIf { it.isNotBlank() }
        }.onFailure { error ->
            Log.w(TAG, "Unable to resolve launcher label for $packageName", error)
        }.getOrNull()
    }

    companion object {
        private const val TAG = "AppFocusService"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
