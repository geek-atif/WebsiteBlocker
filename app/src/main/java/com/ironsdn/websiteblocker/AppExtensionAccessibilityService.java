package com.ironsdn.websiteblocker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressLint("LongLogTag")
public class AppExtensionAccessibilityService extends AccessibilityService {
    private static final boolean DEBUG = false;
    static final String TAG = "com.ironsdn.websiteblocker.AppExtensionAccessibilityService";
    public static AppExtensionAccessibilityService sAccessibilityService;
    private HashMap<String, Long> previousUrlDetections = new HashMap<>();
    public boolean onKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 3) {
            Log.d(TAG, "[NAL] clear?11");

        } else if (keyCode == 4) {

        } else if (keyCode == 187) {
            Log.d(TAG, "[NAL] clear?12");

        }
        return super.onKeyEvent(keyEvent);
    }

    public void onServiceConnected() {
        if (Build.VERSION.SDK_INT >= 18) {
            Log.d(TAG, " [onServiceConnected]");
            sAccessibilityService = this;

            AccessibilityServiceInfo serviceInfo = getServiceInfo();
            serviceInfo.flags = 121;
            serviceInfo.eventTypes = 10281;
            serviceInfo.notificationTimeout = 100;
            setServiceInfo(serviceInfo);


        }
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "[NAL] [Disabled][onUnbind]");

        disableAccessibilityService();
        return super.onUnbind(intent);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "[NAL] [Disabled][onDestroy]");

        disableAccessibilityService();
    }

    public void onInterrupt() {
        Log.d(TAG, "[NAL] [Disabled][onInterrupt]");
        disableAccessibilityService();
    }

    public void disableAccessibilityService() {
        if (Build.VERSION.SDK_INT >= 24) {
            disableSelf();
        }
    }


    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            String packageName ="";
            if(event.getPackageName()!=null){
                packageName = event.getPackageName().toString();
            }

            AccessibilityNodeInfo parentNodeInfo = event.getSource();
            if (parentNodeInfo == null) {
                return;
            }

            SupportedBrowserConfig browserConfig = null;
            Log.d(TAG, "packageName  " + packageName);
            for (SupportedBrowserConfig supportedConfig: getSupportedBrowsers()) {
                if (supportedConfig.packageName.equals(packageName)) {
                    browserConfig = supportedConfig;
                }
            }
            //this is not supported browser, so exit
            if (browserConfig == null) {
                Log.d(TAG, "browserConfig is null ");
                return;
            }

            String capturedUrl = captureUrl(parentNodeInfo, browserConfig);
            Log.d(TAG, "capturedUrl " + capturedUrl);
            parentNodeInfo.recycle();

            //we can't find a url. Browser either was updated or opened page without url text field
            if (capturedUrl == null) {
                return;
            }

            long eventTime = event.getEventTime();
            String detectionId = packageName + ", and url " + capturedUrl;
            Log.d(TAG, "detectionId " + detectionId);
            //noinspection ConstantConditions
            long lastRecordedTime = previousUrlDetections.containsKey(detectionId) ? previousUrlDetections.get(detectionId) : 0;
            //some kind of redirect throttling
            if (eventTime - lastRecordedTime > 2000) {
                previousUrlDetections.put(detectionId, eventTime);
                analyzeCapturedUrl(capturedUrl, browserConfig.packageName);
            }
        }catch (Exception e){
e.printStackTrace();
        }

    }

    private String captureUrl(AccessibilityNodeInfo info, SupportedBrowserConfig config) {
        Log.d(TAG, "captureUrl()");
        List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByViewId(config.addressBarId);
        if (nodes == null || nodes.size() <= 0) {
            return null;
        }

        AccessibilityNodeInfo addressBarNodeInfo = nodes.get(0);
        String url = null;
        if (addressBarNodeInfo.getText() != null) {
            url = addressBarNodeInfo.getText().toString();
        }
        addressBarNodeInfo.recycle();
        return url;
    }

    private void analyzeCapturedUrl(@NonNull String capturedUrl, @NonNull String browserPackage) {
        Log.d(TAG, "analyzeCapturedUrl() capturedUrl " + capturedUrl+" browserPackage "+ browserPackage);
        String redirectUrl = "https://www.ironsdn.com/";


        int hashCode = JavaUtility.getHash(capturedUrl);
        Log.e(TAG, "currentURL : " + capturedUrl +" hashCode :"+ hashCode);
        if(hashCode!=0){
            URlLookup uRlLookup=   Utility.Companion.getURlLookup(hashCode);
            Log.e(TAG, "uRlLookup " + uRlLookup.toString() +" url  :"+ uRlLookup.getURL());
            if(uRlLookup.getAlert())
                performRedirect(redirectUrl, browserPackage);
        }
       /* if (capturedUrl.contains("m.facebook.com")) {
            performRedirect(redirectUrl, browserPackage);
        }*/
    }

    /** we just reopen the browser app with our redirect url using service context
     * We may use more complicated solution with invisible activity to send a simple intent to open the url */
    private void performRedirect(@NonNull String redirectUrl, @NonNull String browserPackage) {
        try {
            Log.d(TAG, "redirectUrl "+redirectUrl+" browserPackage "+browserPackage);
//            Intent intent = new Intent(this, MainActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            getApplicationContext().startActivity(intent);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
            intent.setPackage(browserPackage);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackage);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //  intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            getApplicationContext().startActivity(intent);
        }
        catch(ActivityNotFoundException e) {
            // the expected browser is not installed
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
            startActivity(i);

        }
    }


    @NonNull
    private static String[] packageNames() {
        List<String> packageNames = new ArrayList<>();
        for (SupportedBrowserConfig config: getSupportedBrowsers()) {
            packageNames.add(config.packageName);
        }
        return packageNames.toArray(new String[0]);
    }

    private static class SupportedBrowserConfig {
        public String packageName, addressBarId;
        public SupportedBrowserConfig(String packageName, String addressBarId) {
            this.packageName = packageName;
            this.addressBarId = addressBarId;
        }
    }

    /** @return a list of supported browser configs
     * This list could be instead obtained from remote server to support future browser updates without updating an app */
    @NonNull
    private static List<SupportedBrowserConfig> getSupportedBrowsers() {
        List<SupportedBrowserConfig> browsers = new ArrayList<>();
        browsers.add( new SupportedBrowserConfig("com.android.chrome", "com.android.chrome:id/url_bar"));
        browsers.add( new SupportedBrowserConfig("org.mozilla.firefox", "org.mozilla.firefox:id/url_bar_title"));

        browsers.add( new SupportedBrowserConfig("com.opera.browser", "com.opera.browser:id/url_field"));

        browsers.add( new SupportedBrowserConfig("com.opera.mini.native", "com.opera.mini.native:id/url_field"));

        browsers.add( new SupportedBrowserConfig("com.duckduckgo.mobile.android", "com.duckduckgo.mobile.android:id/omnibarTextInput"));

        browsers.add( new SupportedBrowserConfig("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar"));

        return browsers;
    }






}
