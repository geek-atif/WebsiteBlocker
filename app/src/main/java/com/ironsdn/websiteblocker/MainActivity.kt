package com.ironsdn.websiteblocker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    var TAG = "com.ironsdn.websiteblocker.MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(isAccessibilitySettingsOn()) {

        }else{
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
        //performRedirect("https://www.ironsdn.com/", "com.android.chrome")

    }


    private fun isAccessibilitySettingsOn(): Boolean {
        var accessibilityEnabled = 0
        val service = packageName + "/" + UrlInterceptorService::class.java.getCanonicalName()
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } else {
        }
        return false
    }

    private fun performRedirect(redirectUrl: String, browserPackage: String) {
        try {
            Log.d(TAG, "redirectUrl $redirectUrl browserPackage $browserPackage")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
            intent.setPackage(browserPackage)
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackage)
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // the expected browser is not installed
            e.printStackTrace()
            Log.e(TAG, e.message)
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
            startActivity(i)
        }
    }

}