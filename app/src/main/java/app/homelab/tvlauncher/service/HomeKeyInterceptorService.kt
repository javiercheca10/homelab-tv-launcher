package app.homelab.tvlauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import app.homelab.tvlauncher.MainActivity

class HomeKeyInterceptorService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.google.android.tvlauncher" ||
            pkg == "com.google.android.apps.tv.launcherx" ||
            (pkg.contains("tvlauncher") && pkg != packageName) ||
            pkg == "com.sony.dtv.home" ||
            pkg == "com.sony.dtv.launcher"
        ) {
            launchHome()
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                launchHome()
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    private fun launchHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(intent)
    }
}
