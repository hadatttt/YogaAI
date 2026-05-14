package hoang.dqm.codebase.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import hoang.dqm.codebase.base.application.getBaseApplication

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)



fun FragmentActivity.openSettingNetWork() {
    try {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

