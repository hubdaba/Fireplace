package com.thefriendlybronto.fireplace;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thefriendlybronto on 8/25/15.
 */
public class BeaconServiceTask extends AsyncTask<Object, Object, Object> {

    private static final String TAG = BeaconServiceTask.class.getSimpleName();
    private static final String SERVICE_ENDPOINT = "https://proximitybeacon.googleapis.com/v1beta1/";

    private final Activity activity;
    private final String accountName;
    private final int method;
    private final String urlPath;
    private final Response.Listener<JSONObject> responseListener;
    private final Response.ErrorListener errorListener;
    private final JSONObject body;

    public BeaconServiceTask(
            Activity activity,
            String accountName,
            int method,
            String urlPath,
            Response.Listener<JSONObject> responseListener,
            Response.ErrorListener errorListener) {
        this(activity, accountName, method, urlPath, new JSONObject(), responseListener, errorListener);
    }

    public BeaconServiceTask(
            Activity activity,
            String accountName,
            int method,
            String urlPath,
            JSONObject body,
            Response.Listener<JSONObject> responseListener,
            Response.ErrorListener errorListener) {
        this.activity = activity;
        this.accountName = accountName;
        this.method = method;
        this.urlPath = urlPath;
        this.responseListener = responseListener;
        this.errorListener = errorListener;
        this.body = body;
    }

    @Override
    protected Object doInBackground(Object... params) {
        try {
            final String token = GoogleAuthUtil.getToken(activity, accountName, Constants.AUTH_SCOPE);
            String url = SERVICE_ENDPOINT + urlPath;
            JsonObjectRequest request = new JsonObjectRequest(
                    method, url, body, responseListener, errorListener) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };
            int initialTimeoutSeconds = method == Request.Method.GET ? 10 : 15;
            request.setRetryPolicy(new DefaultRetryPolicy(initialTimeoutSeconds * 1000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            String logMsg = getRequestMethodName() + " " + url;
            if (body != null && body.length() > 0) {
                logMsg += ", body: " + body.toString();
            }
            Log.d(TAG, logMsg);
            RequestQueue requestQueue = RequestQueueManager.getRequestQueue(activity);
            requestQueue.add(request);
        } catch (UserRecoverableAuthException e) {
            Log.w(TAG, "UserRocoverable: " + e);
        } catch (GoogleAuthException e) {
            Log.w(TAG, "GoogleAuthException: " + e);
        } catch (IOException e) {
            Log.w(TAG, "IoException: " + e);
        }
        return null;
    }

    private String getRequestMethodName() {
        switch (method) {
            case Request.Method.GET:
                return "GET";
            case Request.Method.PUT:
                return "PUT";
            case Request.Method.POST:
                return "POST";
            case Request.Method.DELETE:
                return "DELETE";
            default: return "default";
        }
    }


}
