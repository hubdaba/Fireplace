package com.example.thefriendlybronto.blefun;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thefriendlybronto on 8/23/15.
 */
public class ManageBeaconFragment extends Fragment {
    private static final String TAG = MainActivityFragment.class.getSimpleName();

    private Beacon beacon;
    private String accountName;
    private String namespace;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = this.getArguments();
        beacon =  b.getParcelable("beacon");
        accountName = b.getString("accountName");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_manage_beacon, container, false);
        Button button = (Button)
                rootView.findViewById(R.id.register);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlPath = beacon.getBeaconName();
                try {
                    Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                           Log.d(TAG, "JSon response " + response);
                        }
                    };
                    Response.ErrorListener errorListener = new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, error.toString());
                            Log.d(TAG, error.networkResponse.toString());
                            Log.d(TAG, Integer.toString(error.networkResponse.statusCode));
                            Log.d(TAG, new String(error.networkResponse.data));
                        }
                    };
                    new BeaconServiceTask(
                            getActivity(),
                            accountName,
                            Request.Method.POST,
                            "beacons:register?key=" + getResources().getString(R.string.appkey),
                            beacon.toJson().put("status", Beacon.STATUS_ACTIVE),
                            responseListener,
                            errorListener).execute();

                } catch (JSONException e) {
                    Log.d(TAG, "Json Exception " + e);
                }
            }
        });
      return rootView;
    }

}
