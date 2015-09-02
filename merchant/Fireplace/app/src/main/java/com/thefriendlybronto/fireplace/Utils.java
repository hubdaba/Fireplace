package com.thefriendlybronto.fireplace;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.util.Base64;

import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by thefriendlybronto on 8/25/15.
 */
public class Utils {
    static byte[] base64Decode(String s) {
        return Base64.decode(s, Base64.DEFAULT);
    }

    static String base64Encode(byte[] b) {
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    static void handleAuthException(final Activity activity, final Exception e) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e).getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                            statusCode, activity, Constants.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                }
                else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    activity.startActivityForResult(
                            intent, Constants.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }
}
