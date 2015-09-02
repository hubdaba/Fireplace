package com.thefriendlybronto.fireplace;

/**
 * Created by thefriendlybronto on 8/25/15.
 */
public class Constants {
    public static final int REQUEST_CODE_ENABLE_BLE = 1;
    public static final int REQUEST_CODE_PICK_ACCOUNT = 2;

    public static final String BUNDLE_ACCOUNTNAME = "accountname";
    public static final String BUNDLE_BEACON = "beacon";
    public static final String BUNDLE_BEACON_NAME = "beaconname";

    static final String AUTH_SCOPE = "oauth2: https://www.googleapis.com/auth/userlocation.beacon.registry";
    public static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 3;

    public static final String PREFS_NAME = "BEACON_ACCOUNT_NAME";
}
