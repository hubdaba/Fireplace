package com.thefriendlybronto.fireplace;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thefriendlybronto on 8/30/15.
 */
public class MerchantServiceTask extends AsyncTask<Object, Object, Object> {
    private static final String TAG = MerchantServiceTask.class.getSimpleName();
    private static final String SERVICE_ENDPOINT = "https://endless-bounty-104719.appspot.com/";
    private Activity activity;
    private String accountName;
    private int method;
    private String urlPath;
    private JSONObject body;
    private Response.Listener<JSONObject> responseListener;
    private Response.ErrorListener errorListener;

    public MerchantServiceTask(
            Activity activity,
            String accountName,
            int method,
            String urlPath,
            Response.Listener<JSONObject> responseListener,
            Response.ErrorListener errorListener) {
        this(activity, accountName, method, urlPath, new JSONObject(), responseListener, errorListener);
    }

    public MerchantServiceTask(
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
        this.body = body;
        this.responseListener = responseListener;
        this.errorListener = errorListener;
    }

    @Override
    protected Object doInBackground(Object... params) {
            String url = SERVICE_ENDPOINT + urlPath;
            JsonObjectRequest request = new JsonObjectRequest(method, url, body, responseListener, errorListener);
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
