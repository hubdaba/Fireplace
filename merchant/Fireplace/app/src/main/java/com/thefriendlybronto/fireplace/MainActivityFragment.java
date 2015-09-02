package com.thefriendlybronto.fireplace;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = MainActivityFragment.class.getSimpleName();

    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    // The Eddystone-UID frame type byte.
    // See https://github.com/google/eddystone for more information.
    private static final byte EDDYSTONE_UID_FRAME_TYPE = 0x00;

    // Receives the runnable that stops scanning after SCAN_TIME_MILLIS.
    private static final Handler handler = new Handler(Looper.getMainLooper());



    private ArrayList<Beacon> arrayList;
    private BluetoothLeScanner scanner;
    private BeaconArrayAdapter beaconArrayAdapter;
    private SharedPreferences sharedPreferences;

    private TextView accountNameView;
    private Button scanButton;
    private ScanCallback scanCallback;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        arrayList = new ArrayList<>();
        beaconArrayAdapter = new BeaconArrayAdapter(getActivity(), R.id.beaconList, arrayList);


        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    Log.w(TAG, "null scan record");
                    return;
                }
                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
                if (serviceData == null) {
                    Log.e(TAG, "null service data");
                }
                Log.d(TAG, "found beacon");

                if (serviceData[0] != EDDYSTONE_UID_FRAME_TYPE) {
                    return;
                }

                byte[] id = Arrays.copyOfRange(serviceData, 2, 18);
                // if the id is already found return.
                for (Beacon beacon : arrayList) {
                    if (Arrays.equals(beacon.getId(), id)) {
                        return;
                    }
                }
                final Beacon beacon = new Beacon(
                        Beacon.BeaconType.EDDYSTONE,
                        id,
                        Beacon.BeaconStatus.UNKNOWN,
                        result.getRssi());
                beaconArrayAdapter.add(beacon);
                Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Beacon responseBeacon = new Beacon(response);
                        try {
                            Log.d(TAG, responseBeacon.toJson().toString());
                        } catch (JSONException e) {

                        }
                        int pos = beaconArrayAdapter.getPosition(beacon);
                        arrayList.set(pos, responseBeacon);
                        beaconArrayAdapter.notifyDataSetChanged();
                    }
                };
                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Beacon.BeaconStatus status = Beacon.BeaconStatus.UNREGISTERED;
                        try {
                            if (error.networkResponse == null || error.networkResponse.data == null) {
                                Log.e(TAG, "problem with volley");
                                return;
                            }
                            JSONObject err = new JSONObject(new String(error.networkResponse.data));
                            Log.d(TAG, err.toString());
                            int responseCode = err.getJSONObject("error").getInt("code");
                            if (responseCode == 404) {
                                status = Beacon.BeaconStatus.UNREGISTERED;
                            } else if (responseCode == 403) {
                                status = Beacon.BeaconStatus.UNAUTHORIZED;
                            } else {
                                Log.e(TAG, "UNKNOWN ERROR " + err.toString());
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "got excpetion when parsing error", e);
                        } finally {
                            int pos = beaconArrayAdapter.getPosition(beacon);
                            arrayList.set(pos, new Beacon(beacon.getType(), beacon.getId(), status, beacon.getRssi()));
                            beaconArrayAdapter.notifyDataSetChanged();
                        }
                    }
                };
                String urlPath = beacon.getBeaconName();
                String accountName = accountNameView.getText().toString();
                System.out.println("start api");
                new BeaconServiceTask(getActivity(), accountName, Request.Method.GET, urlPath, responseListener, errorListener).execute();
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Bluetooth scan failed: " + errorCode);
            }
        };
        // initialize the scanner.
        createScanner();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        scanButton = (Button) rootView.findViewById(R.id.scan);
        accountNameView = (TextView) rootView.findViewById(R.id.accountName);
        ScanFilter scanFilter =
                new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build();
        final List<ScanFilter> scanFilterList = new ArrayList<>();
        scanFilterList.add(scanFilter);
        final ScanSettings scanSettings =
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build();
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                scanButton.setEnabled(false);
                scanner.startScan(scanFilterList, scanSettings, scanCallback);

                Runnable stopScanning = new Runnable() {
                    @Override
                    public void run() {
                        scanner.stopScan(scanCallback);
                        Log.i(TAG, "stopped scan");
                        scanButton.setEnabled(true);
                    }
                };
                handler.postDelayed(stopScanning, 1000 /* 1 second */);

            }
        });
        accountNameView = (TextView)rootView.findViewById(R.id.accountName);
        accountNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickUserAccount();
            }
        });
        String accountName = sharedPreferences.getString("accountName", "");
        if (accountName != null && !accountName.isEmpty()) {
            accountNameView.setText(accountName);
        }
        else {
            pickUserAccount();
        }
        ListView listView = (ListView) rootView.findViewById(R.id.beaconList);
        listView.setAdapter(beaconArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Beacon beacon = beaconArrayAdapter.getItem(position);
                ManageBeaconFragment fragment = new ManageBeaconFragment();
                Bundle bundle = new Bundle();
                bundle.putString(Constants.BUNDLE_ACCOUNTNAME, accountNameView.getText().toString());
                bundle.putParcelable(Constants.BUNDLE_BEACON, beacon);
                fragment.setArguments(bundle);
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment, fragment)
                        .addToBackStack(TAG)
                        .commit();
            }
        });
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
        else if (requestCode == Constants.REQUEST_CODE_ENABLE_BLE) {
            if (resultCode == Activity.RESULT_OK) {
                createScanner();
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getActivity(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createScanner() {
        BluetoothManager btManager =
                (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLE);
        }
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.e(TAG, "Can't enable Bluetooth");
            Toast.makeText(getActivity(), "Can't enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        scanner = btAdapter.getBluetoothLeScanner();
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, Constants.REQUEST_CODE_PICK_ACCOUNT);
    }
}
