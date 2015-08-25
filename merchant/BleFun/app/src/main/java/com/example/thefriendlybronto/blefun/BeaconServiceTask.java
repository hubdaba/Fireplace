package com.example.thefriendlybronto.blefun;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
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

import static com.example.thefriendlybronto.blefun.Constants.AUTH_SCOPE;

/**
 * Created by thefriendlybronto on 8/23/15.
 */
public class BeaconServiceTask extends AsyncTask<Object, Object, Object> {

    private static RequestQueue requestQueue = null;

    static RequestQueue getRequestQueue(Context ctx) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }

    private static final String TAG = BeaconServiceTask.class.getSimpleName();

    private static final String SERVICE_ENDPOINT = "https://proximitybeacon.googleapis.com/v1beta1/";

    private final Activity activity;
    private final String accountName;

    private final int requestMethod;
    private final String urlPath;
    private final JSONObject body;
    private final Response.Listener<JSONObject> responseListener;
    private final Response.ErrorListener errorListener;

    public BeaconServiceTask(
            Activity activity,
            String accountName,
            int requestMethod,
            String urlPath,
            Response.Listener<JSONObject> responseListener,
            Response.ErrorListener errorListener) {
        this(activity, accountName, requestMethod, urlPath, new JSONObject(), responseListener, errorListener);
    }

    public BeaconServiceTask(
            Activity activity,
            String accountName,
            int requestMethod,
            String urlPath,
            JSONObject body,
            Response.Listener<JSONObject> responseListener,
            Response.ErrorListener errorListener) {
        this.activity = activity;
        this.accountName = accountName;
        this.requestMethod = requestMethod;
        this.urlPath = urlPath;
        this.responseListener = responseListener;
        this.body = body;
        this.errorListener = errorListener;
    }

    private String getRequestMethodName() {
        switch (requestMethod) {
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

    @Override
    protected String doInBackground(Object... params) {
        try {
            final String token = GoogleAuthUtil.getToken(activity, accountName, AUTH_SCOPE);
            String url = SERVICE_ENDPOINT + urlPath;
            Log.d(TAG, url);
            JsonObjectRequest request = new JsonObjectRequest(
                    requestMethod, url, body, responseListener, errorListener) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    return headers;
                }
            };
            int initialTimeoutSeconds = requestMethod == Request.Method.GET ? 10 : 15;
            request.setRetryPolicy(new DefaultRetryPolicy(initialTimeoutSeconds * 1000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            String logMsg = getRequestMethodName() + " " + url;
            if (body != null && body.length() > 0) {
                logMsg += ", body: " + body.toString();
            }
            Log.i(TAG, logMsg);
            RequestQueue requestQueue = getRequestQueue(activity);
            requestQueue.add(request);
        } catch (UserRecoverableAuthException e) {
            e.printStackTrace();
        } catch (GoogleAuthException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();

        }
        Log.d(TAG, "returningnull");
        return null;
    }
}
