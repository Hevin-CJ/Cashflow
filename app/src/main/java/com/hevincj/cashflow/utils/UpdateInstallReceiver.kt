package com.hevincj.cashflow.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast

class UpdateInstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UpdateInstallReceiver"
        const val ACTION_INSTALL_COMPLETE = "com.hevincj.cashflow.ACTION_INSTALL_COMPLETE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_COMPLETE) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Log.d(TAG, "Install pending user action. Prompting user...")
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }

                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.d(TAG, "Package installation succeeded!")
                Toast.makeText(context, "Update installed successfully!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.e(TAG, "Package installation failed with status $status: $message")
            }
        }
    }
}
