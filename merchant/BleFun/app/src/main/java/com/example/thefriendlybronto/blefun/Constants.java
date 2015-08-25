package com.example.thefriendlybronto.blefun;

/**
 * Created by thefriendlybronto on 8/20/15.
 */
public class Constants {
    private Constants() {}

    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_ENABLE_BLE = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    static final int REQUEST_CODE_PLACE_PICKER = 1003;

    static final String API_KEY = "AIzaSyCa8xRLl3CGQbBi5ZbhJ8g0OaoyYd0zoLc";
    static final String AUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    static final String PREFS_NAME = "com.google.sample.beaconservice.Prefs";
}
