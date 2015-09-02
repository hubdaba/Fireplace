package com.thefriendlybronto.fireplace;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by thefriendlybronto on 8/30/15.
 */
public class RequestQueueManager {
    private static RequestQueue requestQueue = null;

    // Static method to guarantee singleton access
    static RequestQueue getRequestQueue(Context ctx) {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx);
        }
        return requestQueue;
    }


}
