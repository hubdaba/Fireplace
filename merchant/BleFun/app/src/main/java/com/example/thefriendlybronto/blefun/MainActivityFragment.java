package com.example.thefriendlybronto.blefun;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.gms.common.AccountPicker;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by thefriendlybronto on 8/20/15.
 */
public class MainActivityFragment extends Fragment {
    private static final String TAG = MainActivityFragment.class.getSimpleName();
    private static final long SCAN_TIME_MILLIS = 2000;


    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();

    private static final byte EDDYSTONE_UID_FRAME_TYPE = 0x00;

    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    private static final ScanFilter EDDYSTONE_SCAN_FILTER = new ScanFilter.Builder()
            .setServiceUuid(EDDYSTONE_SERVICE_UUID)
            .build();

    private static final List<ScanFilter> SCAN_FILTERS = buildScanFilters();

    private static List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(EDDYSTONE_SCAN_FILTER);
        return scanFilters;
    }

    private static final Comparator<Beacon> RSSI_COMPARATOR = new Comparator<Beacon>() {
        @Override
        public int compare(Beacon lhs, Beacon rhs) {
            return ((Integer) rhs.rssi).compareTo(lhs.rssi);
        }
    };

    private SharedPreferences sharedPreferences;
    private ScanCallback scanCallback;
    private BluetoothLeScanner scanner;
    private ArrayList<Beacon> arrayList;
    private BeaconArrayAdapter arrayAdapter;
    private TextView accountNameView;
    Button scanButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        arrayList = new ArrayList<>();
        arrayAdapter = new BeaconArrayAdapter(getActivity(), R.layout.beacon_list_item, arrayList);

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    System.out.println(result);
                    return;
                }
                Log.d(TAG, result.toString());
                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
                if (serviceData == null) {
                    Log.d(TAG, "no service data");
                    return;
                }
                if (serviceData[0] != EDDYSTONE_UID_FRAME_TYPE) {
                    Log.d(TAG, "service data not formatted");
                    return;
                }
                byte[] id = Arrays.copyOfRange(serviceData, 2, 18);
                for (Beacon beacon : arrayList) {
                    if (beacon.id == id) {
                        return;
                    }
                }
                Log.i(TAG, "id " + Utils.toHexString(id) + ", rssi " + result.getRssi());
                Beacon beacon = new Beacon("EDDYSTONE", id, Beacon.STATUS_UNSPECIFIED, result.getRssi());
                insertIntoListAndFetchStatus(beacon);
            }
        };

        createScanner();
    }

    private void insertIntoListAndFetchStatus(final Beacon beacon) {
        arrayAdapter.add(beacon);
        arrayAdapter.sort(RSSI_COMPARATOR);
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                int pos = arrayAdapter.getPosition(beacon);
                arrayList.set(pos, new Beacon(response));
                arrayAdapter.notifyDataSetChanged();
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String registrationStatus = Beacon.STATUS_UNSPECIFIED;
                Log.e(TAG, error.toString());
                if (error.networkResponse == null || error.networkResponse.data == null) {
                    return;
                }
                try {
                    JSONObject err = new JSONObject(new String(error.networkResponse.data));
                    int responseCode = err.getJSONObject("error").getInt("code");
                    if (responseCode == 404) {
                        registrationStatus = Beacon.UNREGISTERED;
                    } else if (responseCode == 403) {
                        registrationStatus = Beacon.NOT_AUTHORIZED;
                    } else {
                        Log.e(TAG, "Unhandled expceptoin " + responseCode);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON exception");
                } finally {
                    int pos = arrayAdapter.getPosition(beacon);
                    arrayList.set(pos, new Beacon(beacon.type, beacon.id, registrationStatus, beacon.rssi));
                    arrayAdapter.notifyDataSetChanged();
                }



            }
        };
        String urlPath = beacon.getBeaconName();
        String accountName = accountNameView.getText().toString();
        new BeaconServiceTask(getActivity(), accountName, Request.Method.GET, urlPath, responseListener, errorListener).execute();
    }

    private void createScanner() {
        BluetoothManager btManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_PICK_ACCOUNT) {
            if (resultCode == Activity.RESULT_OK) {
                String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                accountNameView.setText(name);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("accountName", name);
                editor.apply();
                new AuthorizedServiceTask(getActivity(), name).execute();
            } else {
                Toast.makeText(getActivity(), "ACOUNT PROBLEMS", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == Constants.REQUEST_CODE_ENABLE_BLE) {
            Toast.makeText(getActivity(), "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        progressBar.setProgress(0);
        // progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);

        scanButton = (Button)rootView.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
             public void onClick(View v) {
                arrayAdapter.clear();
                Utils.setEnabledViews(false, scanButton);
                scanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
                Log.i(TAG, "starting scan");

                CountDownTimer countDownTimer = new CountDownTimer(SCAN_TIME_MILLIS, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        double i = (1 - millisUntilFinished / (double) SCAN_TIME_MILLIS) * 100;
                        progressBar.setProgress((int) i);
                    }

                    @Override
                    public void onFinish() {
                        progressBar.setProgress(100);
                    }
                };
                countDownTimer.start();

                Runnable stopScanning = new Runnable() {
                    @Override
                    public void run() {
                        scanner.stopScan(scanCallback);
                        Log.i(TAG, "stopped scan");
                        Utils.setEnabledViews(true, scanButton);
                    }
                };
                handler.postDelayed(stopScanning, SCAN_TIME_MILLIS);
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
        } else {
            pickUserAccount();
        }

        ListView listView = (ListView) rootView.findViewById(R.id.listView);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Beacon beacon = arrayAdapter.getItem(position);
                if (beacon.status.equals(Beacon.NOT_AUTHORIZED)) {
                    new AlertDialog.Builder(getActivity()).setTitle("Not authorized")
                            .setMessage("no permissions")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();


                }
                Bundle bundle = new Bundle();
                bundle.putString("accountName", accountNameView.getText().toString());
                bundle.putParcelable("beacon", arrayAdapter.getItem(position));
                ManageBeaconFragment fragment = new ManageBeaconFragment();
                fragment.setArguments(bundle);
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.rootlayout, fragment)
                        .addToBackStack(TAG)
                        .commit();

            }
        });

        return rootView;
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, Constants.REQUEST_CODE_PICK_ACCOUNT);
    }
}
