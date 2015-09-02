package com.thefriendlybronto.fireplace;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.AccountPicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thefriendlybronto on 8/29/15.
 */
public class MerchantFragment extends Fragment {

    private static final String TAG = MerchantFragment.class.getSimpleName();

    private TextView accountNameView;
    private TextView merchantNameView;
    private TextView inputTextView;
    private SharedPreferences sharedPreferences;
    private Button insertMerchantButton;

    private MerchantBeaconArrayAdapter merchantBeaconArrayAdapter;
    private List<String> beaconIds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        beaconIds = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.merchant, container, false);
            accountNameView = (TextView) rootView.findViewById(R.id.merchantAccountName);
            accountNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickUserAccount();
                }
            });
            merchantNameView = (TextView) rootView.findViewById(R.id.merchantName);
            inputTextView = (TextView) rootView.findViewById(R.id.editTextMerchantName);
            merchantBeaconArrayAdapter = new MerchantBeaconArrayAdapter(getActivity(), R.id.merchantBeaconList, beaconIds);
            insertMerchantButton = (Button) rootView.findViewById(R.id.insertMerchantButton);
            final ListView merchantBeaconListView = (ListView) rootView.findViewById(R.id.merchantBeaconList);
            merchantBeaconListView.setAdapter(merchantBeaconArrayAdapter);
            merchantBeaconListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String beaconName = merchantBeaconArrayAdapter.getItem(position);
                    BeaconAttachmentFragment fragment = new BeaconAttachmentFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.BUNDLE_ACCOUNTNAME, accountNameView.getText().toString());
                    bundle.putString(Constants.BUNDLE_BEACON_NAME, beaconName);
                    fragment.setArguments(bundle);
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment, fragment)
                            .addToBackStack(TAG)
                            .commit();
                }
            });


            setUpMerchantInsertButton();
            final String accountName = sharedPreferences.getString("accountName", "");



            if (accountName != null && !accountName.isEmpty()) {
                accountNameView.setText(accountName);
                Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(final JSONObject response) {
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    merchantNameView.setText("Merchant Name: " + response.getString("merchantName"));
                                    JSONArray beaconJsonArray = response.getJSONArray("beacons");
                                    Log.d(TAG, beaconJsonArray.toString());
                                    beaconIds.clear();
                                    for (int i = 0; i <  beaconJsonArray.length(); i++) {
                                        String beaconId = beaconJsonArray.getString(i);
                                        beaconIds.add(beaconId);
                                    }
                                    Log.d(TAG, beaconIds.toString());
                                    merchantBeaconArrayAdapter.notifyDataSetChanged();

                                } catch (JSONException e) {
                                    Log.e(TAG, "got exception when processing response.", e);
                                }

                                inputTextView.setVisibility(View.INVISIBLE);
                                insertMerchantButton.setVisibility(View.INVISIBLE);
                            }
                        });

                    }
                };
                Response.ErrorListener errorListener = new Response.ErrorListener() {

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
                            int responseCode = err.getInt("status_code");
                            if (responseCode == 404) {
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        merchantNameView.setText("please set up a merchant");
                                        inputTextView.setVisibility(View.VISIBLE);
                                        insertMerchantButton.setVisibility(View.VISIBLE);
                                    }
                                });
                            }

                        } catch (JSONException e) {
                            Log.d(TAG, "got excpetion when parsing error", e);
                        }
                    }
                };
                new MerchantServiceTask(
                        getActivity(),
                        accountName,
                        Request.Method.GET,
                        "merchant?email=" + accountName,
                        responseListener,
                        errorListener).execute();
            }
            else {
                pickUserAccount();
            }


            return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == Activity.RESULT_OK) {
                String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                accountNameView.setText(name);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("accountName", name);
                editor.apply();
                // The first time the account tries to contact the beacon service we'll pop a dialog
                // asking the user to authorize our activity. Ensure that's handled cleanly here, rather
                // than when the scan tries to fetch the status of every beacon within range.
                new AuthorizedServiceTask(getActivity(), name).execute();
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(getActivity(), "Please pick an account", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, Constants.REQUEST_CODE_PICK_ACCOUNT);
    }

    private void setUpMerchantInsertButton() {
        final Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {

                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                merchantNameView.setText(response.getString("merchantName"));
                                insertMerchantButton.setVisibility(View.INVISIBLE);
                                inputTextView.setVisibility(View.INVISIBLE);
                            } catch(JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            }
        };
        final Response.ErrorListener errorListener = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, new String(error.networkResponse.data));
            }
        };

        insertMerchantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject body;
                try {
                    JSONObject merchantData = new JSONObject().put("email", accountNameView.getText()).put("merchantName", inputTextView.getText());
                    body = new JSONObject().put("body", merchantData);
                } catch (JSONException e) {
                    Log.d(TAG, "exceptoin when making jsonbdoy", e);
                    return;
                }
                new MerchantServiceTask(
                        getActivity(),
                        accountNameView.getText().toString(),
                        Request.Method.POST,
                        "merchant",
                        body,
                        responseListener,
                        errorListener).execute();

            }
        });
    }
}
