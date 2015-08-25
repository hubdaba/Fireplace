package com.example.thefriendlybronto.blefun;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRouter;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

/**
 * Created by thefriendlybronto on 8/23/15.
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
        } catch (final UserRecoverableAuthException e) {
            Log.e(TAG, "RECOVERABLE " + e);
            activity.runOnUiThread(
                    new Runnable() {

                        @Override
                        public void run() {
                            if (e instanceof UserRecoverableAuthException) {
                                Intent intent = ((UserRecoverableAuthException) e).getIntent();
                                Log.d(TAG, "RECOVERING");
                                activity.startActivityForResult(
                                        intent, Constants.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                            }
                        }
                    }
            );


        } catch (GoogleAuthException e) {
            Log.w(TAG, "GoogleAuthException " + e);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }
}
