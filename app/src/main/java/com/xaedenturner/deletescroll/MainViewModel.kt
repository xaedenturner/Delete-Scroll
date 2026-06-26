package com.xaedenturner.deletescroll

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val isServiceEnabled: Boolean = false,
        val hasOverlayPermission: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    // Called from onResume() so state always reflects reality after
    // the user returns from the system accessibility or overlay settings screens.
    fun refreshStatus() {
        val context = getApplication<Application>()
        _uiState.value = UiState(
            isServiceEnabled = isAccessibilityServiceEnabled(context),
            hasOverlayPermission = Settings.canDrawOverlays(context)
        )
    }

    // Uses AccessibilityManager rather than string-parsing Settings.Secure
    // directly, which is more robust and avoids manual ComponentName formatting.
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                        it.resolveInfo.serviceInfo.name.endsWith("ReelsBlockerService")
            }
    }
}