package com.thefriendlybronto.fireplace;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

/**
 * Created by thefriendlybronto on 8/25/15.
 */
public class AuthorizedServiceTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = AuthorizedServiceTask.class.getSimpleName();

    private final Activity activity;
    private final String accountName;

    public AuthorizedServiceTask(Activity activity, String accountName) {
        this.activity = activity;
        this.accountName = accountName;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.i(TAG, "checking authorization for " + accountName);
        try {
            GoogleAuthUtil.getToken(activity, accountName, Constants.AUTH_SCOPE);
        } catch (UserRecoverableAuthException e) {
            // GooglePlayServices.apk is either old, disabled, or not present
            // so we need to show the user some UI in the activity to recover.
            Log.w(TAG, "Recoverable: " + e);
            Utils.handleAuthException(activity, e);
        } catch (GoogleAuthException e) {
            // Some other type of unrecoverable exception has occurred.
            // Report and log the error as appropriate for your app.
            Log.w(TAG, "GoogleAuthException: " + e);
        } catch (IOException e) {
            // The fetchToken() method handles Google-specific exceptions,
            // so this indicates something went wrong at a higher level.
            // TIP: Check for network connectivity before starting the AsyncTask.
        }
        return null;
    }
}
