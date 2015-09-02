package com.thefriendlybronto.fireplace;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Created by thefriendlybronto on 8/28/15.
 */
public class ManageBeaconFragment extends Fragment {

    private static final String TAG = ManageBeaconFragment.class.getSimpleName();

    private Beacon beacon;
    private String accountName;
    private TextView beaconNameView;
    private TextView beaconStatus;

    public ManageBeaconFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = this.getArguments();
        this.accountName = b.getString(Constants.BUNDLE_ACCOUNTNAME);
        this.beacon = b.getParcelable(Constants.BUNDLE_BEACON);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.manage_beacon, container, false);
        Button registerButton = (Button) rootView.findViewById(R.id.registerBeacon);
        final Response.ErrorListener errorListener =
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        try {
                            if (error.networkResponse == null || error.networkResponse.data == null) {
                                Log.e(TAG, "problem with volley");
                                return;
                            }
                            Log.d(TAG, new String(error.networkResponse.data));
                            JSONObject err = new JSONObject(new String(error.networkResponse.data));
                            Log.d(TAG, err.toString());
                        } catch (JSONException e) {
                            Log.d(TAG, "got excpetion when parsing error", e);
                        }
                    }
                };
        final Response.Listener<JSONObject> registerResponseListener =
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(final JSONObject response) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beacon = new Beacon(response);
                                redraw();
                            }
                        });

                    }
                };
        final Response.Listener<JSONObject> activateResponseListener =
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(final JSONObject response) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beacon.setStatus(Beacon.BeaconStatus.ACTIVE);
                                redraw();
                            }
                        });
                    }
                };
        final Response.Listener<JSONObject> deactivateResponseListener =

                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beacon.setStatus(Beacon.BeaconStatus.INACTIVE);
                                redraw();
                            }
                        });

                    }
                };
        final Response.Listener<JSONObject> decomissionResponseListener =
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beacon.setStatus(Beacon.BeaconStatus.DECOMISSIONED);
                                redraw();
                            }
                        });
                    }
                };
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (beacon.getStatus()) {
                    case UNREGISTERED:
                        try {
                            new BeaconServiceTask(
                                    getActivity(),
                                    accountName,
                                    Request.Method.POST,
                                    "beacons:register",
                                    beacon.toJson().put("status", Beacon.BeaconStatus.ACTIVE),
                                    registerResponseListener,
                                    errorListener).execute();
                        } catch (JSONException e) {
                            Log.e(TAG, "error when registering beacon", e);
                        }
                        break;
                    case ACTIVE:
                    case DECOMISSIONED:
                    case INACTIVE:
                    case UNKNOWN:
                    case UNAUTHORIZED:
                        Log.e(TAG, "unable to activate");

                }
                try {
                    new MerchantServiceTask(
                            getActivity(),
                            accountName,
                            Request.Method.POST,
                            "beacon",
                            new JSONObject().put("body", new JSONObject().put("email", accountName).put("id", beacon.getBeaconName())),
                            new Response.Listener<JSONObject>() {

                                @Override
                                public void onResponse(JSONObject response) {
                                    Log.d(TAG, "sucessful write");
                                }
                            },
                            errorListener).execute();
                } catch (JSONException e) {

                }

            }
        });
        Button decomissionButton = (Button) rootView.findViewById(R.id.decomissionBeacon);
        decomissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (beacon.getStatus()) {

                    case ACTIVE:
                    case INACTIVE:
                        // already active.
                            new BeaconServiceTask(
                                    getActivity(),
                                    accountName,
                                    Request.Method.POST,
                                    beacon.getBeaconName() + ":decomission",
                                    decomissionResponseListener,
                                    errorListener).execute();

                        break;
                    case UNREGISTERED:
                    case DECOMISSIONED:
                    case UNKNOWN:
                    case UNAUTHORIZED:
                        Log.e(TAG, "unable to decomission");

                }
            }
        });
        Button activateButton = (Button) rootView.findViewById(R.id.activateBeacon);
        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (beacon.getStatus()) {
                    case INACTIVE:
                        // already active.
                        new BeaconServiceTask(
                                getActivity(),
                                accountName,
                                Request.Method.POST,
                                beacon.getBeaconName() + ":activate",
                                activateResponseListener,
                                errorListener).execute();

                        break;
                    case ACTIVE:
                    case UNREGISTERED:
                    case DECOMISSIONED:
                    case UNKNOWN:
                    case UNAUTHORIZED:
                        Log.e(TAG, "unable to activate");

                }
            }
        });
        Button deactivateButton = (Button) rootView.findViewById(R.id.deactivateBeacon);
        deactivateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (beacon.getStatus()) {
                    case ACTIVE:
                        // already active.
                        new BeaconServiceTask(
                                getActivity(),
                                accountName,
                                Request.Method.POST,
                                beacon.getBeaconName() + ":deactivate",
                                deactivateResponseListener,
                                errorListener).execute();

                        break;
                    case INACTIVE:
                    case UNREGISTERED:
                    case DECOMISSIONED:
                    case UNKNOWN:
                    case UNAUTHORIZED:
                        Log.e(TAG, "unable to deactivate");
                }
            }
        });

        beaconNameView = (TextView) rootView.findViewById(R.id.beaconName);
        beaconStatus = (TextView) rootView.findViewById(R.id.manageBeaconStatus);

        beaconNameView.setText(beacon.getBeaconName());
        beaconStatus.setText(beacon.getStatus().name());
        return rootView;
    }

    private void redraw() {
        this.beaconNameView.setText(beacon.getBeaconName());
        this.beaconStatus.setText(beacon.getStatus().name());

    }
}
